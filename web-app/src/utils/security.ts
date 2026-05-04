// Passcode hashing using Web Crypto API
// Uses PBKDF2, slow by design to resist brute-force attacks.

const PBKDF2_ITERATIONS = 150_000;
const MAX_FAILED_ATTEMPTS = 5;
const LOCKOUT_MS = 60_000;

export async function hashPasscode(passcode: string, salt: string): Promise<string> {
  const keyMaterial = await crypto.subtle.importKey(
    'raw',
    new TextEncoder().encode(passcode),
    'PBKDF2',
    false,
    ['deriveBits']
  );
  const bits = await crypto.subtle.deriveBits(
    {
      name: 'PBKDF2',
      salt: new TextEncoder().encode(salt),
      iterations: PBKDF2_ITERATIONS,
      hash: 'SHA-256',
    },
    keyMaterial,
    256
  );
  return Array.from(new Uint8Array(bits))
    .map(b => b.toString(16).padStart(2, '0'))
    .join('');
}

export async function hashRecoveryAnswer(answer: string, salt: string): Promise<string> {
  const keyMaterial = await crypto.subtle.importKey(
    'raw',
    new TextEncoder().encode(answer.trim().toLowerCase()),
    'PBKDF2',
    false,
    ['deriveBits']
  );
  const bits = await crypto.subtle.deriveBits(
    {
      name: 'PBKDF2',
      salt: new TextEncoder().encode(`${salt}:recovery`),
      iterations: PBKDF2_ITERATIONS,
      hash: 'SHA-256',
    },
    keyMaterial,
    256
  );
  return Array.from(new Uint8Array(bits))
    .map(b => b.toString(16).padStart(2, '0'))
    .join('');
}

export function generateSalt(): string {
  const array = new Uint8Array(16);
  crypto.getRandomValues(array);
  return Array.from(array).map(b => b.toString(16).padStart(2, '0')).join('');
}

// ── Security Storage ──────────────────────────────────────────────────────────

const SECURITY_KEY = 'radafiq_security';

interface SecurityStorage {
  passcodeHash: string;
  passcodeSalt: string;
  lockEnabled: boolean;
  recoveryQuestion: string;
  recoveryAnswerHash: string;
  ownerUid?: string;
  failedAttempts?: number;
  lockoutUntil?: number;
}

export function loadSecurityStorage(): SecurityStorage {
  try {
    const raw = localStorage.getItem(SECURITY_KEY);
    if (!raw) return emptySecurityStorage();
    return { ...emptySecurityStorage(), ...JSON.parse(raw) };
  } catch {
    return emptySecurityStorage();
  }
}

export function saveSecurityStorage(data: Partial<SecurityStorage>): void {
  const existing = loadSecurityStorage();
  localStorage.setItem(SECURITY_KEY, JSON.stringify({ ...existing, ...data }));
}

export function clearSecurityStorage(): void {
  localStorage.removeItem(SECURITY_KEY);
}

export function clearSecurityStorageForOtherUser(uid: string): void {
  const existing = loadSecurityStorage();
  if (existing.passcodeHash && existing.ownerUid !== uid) {
    clearSecurityStorage();
  }
}

export function isPasscodeLockedOut(): boolean {
  const { lockoutUntil = 0 } = loadSecurityStorage();
  return lockoutUntil > Date.now();
}

export function recordPasscodeFailure(): void {
  const existing = loadSecurityStorage();
  const failedAttempts = (existing.failedAttempts ?? 0) + 1;
  saveSecurityStorage({
    failedAttempts,
    lockoutUntil: failedAttempts >= MAX_FAILED_ATTEMPTS ? Date.now() + LOCKOUT_MS : 0,
  });
}

export function clearPasscodeFailures(): void {
  saveSecurityStorage({ failedAttempts: 0, lockoutUntil: 0 });
}

function emptySecurityStorage(): SecurityStorage {
  return {
    passcodeHash: '',
    passcodeSalt: '',
    lockEnabled: false,
    recoveryQuestion: '',
    recoveryAnswerHash: '',
    ownerUid: '',
    failedAttempts: 0,
    lockoutUntil: 0,
  };
}

// ── Passkey credential storage ────────────────────────────────────────────────
// Stores the WebAuthn credential ID per user UID in localStorage.
// The credential ID is not secret — it's a handle the browser uses to look up
// the private key stored in the platform authenticator (TPM / Secure Enclave).

const PASSKEY_KEY = 'radafiq_passkey';

interface PasskeyStorage {
  credentialId: string; // base64url
  ownerUid: string;
}

export function loadPasskeyStorage(): PasskeyStorage {
  try {
    const raw = localStorage.getItem(PASSKEY_KEY);
    if (!raw) return { credentialId: '', ownerUid: '' };
    return { credentialId: '', ownerUid: '', ...JSON.parse(raw) };
  } catch {
    return { credentialId: '', ownerUid: '' };
  }
}

export function savePasskeyStorage(credentialId: string, ownerUid: string): void {
  localStorage.setItem(PASSKEY_KEY, JSON.stringify({ credentialId, ownerUid }));
}

export function clearPasskeyStorage(): void {
  localStorage.removeItem(PASSKEY_KEY);
}

/** Returns the stored credential ID for the given user, or '' if none. */
export function getPasskeyCredentialId(uid: string): string {
  const stored = loadPasskeyStorage();
  return stored.ownerUid === uid ? stored.credentialId : '';
}
