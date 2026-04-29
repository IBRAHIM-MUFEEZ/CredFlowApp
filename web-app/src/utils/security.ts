// Passcode hashing using Web Crypto API
// Uses PBKDF2 (100,000 iterations) — slow by design to resist brute-force attacks

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
      iterations: 100_000,
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
      iterations: 100_000,
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
}

export function loadSecurityStorage(): SecurityStorage {
  try {
    const raw = localStorage.getItem(SECURITY_KEY);
    if (!raw) return { passcodeHash: '', passcodeSalt: '', lockEnabled: false, recoveryQuestion: '', recoveryAnswerHash: '' };
    return JSON.parse(raw);
  } catch {
    return { passcodeHash: '', passcodeSalt: '', lockEnabled: false, recoveryQuestion: '', recoveryAnswerHash: '' };
  }
}

export function saveSecurityStorage(data: Partial<SecurityStorage>): void {
  const existing = loadSecurityStorage();
  localStorage.setItem(SECURITY_KEY, JSON.stringify({ ...existing, ...data }));
}

export function clearSecurityStorage(): void {
  localStorage.removeItem(SECURITY_KEY);
}
