import React, { useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { TrendingUp, TrendingDown, Settings } from 'lucide-react';
import { useApp } from '../context/AppContext';
import { formatMoney, getGreeting, getInitials } from '../utils/format';
import { CardSummary, CustomerSummary, isVisibleInTransactions, isScheduledForFutureMonth } from '../types/models';

function HeroPanel({ title, amount, subtitle }: { title: string; amount: string; subtitle: string }) {
  return (
    <div className="hero-panel" style={{ marginBottom: '1rem' }}>
      <p style={{ fontSize: '0.8125rem', fontWeight: 600, opacity: 0.8, textTransform: 'uppercase', letterSpacing: '0.04em', marginBottom: 8 }}>{title}</p>
      <h1 style={{ fontSize: '2.25rem', fontWeight: 800, marginBottom: 8 }}>{amount}</h1>
      <p style={{ fontSize: '0.875rem', opacity: 0.75 }}>{subtitle}</p>
    </div>
  );
}

function ActivityCard({ card, currentDue, emiOutstanding }: {
  card: CardSummary;
  currentDue: number;
  emiOutstanding: number;
}) {
  const isCredit = card.accountKind === 'credit_card';
  const accentColor = isCredit ? 'var(--warning)' : 'var(--secondary)';

  return (
    <div className="flow-card" style={{ '--card-accent': accentColor, marginBottom: '0.75rem' } as React.CSSProperties}>
      <div style={{ display: 'flex', alignItems: 'flex-start', gap: 12 }}>
        <div style={{
          width: 42, height: 42, borderRadius: '50%',
          background: `color-mix(in srgb, ${accentColor} 15%, transparent)`,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          flexShrink: 0, marginTop: 2,
        }}>
          {isCredit ? <TrendingUp size={18} color={accentColor} /> : <TrendingDown size={18} color={accentColor} />}
        </div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div className="truncate font-semibold">{card.name}</div>
          <div className="text-muted text-sm">
            {isCredit ? 'Credit Card' : 'Bank Account'}
            {card.dueDate && ` • Due ${card.dueDate}`}
          </div>

          {/* Credit card: Current Due (non-EMI only) + EMI Outstanding (all EMIs) */}
          {isCredit ? (
            <div style={{ display: 'flex', gap: 8, marginTop: 8, flexWrap: 'wrap' }}>
              {/* Current Due — non-EMI unpaid transactions */}
              <div style={{
                flex: 1, minWidth: 100,
                background: 'color-mix(in srgb, var(--warning) 8%, transparent)',
                border: '1px solid color-mix(in srgb, var(--warning) 20%, transparent)',
                borderRadius: 8, padding: '6px 10px',
              }}>
                <div style={{ fontSize: '0.6875rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
                  Current Due
                </div>
                <div style={{ fontSize: '0.9375rem', fontWeight: 700, color: 'var(--warning)', marginTop: 2 }}>
                  {formatMoney(currentDue)}
                </div>
              </div>
              {/* EMI Outstanding — all unpaid EMI installments */}
              {emiOutstanding > 0 && (
                <div style={{
                  flex: 1, minWidth: 100,
                  background: 'color-mix(in srgb, var(--primary) 8%, transparent)',
                  border: '1px solid color-mix(in srgb, var(--primary) 20%, transparent)',
                  borderRadius: 8, padding: '6px 10px',
                }}>
                  <div style={{ fontSize: '0.6875rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
                    EMI Outstanding
                  </div>
                  <div style={{ fontSize: '0.9375rem', fontWeight: 700, color: 'var(--primary)', marginTop: 2 }}>
                    {formatMoney(emiOutstanding)}
                  </div>
                </div>
              )}
            </div>
          ) : (
            <div style={{ marginTop: 6, fontWeight: 700, fontSize: '1rem', color: accentColor }}>
              +{formatMoney(card.payable)}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

interface PersonSummary {
  personId: string;
  personName: string;
  totalUsed: number;
  totalDue: number;
}

function PersonCard({ person }: { person: PersonSummary }) {
  const initials = getInitials(person.personName);
  return (
    <div className="flow-card" style={{ '--card-accent': 'var(--primary)', marginBottom: '0.75rem' } as React.CSSProperties}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        <div className="avatar">{initials}</div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div className="truncate font-semibold">{person.personName}</div>
          <div className="text-muted text-sm">Person • Used {formatMoney(person.totalUsed)}</div>
        </div>
        <div style={{ textAlign: 'right', flexShrink: 0 }}>
          <div style={{ fontWeight: 700, color: person.totalDue > 0 ? 'var(--warning)' : 'var(--primary)' }}>
            {formatMoney(person.totalDue)}
          </div>
          <div className="text-muted text-xs">{person.totalDue > 0 ? 'Due' : 'Settled'}</div>
        </div>
      </div>
    </div>
  );
}

export default function Dashboard() {
  const { cards, customers, profile } = useApp();
  const navigate = useNavigate();

  const usedAccountIds = useMemo(() =>
    new Set(customers.flatMap(c => c.transactions.map(t => t.accountId))),
    [customers]
  );

  const visibleCards = useMemo(() =>
    cards.filter(c => usedAccountIds.has(c.id) && c.accountKind !== 'person'),
    [cards, usedAccountIds]
  );

  const totalUsed = visibleCards.reduce((s, c) => s + c.bill, 0);
  const totalPaid = visibleCards.reduce((s, c) => s + c.pending, 0);
  const totalBalance = visibleCards.reduce((s, c) => s + c.payable, 0);

  // Compute per-account breakdowns from customer transactions:
  //   emiOutstandingByAccount  — ALL unpaid EMI installments (past + current + future)
  //   nonEmiDueByAccount       — unpaid non-EMI transactions only (shown as "Current Due")
  const { emiOutstandingByAccount, nonEmiDueByAccount } = useMemo(() => {
    const emiMap = new Map<string, number>();
    const nonEmiMap = new Map<string, number>();

    customers.forEach(customer => {
      customer.transactions.forEach(t => {
        if (t.accountKind === 'person') return; // person transactions not on cards
        const due = t.isSettled ? 0 : Math.max(0, t.amount - t.partialPaidAmount);
        if (due <= 0) return;

        if (t.emiGroupId) {
          // EMI — goes entirely into EMI Outstanding regardless of month
          emiMap.set(t.accountId, (emiMap.get(t.accountId) ?? 0) + due);
        } else {
          // Non-EMI visible transaction — goes into Current Due
          const today = new Date();
          if (!isScheduledForFutureMonth(t, today)) {
            nonEmiMap.set(t.accountId, (nonEmiMap.get(t.accountId) ?? 0) + due);
          }
        }
      });
    });

    return { emiOutstandingByAccount: emiMap, nonEmiDueByAccount: nonEmiMap };
  }, [customers]);

  const personSummaries = useMemo((): PersonSummary[] => {
    const map = new Map<string, PersonSummary>();
    customers.forEach(customer => {
      customer.transactions
        .filter(t => t.accountKind === 'person' && isVisibleInTransactions(t))
        .forEach(t => {
          const key = t.accountId;
          const name = t.personName || t.accountName;
          const due = t.isSettled ? 0 : Math.max(0, t.amount - t.partialPaidAmount);
          const existing = map.get(key);
          if (!existing) {
            map.set(key, { personId: key, personName: name, totalUsed: t.amount, totalDue: due });
          } else {
            map.set(key, { ...existing, totalUsed: existing.totalUsed + t.amount, totalDue: existing.totalDue + due });
          }
        });
    });
    return Array.from(map.values()).sort((a, b) => b.totalDue - a.totalDue);
  }, [customers]);

  const greeting = getGreeting();
  const name = profile?.displayName?.trim() || 'Your Profile';
  const initials = getInitials(name);

  return (
    <div className="page-content">
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1.5rem' }}>
        <div>
          <p className="text-muted text-sm">{greeting}</p>
          <h2 style={{ marginTop: 2 }}>{name}</h2>
        </div>
        <button
          className="avatar"
          style={{ cursor: 'pointer', border: '1px solid rgba(255,255,255,0.3)' }}
          onClick={() => navigate('/settings')}
          title="Settings"
          aria-label="Open settings"
        >
          {profile?.photoUrl ? (
            <img
              src={profile.photoUrl}
              alt=""
              referrerPolicy="no-referrer"
              style={{ width: '100%', height: '100%', borderRadius: '50%', objectFit: 'cover' }}
            />
          ) : initials}
        </button>
      </div>

      {/* Hero */}
      <HeroPanel
        title="Outstanding Balance"
        amount={formatMoney(totalBalance)}
        subtitle={`${visibleCards.length} active account(s) contributing to your ledger flow.`}
      />

      {/* Metrics */}
      <div className="two-col" style={{ marginBottom: '1.5rem' }}>
        <div className="metric-pill">
          <span className="label">Total Used</span>
          <span className="value" style={{ color: 'var(--warning)' }}>{formatMoney(totalUsed)}</span>
        </div>
        <div className="metric-pill">
          <span className="label">Total Paid</span>
          <span className="value" style={{ color: 'var(--secondary)' }}>{formatMoney(totalPaid)}</span>
        </div>
      </div>

      {/* Account Activity */}
      <div style={{ marginBottom: '1rem' }}>
        <h3 style={{ marginBottom: 4 }}>Account Activity</h3>
        <p className="text-muted text-sm" style={{ marginBottom: '1rem' }}>Live summary of your accounts and person balances.</p>

        {visibleCards.length === 0 && personSummaries.length === 0 ? (
          <div className="empty-state">
            <div className="empty-state-icon">📊</div>
            <h3>No activity yet</h3>
            <p>As customers and payments are recorded, activity cards will appear here.</p>
          </div>
        ) : (
          <>
            {/* BUG-35 fix: sort first, then slice */}
            {[...visibleCards].sort((a, b) => b.payable - a.payable).slice(0, 6).map(card => (
              <ActivityCard
                key={card.id}
                card={card}
                currentDue={nonEmiDueByAccount.get(card.id) ?? 0}
                emiOutstanding={emiOutstandingByAccount.get(card.id) ?? 0}
              />
            ))}
            {personSummaries.slice(0, 6).map(person => (
              <PersonCard key={person.personId} person={person} />
            ))}
          </>
        )}
      </div>
    </div>
  );
}
