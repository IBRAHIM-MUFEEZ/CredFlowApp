#!/usr/bin/env node

/**
 * Firestore User Data Migration Script (Admin SDK — bypasses security rules)
 *
 * Usage:
 *   node migrate-user.js <oldUserId> <newFirebaseUid>
 *
 * Example:
 *   node migrate-user.js "google_mufeezibrahim786@gmail.com" "rYORDOIavyYu7V5n6E9wXOXbP913"
 *
 * Requires: serviceAccountKey.json in the web-app/ folder
 * Get it from: Firebase Console → Project Settings → Service Accounts → Generate new private key
 */

import admin from 'firebase-admin';
import { readFileSync } from 'fs';
import { createInterface } from 'readline';
import { resolve, dirname } from 'path';
import { fileURLToPath } from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));

// ── Load service account key ──────────────────────────────────────────────────

const keyPath = resolve(__dirname, 'serviceAccountKey.json');

let serviceAccount;
try {
  serviceAccount = JSON.parse(readFileSync(keyPath, 'utf8'));
} catch {
  console.error('\n❌ serviceAccountKey.json not found in web-app/ folder.\n');
  console.error('To get it:');
  console.error('  1. Go to Firebase Console → Project Settings (gear icon)');
  console.error('  2. Click "Service accounts" tab');
  console.error('  3. Click "Generate new private key"');
  console.error('  4. Save the downloaded file as: web-app/serviceAccountKey.json\n');
  process.exit(1);
}

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
});

const db = admin.firestore();

// ── Config ────────────────────────────────────────────────────────────────────

const COLLECTIONS = ['customers', 'transactions', 'accounts', 'payments', 'savings', 'profile'];
const BATCH_SIZE = 400;

// ── Helpers ───────────────────────────────────────────────────────────────────

async function prompt(question) {
  const rl = createInterface({ input: process.stdin, output: process.stdout });
  return new Promise(resolve => {
    rl.question(question, answer => {
      rl.close();
      resolve(answer.trim().toLowerCase());
    });
  });
}

// ── Migration ─────────────────────────────────────────────────────────────────

async function migrateUser(oldUserId, newFirebaseUid) {
  console.log('\n🔄 Starting migration...');
  console.log(`   From: /users/${oldUserId}`);
  console.log(`   To:   /users/${newFirebaseUid}\n`);

  let totalDocsCopied = 0;

  for (const collectionName of COLLECTIONS) {
    console.log(`📂 Processing ${collectionName}...`);

    const oldRef = db.collection('users').doc(oldUserId).collection(collectionName);
    const newRef = db.collection('users').doc(newFirebaseUid).collection(collectionName);

    const snapshot = await oldRef.get();

    if (snapshot.empty) {
      console.log(`   ⚠️  No documents in ${collectionName} — skipping\n`);
      continue;
    }

    console.log(`   Found ${snapshot.size} documents`);

    const docs = snapshot.docs;
    for (let i = 0; i < docs.length; i += BATCH_SIZE) {
      const batch = db.batch();
      const chunk = docs.slice(i, Math.min(i + BATCH_SIZE, docs.length));

      chunk.forEach(docSnap => {
        batch.set(newRef.doc(docSnap.id), docSnap.data());
      });

      await batch.commit();
      console.log(`   ✅ Copied ${chunk.length} docs (batch ${Math.floor(i / BATCH_SIZE) + 1})`);
      totalDocsCopied += chunk.length;
    }
    console.log('');
  }

  console.log(`✅ Migration complete — ${totalDocsCopied} total documents copied.\n`);
}

// ── Verification ──────────────────────────────────────────────────────────────

async function verifyMigration(oldUserId, newFirebaseUid) {
  console.log('🔍 Verifying...\n');
  let allMatch = true;

  for (const collectionName of COLLECTIONS) {
    const oldSnap = await db.collection('users').doc(oldUserId).collection(collectionName).get();
    const newSnap = await db.collection('users').doc(newFirebaseUid).collection(collectionName).get();

    if (oldSnap.size !== newSnap.size) {
      console.log(`   ⚠️  ${collectionName}: old=${oldSnap.size} new=${newSnap.size} — MISMATCH`);
      allMatch = false;
    } else if (newSnap.size > 0) {
      console.log(`   ✅ ${collectionName}: ${newSnap.size} docs match`);
    }
  }

  console.log('');
  return allMatch;
}

// ── Deletion ──────────────────────────────────────────────────────────────────

async function deleteOldPath(oldUserId) {
  console.log(`🗑️  Deleting old path /users/${oldUserId}...\n`);

  for (const collectionName of COLLECTIONS) {
    const ref = db.collection('users').doc(oldUserId).collection(collectionName);
    const snapshot = await ref.get();
    if (snapshot.empty) continue;

    const docs = snapshot.docs;
    for (let i = 0; i < docs.length; i += BATCH_SIZE) {
      const batch = db.batch();
      docs.slice(i, Math.min(i + BATCH_SIZE, docs.length)).forEach(d => batch.delete(d.ref));
      await batch.commit();
    }
    console.log(`   ✅ Deleted ${snapshot.size} docs from ${collectionName}`);
  }

  // Delete the parent document itself
  await db.collection('users').doc(oldUserId).delete().catch(() => {});
  console.log(`   ✅ Deleted parent document\n`);
}

// ── Main ──────────────────────────────────────────────────────────────────────

async function main() {
  const [oldUserId, newFirebaseUid] = process.argv.slice(2);

  if (!oldUserId || !newFirebaseUid) {
    console.error('\n❌ Usage: node migrate-user.js <oldUserId> <newFirebaseUid>\n');
    console.error('Example:');
    console.error('  node migrate-user.js "google_mufeezibrahim786@gmail.com" "rYORDOIavyYu7V5n6E9wXOXbP913"\n');
    process.exit(1);
  }

  console.log('\n🔐 Radafiq User Data Migration (Admin SDK)');
  console.log('==========================================\n');
  console.log(`📊 Project: ${serviceAccount.project_id}`);
  console.log(`👤 Old user ID : ${oldUserId}`);
  console.log(`🆔 New UID     : ${newFirebaseUid}\n`);

  const confirm = await prompt('⚠️  Copy all data from old path to new path? (yes/no): ');
  if (confirm !== 'yes' && confirm !== 'y') {
    console.log('\n❌ Cancelled.\n');
    process.exit(0);
  }

  try {
    await migrateUser(oldUserId, newFirebaseUid);

    const allMatch = await verifyMigration(oldUserId, newFirebaseUid);

    if (!allMatch) {
      console.log('⚠️  Some counts did not match. Check above before deleting old data.\n');
    }

    const shouldDelete = await prompt('Delete the old path now? (yes/no): ');
    if (shouldDelete === 'yes' || shouldDelete === 'y') {
      await deleteOldPath(oldUserId);
    } else {
      console.log('\n⚠️  Old path kept. Delete it manually from Firestore Console when ready.\n');
    }

    console.log('🎉 Done!\n');
    console.log('Next steps:');
    console.log('  1. Open the app and sign in — all data should appear');
    console.log('  2. Restore strict Firestore rules (request.auth.uid == userId)');
    console.log('  3. Delete serviceAccountKey.json — it has full admin access!\n');

  } catch (err) {
    console.error('\n❌ Migration failed:', err.message);
    console.error('Your data was NOT deleted. Safe to retry.\n');
    process.exit(1);
  }

  process.exit(0);
}

main();
