// SHA-256 hashing using Web Crypto API

export async function sha256(message: string): Promise<string> {
  const msgBuffer = new TextEncoder().encode(message);
  const hashBuffer = await crypto.subtle.digest('SHA-256', msgBuffer);
  const hashArray = Array.from(new Uint8Array(hashBuffer));
  return hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
}

export async function hashPasscode(passcode: string, salt: string): Promise<string> {
  return sha256(`${salt}:${passcode}`);
}

export async function hashRecoveryAnswer(answer: string, salt: string): Promise<string> {
  return sha256(`${salt}:recovery:${answer.trim().toLowerCase()}`);
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
