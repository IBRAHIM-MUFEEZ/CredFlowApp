import React, { useState } from 'react';
import { useApp } from '../context/AppContext';
import RadafiqLogo from '../components/RadafiqLogo';

// Completely standalone lock screen — no shared layout, no CSS classes that
// could introduce pseudo-elements or z-index stacking issues.

export default function AppLock() {
  const { security, verifyPasscode, resetPasscodeWithRecovery } = useApp();
  const [passcode, setPasscode] = useState('');
  const [error, setError] = useState('');
  const [checking, setChecking] = useState(false);
  const [showRecovery, setShowRecovery] = useState(false);
  const [recoveryAnswer, setRecoveryAnswer] = useState('');
  const [newPasscode, setNewPasscode] = useState('');
  const [confirmNew, setConfirmNew] = useState('');

  const verify = async (code: string) => {
    setChecking(true);
    setError('');
    const ok = await verifyPasscode(code);
    if (!ok) {
      setError('Incorrect passcode. Please try again.');
      setPasscode('');
    }
    setChecking(false);
  };

  const pressDigit = (digit: string) => {
    if (checking) return;
    const next = passcode.length < 6 ? passcode + digit : passcode;
    if (next === passcode) return;
    setPasscode(next);
    setError('');
    if (next.length === 6) verify(next);
  };

  const pressBackspace = () => {
    if (checking) return;
    setPasscode(p => p.slice(0, -1));
    setError('');
  };

  const handleRecovery = async () => {
    if (!recoveryAnswer.trim() || newPasscode.length !== 6 || newPasscode !== confirmNew) return;
    setChecking(true);
    setError('');
    const ok = await resetPasscodeWithRecovery(recoveryAnswer, newPasscode);
    if (!ok) setError('Recovery answer is incorrect.');
    setChecking(false);
  };

  const KEYS = [
    '1', '2', '3',
    '4', '5', '6',
    '7', '8', '9',
    '',  '0', '⌫',
  ];

  // ── Shared styles ──────────────────────────────────────────────────────────
  const page: React.CSSProperties = {
    width: '100vw',
    minHeight: '100vh',
    background: 'linear-gradient(160deg, #071525 0%, #0C2035 60%, #102840 100%)',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    padding: '2rem 1rem',
    boxSizing: 'border-box',
    fontFamily: "'Inter', system-ui, sans-serif",
  };

  const card: React.CSSProperties = {
    width: '100%',
    maxWidth: 340,
    textAlign: 'center',
  };

  const numpadBtn = (disabled: boolean, isBack: boolean): React.CSSProperties => ({
    height: 64,
    width: '100%',
    fontSize: isBack ? '1.5rem' : '1.375rem',
    fontWeight: 700,
    borderRadius: 16,
    border: '1.5px solid rgba(26,171,207,0.3)',
    background: 'rgba(16,40,64,0.9)',
    color: isBack ? '#6BAED4' : '#E8F4FF',
    cursor: disabled ? 'default' : 'pointer',
    opacity: disabled ? 0.3 : 1,
    outline: 'none',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    userSelect: 'none' as const,
    WebkitUserSelect: 'none' as const,
    touchAction: 'manipulation' as const,
    transition: 'background 0.1s',
    boxSizing: 'border-box' as const,
  });

  // ── Recovery screen ────────────────────────────────────────────────────────
  if (showRecovery) {
    return (
      <div style={page}>
        <div style={{ ...card, maxWidth: 400 }}>
          <div style={{ marginBottom: '1.5rem' }}>
            <h2 style={{ color: '#E8F4FF', marginBottom: 6 }}>Forgot Passcode</h2>
            <p style={{ color: '#6BAED4', fontSize: '0.875rem' }}>
              Answer your recovery question to reset your passcode.
            </p>
          </div>

          <div style={{ background: 'rgba(16,40,64,0.9)', border: '1px solid rgba(26,171,207,0.25)', borderRadius: 20, padding: '1.25rem', marginBottom: '1rem' }}>
            <p style={{ color: '#E8F4FF', fontWeight: 600, fontSize: '0.9375rem' }}>{security.recoveryQuestion}</p>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.875rem' }}>
            {[
              { label: 'Your Answer', val: recoveryAnswer, set: setRecoveryAnswer, type: 'text' },
              { label: 'New Passcode (6 digits)', val: newPasscode, set: (v: string) => setNewPasscode(v.replace(/\D/g,'').slice(0,6)), type: 'password' },
              { label: 'Confirm New Passcode', val: confirmNew, set: (v: string) => setConfirmNew(v.replace(/\D/g,'').slice(0,6)), type: 'password' },
            ].map(({ label, val, set, type }) => (
              <div key={label} style={{ textAlign: 'left' }}>
                <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 600, color: '#6BAED4', marginBottom: 6 }}>{label}</label>
                <input
                  type={type}
                  value={val}
                  onChange={e => set(e.target.value)}
                  style={{ width: '100%', background: 'rgba(16,40,64,0.9)', border: '1.5px solid rgba(26,171,207,0.3)', borderRadius: 12, padding: '0.75rem 1rem', color: '#E8F4FF', fontSize: '0.9375rem', outline: 'none', boxSizing: 'border-box' }}
                />
              </div>
            ))}

            {error && <p style={{ color: '#E8445A', fontSize: '0.875rem' }}>{error}</p>}

            <button
              onClick={handleRecovery}
              disabled={checking || !recoveryAnswer.trim() || newPasscode.length !== 6 || newPasscode !== confirmNew}
              style={{ ...numpadBtn(checking || !recoveryAnswer.trim() || newPasscode.length !== 6 || newPasscode !== confirmNew, false), height: 48, background: '#1A8FD4', color: 'white', borderRadius: 12, fontSize: '0.9375rem' }}
            >
              {checking ? 'Verifying...' : 'Reset Passcode'}
            </button>

            <button
              onClick={() => { setShowRecovery(false); setError(''); }}
              style={{ background: 'none', border: 'none', color: '#6BAED4', cursor: 'pointer', fontSize: '0.875rem', padding: '0.5rem' }}
            >
              ← Back to PIN
            </button>
          </div>
        </div>
      </div>
    );
  }

  // ── PIN screen ─────────────────────────────────────────────────────────────
  return (
    <div style={page}>
      <div style={card}>

        {/* Logo */}
        <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '1.25rem' }}>
          <RadafiqLogo size={80} />
        </div>

        {/* Title — white text, explicit color so it's always visible */}
        <h2 style={{ color: '#E8F4FF', marginBottom: 6, fontSize: '1.5rem', fontWeight: 700 }}>
          Radafiq is Locked
        </h2>
        <p style={{ color: '#6BAED4', fontSize: '0.875rem', marginBottom: '1.75rem' }}>
          Enter your 6-digit passcode to continue.
        </p>

        {/* PIN dots */}
        <div style={{ display: 'flex', justifyContent: 'center', gap: 14, marginBottom: '1.25rem' }}>
          {Array.from({ length: 6 }, (_, i) => (
            <div key={i} style={{
              width: 14, height: 14, borderRadius: '50%', flexShrink: 0,
              background: i < passcode.length ? '#1A8FD4' : 'rgba(26,171,207,0.25)',
              transition: 'background 0.12s',
            }} />
          ))}
        </div>

        {/* Error */}
        <div style={{ minHeight: 24, marginBottom: '0.75rem' }}>
          {error && <p style={{ color: '#E8445A', fontSize: '0.875rem' }}>{error}</p>}
        </div>

        {/* Numpad */}
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(3, 1fr)',
          gap: 10,
          width: '100%',
          maxWidth: 300,
          margin: '0 auto 1.5rem',
        }}>
          {KEYS.map((key, i) => {
            if (key === '') return <div key={i} />;
            const isBack = key === '⌫';
            const disabled = checking || (isBack ? passcode.length === 0 : passcode.length >= 6);
            return (
              <button
                key={i}
                type="button"
                aria-label={isBack ? 'Backspace' : `Digit ${key}`}
                style={numpadBtn(disabled, isBack)}
                onPointerDown={(e) => {
                  e.preventDefault();
                  if (disabled) return;
                  if (isBack) pressBackspace();
                  else pressDigit(key);
                }}
                onClick={(e) => {
                  e.preventDefault();
                }}
              >
                {key}
              </button>
            );
          })}
        </div>

        {/* Forgot passcode */}
        {security.hasRecoveryQuestion && (
          <button
            type="button"
            onClick={() => { setShowRecovery(true); setError(''); setPasscode(''); }}
            style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#1A8FD4', fontSize: '0.875rem', fontWeight: 500, padding: '0.5rem' }}
          >
            Forgot passcode?
          </button>
        )}
      </div>
    </div>
  );
}
