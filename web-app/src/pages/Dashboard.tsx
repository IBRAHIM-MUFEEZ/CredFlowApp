import React, { useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { TrendingUp, TrendingDown, Settings } from 'lucide-react';
import { useApp } from '../context/AppContext';
import { formatMoney, getGreeting, getInitials } from '../utils/format';
import { CardSummary, CustomerSummary, isVisibleInTransactions } from '../types/models';

function HeroPanel({ title, amount, subtitle }: { title: string; amount: string; subtitle: string }) {
  return (
    <div className="hero-panel" style={{ marginBottom: '1rem' }}>
      <p style={{ fontSize: '0.8125rem', fontWeight: 600, opacity: 0.8, textTransform: 'uppercase', letterSpacing: '0.04em', marginBottom: 8 }}>{title}</p>
      <h1 style={{ fontSize: '2.25rem', fontWeight: 800, marginBottom: 8 }}>{amount}</h1>
      <p style={{ fontSize: '0.875rem', opacity: 0.75 }}>{subtitle}</p>
    </div>
  );
}

function ActivityCard({ card }: { card: CardSummary }) {
  const isOutgoing = card.accountKind === 'credit_card';
  const accentColor = isOutgoing ? 'var(--warning)' : 'var(--secondary)';
  return (
    <div className="flow-card" style={{ '--card-accent': accentColor, marginBottom: '0.75rem' } as React.CSSProperties}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        <div style={{
          width: 42, height: 42, borderRadius: '50%',
          background: `${accentColor}22`,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          flexShrink: 0,
        }}>
          {isOutgoing ? <TrendingUp size={18} color={accentColor} /> : <TrendingDown size={18} color={accentColor} />}
        </div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div className="truncate font-semibold">{card.name}</div>
          <div className="text-muted text-sm">
            {card.accountKind === 'credit_card' ? 'Credit Card' : 'Bank Account'}
            {card.dueDate && ` / Due ${card.dueDate}`}
          </div>
        </div>
        <div style={{ textAlign: 'right', flexShrink: 0 }}>
          <div style={{ fontWeight: 700, color: accentColor }}>
            {isOutgoing ? '-' : '+'}{formatMoney(card.payable)}
          </div>
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
              <ActivityCard key={card.id} card={card} />
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
