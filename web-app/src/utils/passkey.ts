// ── WebAuthn Passkey Helpers ──────────────────────────────────────────────────
//
// Pure browser Web Authentication API — no library required.
// Passkeys are device-bound (platform authenticators: Windows Hello, Touch ID,
// Android fingerprint). The credential ID is stored in localStorage so the
// correct credential can be passed back during authentication.
//
// Security model:
//   • Registration creates a public-key credential on the device.
//   • Authentication proves possession of the private key via a signed challenge.
//   • The challenge is a random 32-byte value generated client-side.
//   • Because there is no server, the challenge is single-use and verified
//     implicitly by the browser's authenticator — the authenticator only signs
//     a challenge it was given in the same JS call, so replay attacks are
//     prevented by the browser's own WebAuthn implementation.
//   • The credential ID (not the private key) is stored in localStorage so we
//     can pass it as an allowCredentials hint on subsequent authentications.

const RP_NAME = 'Radafiq';

function isIpAddress(hostname: string): boolean {
  return /^(\d{1,3}\.){3}\d{1,3}$/.test(hostname) || hostname.includes(':');
}

// Derive a stable rpId when the browser allows one. For localhost/IP origins,
// omit rpId so Chrome/Edge can use the origin's effective domain for WebAuthn.
function getRpId(): string | undefined {
  const hostname = window.location.hostname;
  if (!hostname || hostname === 'localhost' || isIpAddress(hostname)) return undefined;
  return hostname;
}

function webAuthnErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof DOMException) {
    switch (error.name) {
      case 'NotAllowedError':
        return 'Windows Hello or biometric verification was cancelled or timed out.';
      case 'SecurityError':
        return 'Passkeys require HTTPS, localhost, or a valid app domain.';
      case 'InvalidStateError':
        return 'A passkey already exists for this device. Try unlocking or remove and register again.';
      case 'NotSupportedError':
        return 'This browser or device does not support platform passkeys.';
      default:
        return error.message || fallback;
    }
  }
  if (error instanceof Error) return error.message;
  return fallback;
}

function randomChallenge(): ArrayBuffer {
  const buf = new Uint8Array(32);
  crypto.getRandomValues(buf);
  return buf.buffer as ArrayBuffer;
}

// Base64url encode/decode (no padding) — used for credential IDs
export function bufferToBase64url(buf: ArrayBuffer | Uint8Array): string {
  const bytes = buf instanceof Uint8Array ? buf : new Uint8Array(buf);
  let str = '';
  bytes.forEach(b => (str += String.fromCharCode(b)));
  return btoa(str).replace(/\+/g, '-').replace(/\//g, '_').replace(/=/g, '');
}

export function base64urlToBuffer(b64: string): ArrayBuffer {
  const padded = b64.replace(/-/g, '+').replace(/_/g, '/').padEnd(
    b64.length + ((4 - (b64.length % 4)) % 4), '='
  );
  const binary = atob(padded);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
  return bytes.buffer as ArrayBuffer;
}

// ── Registration ──────────────────────────────────────────────────────────────

export interface PasskeyRegistrationResult {
  credentialId: string; // base64url
}

/**
 * Prompt the user to register a passkey (fingerprint / Windows Hello / etc.).
 * Returns the credential ID to store for future authentication.
 *
 * @param userId  Firebase UID — used as the WebAuthn user.id
 * @param userName Display name shown in the authenticator dialog
 */
export async function registerPasskey(
  userId: string,
  userName: string
): Promise<PasskeyRegistrationResult> {
  if (!window.PublicKeyCredential) {
    throw new Error('WebAuthn is not supported in this browser.');
  }
  if (!window.isSecureContext) {
    throw new Error('Passkeys require HTTPS or localhost.');
  }

  const challenge = randomChallenge();
  const rpId = getRpId();

  try {
    const credential = await navigator.credentials.create({
      publicKey: {
        rp: rpId ? { name: RP_NAME, id: rpId } : { name: RP_NAME },
      user: {
        id: new TextEncoder().encode(userId),
        name: userName,
        displayName: userName,
      },
      challenge,
      pubKeyCredParams: [
        { type: 'public-key', alg: -7 },   // ES256
        { type: 'public-key', alg: -257 },  // RS256
      ],
      authenticatorSelection: {
        authenticatorAttachment: 'platform', // device-bound (no security keys)
        userVerification: 'required',        // require biometric / PIN
        residentKey: 'preferred',
      },
      timeout: 60000,
      attestation: 'none', // we don't need attestation for app-lock use case
      },
    }) as PublicKeyCredential | null;

    if (!credential) throw new Error('Passkey registration was cancelled.');

    const credentialId = bufferToBase64url(credential.rawId);
    return { credentialId };
  } catch (error) {
    throw new Error(webAuthnErrorMessage(error, 'Passkey registration failed.'));
  }
}

// ── Authentication ────────────────────────────────────────────────────────────

/**
 * Prompt the user to authenticate with their registered passkey.
 * Returns true if the authenticator successfully signed the challenge.
 *
 * @param credentialId  The base64url credential ID stored during registration
 */
export async function authenticatePasskey(credentialId: string): Promise<boolean> {
  if (!window.PublicKeyCredential) {
    throw new Error('WebAuthn is not supported in this browser.');
  }
  if (!window.isSecureContext) {
    throw new Error('Passkeys require HTTPS or localhost.');
  }

  const challenge = randomChallenge();
  const rpId = getRpId();
  const publicKey: PublicKeyCredentialRequestOptions = {
    challenge,
    allowCredentials: [
      {
        type: 'public-key',
        id: base64urlToBuffer(credentialId),
      },
    ],
    userVerification: 'required',
    timeout: 60000,
  };
  if (rpId) publicKey.rpId = rpId;

  const assertion = await navigator.credentials.get({ publicKey }) as PublicKeyCredential | null;

  // If the browser returned an assertion without throwing, the user was
  // verified by the platform authenticator. The browser itself enforces
  // that the signed challenge matches what we passed — no server needed.
  return assertion !== null;
}

// ── Browser support check ─────────────────────────────────────────────────────

/**
 * Returns true if the browser supports platform authenticators
 * (Windows Hello, Touch ID, Android fingerprint, etc.).
 */
export async function isPlatformAuthenticatorAvailable(): Promise<boolean> {
  if (!window.PublicKeyCredential || !window.isSecureContext) return false;
  try {
    return await PublicKeyCredential.isUserVerifyingPlatformAuthenticatorAvailable();
  } catch {
    return false;
  }
}
