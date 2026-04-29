import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Plus, Minus, Trash2 } from 'lucide-react';
import { useApp } from '../context/AppContext';
import { formatMoney, formatDate } from '../utils/format';
import { SavingsEntry } from '../types/models';

export default function SavingsPage() {
  const { customerId } = useParams<{ customerId: string }>();
  const navigate = useNavigate();
  const { customers, addSavingsDeposit, addSavingsWithdrawal, deleteSavingsEntry } = useApp();

  const customer = customers.find(c => c.id === customerId);
  const [showDeposit, setShowDeposit] = useState(false);
  const [showWithdraw, setShowWithdraw] = useState(false);
  const [amount, setAmount] = useState('');
  const [note, setNote] = useState('');
  const [saving, setSaving] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState<SavingsEntry | null>(null);

  if (!customer) {
    return (
      <div className="page-content">
        <button className="btn btn-ghost" onClick={() => navigate(-1)}><ArrowLeft size={18} /> Back</button>
        <div className="empty-state"><h3>Customer not found</h3></div>
      </div>
    );
  }

  const totalDeposited = customer.savingsEntries.filter(e => e.type === 'deposit').reduce((s, e) => s + e.amount, 0);
  const totalWithdrawn = customer.savingsEntries.filter(e => e.type === 'withdrawal').reduce((s, e) => s + e.amount, 0);

  const handleDeposit = async () => {
    if (!amount || parseFloat(amount) <= 0) return;
    setSaving(true);
    try {
      await addSavingsDeposit(customer.id, customer.name, amount, note);
      setShowDeposit(false);
      setAmount('');
      setNote('');
    } finally {
      setSaving(false);
    }
  };

  const handleWithdraw = async () => {
    if (!amount || parseFloat(amount) <= 0) return;
    setSaving(true);
    try {
      await addSavingsWithdrawal(customer.id, customer.name, amount, note);
      setShowWithdraw(false);
      setAmount('');
      setNote('');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="page-content">
      <button className="btn btn-ghost" style={{ marginBottom: '1rem' }} onClick={() => navigate(`/customers/${customer.id}`)}>
        <ArrowLeft size={18} /> {customer.name}
      </button>

      {/* Balance card */}
      <div className="flow-card" style={{ marginBottom: '1rem' }}>
        <p className="text-muted text-xs" style={{ textTransform: 'uppercase', letterSpacing: '0.04em', marginBottom: 6 }}>Available Balance</p>
        <h1 style={{ fontSize: '2.5rem', fontWeight: 800, color: 'var(--primary)', marginBottom: 4 }}>
          {formatMoney(customer.savingsBalance)}
        </h1>
        <p className="text-muted text-sm" style={{ marginBottom: '1.25rem' }}>Bank account savings — not a loan</p>

        <div className="two-col" style={{ marginBottom: '1.25rem' }}>
          <div className="metric-pill">
            <span className="label">Total Deposited</span>
            <span className="value text-primary">{formatMoney(totalDeposited)}</span>
          </div>
          <div className="metric-pill">
            <span className="label">Total Withdrawn</span>
            <span className="value" style={{ color: 'var(--warning)' }}>{formatMoney(totalWithdrawn)}</span>
          </div>
        </div>

        <div style={{ display: 'flex', gap: 10 }}>
          <button className="btn btn-primary" style={{ flex: 1 }} onClick={() => { setAmount(''); setNote(''); setShowDeposit(true); }}>
            <Plus size={16} /> Deposit
          </button>
          <button
            className="btn"
            style={{ flex: 1, background: 'var(--warning)', color: 'white' }}
            disabled={customer.savingsBalance <= 0}
            onClick={() => { setAmount(''); setNote(''); setShowWithdraw(true); }}
          >
            <Minus size={16} /> Withdraw
          </button>
        </div>
      </div>

      {/* History */}
      <h3 style={{ marginBottom: '0.75rem', textTransform: 'uppercase', fontSize: '0.75rem', letterSpacing: '0.04em', color: 'var(--text-muted)' }}>
        History
      </h3>

      {customer.savingsEntries.length === 0 ? (
        <div className="empty-state">
          <div className="empty-state-icon">🐷</div>
          <h3>No savings yet</h3>
          <p>Tap Deposit to record the first deposit for {customer.name}.</p>
        </div>
      ) : (
        customer.savingsEntries.map(entry => {
          const isDeposit = entry.type === 'deposit';
          const accentColor = isDeposit ? 'var(--primary)' : 'var(--warning)';
          return (
            <div key={entry.id} className="flow-card" style={{ '--card-accent': accentColor, marginBottom: '0.75rem' } as React.CSSProperties}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                <div style={{
                  width: 36, height: 36, borderRadius: '50%',
                  background: `${accentColor}22`,
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  flexShrink: 0,
                }}>
                  {isDeposit ? <Plus size={16} color={accentColor} /> : <Minus size={16} color={accentColor} />}
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontWeight: 600, color: accentColor }}>{isDeposit ? 'Deposit' : 'Withdrawal'}</div>
                  {entry.note && <div className="text-muted text-sm truncate">{entry.note}</div>}
                  <div className="text-muted text-xs">{formatDate(entry.date)}</div>
                </div>
                <div style={{ textAlign: 'right', flexShrink: 0 }}>
                  <div style={{ fontWeight: 700, color: accentColor }}>
                    {isDeposit ? '+' : '-'}{formatMoney(entry.amount)}
                  </div>
                </div>
                <button
                  className="btn btn-ghost btn-sm"
                  style={{ color: 'var(--red)' }}
                  onClick={() => setConfirmDelete(entry)}
                >
                  <Trash2 size={14} />
                </button>
              </div>
            </div>
          );
        })
      )}

      {/* Deposit modal */}
      {showDeposit && (
        <div className="modal-overlay" onClick={() => setShowDeposit(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h3 className="modal-title">Deposit</h3>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.875rem', marginBottom: '1rem' }}>
              <div className="form-group">
                <label className="form-label">Amount</label>
                <input className="form-input" type="number" value={amount} onChange={e => setAmount(e.target.value)} placeholder="0.00" autoFocus />
              </div>
              <div className="form-group">
                <label className="form-label">Note (optional)</label>
                <input className="form-input" value={note} onChange={e => setNote(e.target.value)} placeholder="Add a note" />
              </div>
            </div>
            <div className="modal-actions">
              <button className="btn btn-outline" onClick={() => setShowDeposit(false)}>Cancel</button>
              <button className="btn btn-primary" onClick={handleDeposit} disabled={saving || !amount || parseFloat(amount) <= 0}>
                {saving ? 'Saving...' : 'Deposit'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Withdraw modal */}
      {showWithdraw && (
        <div className="modal-overlay" onClick={() => setShowWithdraw(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h3 className="modal-title">Withdraw</h3>
            <p className="modal-subtitle">Available: {formatMoney(customer.savingsBalance)}</p>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.875rem', marginBottom: '1rem' }}>
              <div className="form-group">
                <label className="form-label">Amount</label>
                <input
                  className={`form-input${amount && parseFloat(amount) > customer.savingsBalance ? ' error' : ''}`}
                  type="number"
                  value={amount}
                  onChange={e => setAmount(e.target.value)}
                  placeholder="0.00"
                  autoFocus
                />
              </div>
              <div className="form-group">
                <label className="form-label">Note (optional)</label>
                <input className="form-input" value={note} onChange={e => setNote(e.target.value)} placeholder="Add a note" />
              </div>
            </div>
            <div className="modal-actions">
              <button className="btn btn-outline" onClick={() => setShowWithdraw(false)}>Cancel</button>
              <button
                className="btn"
                style={{ background: 'var(--warning)', color: 'white' }}
                onClick={handleWithdraw}
                disabled={saving || !amount || parseFloat(amount) <= 0 || parseFloat(amount) > customer.savingsBalance}
              >
                {saving ? 'Saving...' : 'Withdraw'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Confirm delete */}
      {confirmDelete && (
        <div className="modal-overlay" onClick={() => setConfirmDelete(null)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h3 className="modal-title">Delete Entry?</h3>
            <p className="modal-subtitle">
              Remove this {confirmDelete.type} of {formatMoney(confirmDelete.amount)} on {formatDate(confirmDelete.date)}? This cannot be undone.
            </p>
            <div className="modal-actions">
              <button className="btn btn-outline" onClick={() => setConfirmDelete(null)}>Cancel</button>
              <button className="btn btn-danger" onClick={() => { deleteSavingsEntry(confirmDelete.id); setConfirmDelete(null); }}>
                Delete
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
