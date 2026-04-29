import React, { useMemo } from 'react';
import { useApp } from '../context/AppContext';
import { formatMoney, formatDate } from '../utils/format';
import { CustomerTransaction } from '../types/models';

interface EmiRow {
  groupId: string;
  transactionId: string;
  customerName: string;
  planName: string;
  accountName: string;
  emiIndex: number;
  emiTotal: number;
  amount: number;
  transactionDate: string;
  isSettled: boolean;
  isPast: boolean;
  isCurrent: boolean;
}

export default function EmiSchedulePage() {
  const { customers, toggleTransactionSettled } = useApp();

  const today = useMemo(() => new Date(), []);

  const allRows = useMemo((): EmiRow[] => {
    const result: EmiRow[] = [];
    customers.forEach(customer => {
      const emiGroups = new Map<string, CustomerTransaction[]>();
      customer.transactions
        .filter(t => t.emiGroupId)
        .forEach(t => {
          const key = t.emiGroupId || `${t.name.split(' — EMI')[0]}_${customer.name}`;
          if (!emiGroups.has(key)) emiGroups.set(key, []);
          emiGroups.get(key)!.push(t);
        });

      emiGroups.forEach((instalments, groupKey) => {
        instalments.sort((a, b) => a.transactionDate.localeCompare(b.transactionDate));
        instalments.forEach(t => {
          const date = new Date(t.transactionDate);
          const isPast = isNaN(date.getTime()) || date <= today;
          const isCurrent = !isNaN(date.getTime()) && date.getFullYear() === today.getFullYear() && date.getMonth() === today.getMonth();
          const planName = t.name.split(' — EMI')[0].trim();
          result.push({
            groupId: groupKey,
            transactionId: t.id,
            customerName: customer.name,
            planName,
            accountName: t.accountName,
            emiIndex: t.emiIndex,
            emiTotal: t.emiTotal,
            amount: t.amount,
            transactionDate: t.transactionDate,
            isSettled: t.isSettled,
            isPast,
            isCurrent,
          });
        });
      });
    });
    return result.sort((a, b) => a.transactionDate.localeCompare(b.transactionDate));
  }, [customers, today]);

  // Group by plan
  const grouped = useMemo(() => {
    const map = new Map<string, EmiRow[]>();
    allRows.forEach(row => {
      const key = `${row.groupId}`;
      if (!map.has(key)) map.set(key, []);
      map.get(key)!.push(row);
    });
    return Array.from(map.entries()).map(([key, rows]) => ({ key, rows }));
  }, [allRows]);

  return (
    <div className="page-content">
      <div style={{ marginBottom: '1.5rem' }}>
        <h2>EMI Schedule</h2>
        <p className="text-muted text-sm" style={{ marginTop: 4 }}>All EMI installments across customers.</p>
      </div>

      {grouped.length === 0 ? (
        <div className="empty-state">
          <div className="empty-state-icon">📅</div>
          <h3>No EMI plans yet</h3>
          <p>Add EMI transactions from a customer's detail page to see them here.</p>
        </div>
      ) : (
        grouped.map(({ key, rows }) => {
          const first = rows[0];
          const totalAmount = rows.reduce((s, r) => s + r.amount, 0);
          const settledCount = rows.filter(r => r.isSettled).length;
          return (
            <div key={key} className="flow-card" style={{ marginBottom: '1rem' }}>
              <div style={{ marginBottom: '0.75rem' }}>
                <div className="font-semibold">{first.planName}</div>
                <div className="text-muted text-sm">
                  {first.customerName} • {first.accountName} • {settledCount}/{rows.length} settled
                </div>
                <div className="text-muted text-xs" style={{ marginTop: 2 }}>
                  Total: {formatMoney(totalAmount)}
                </div>
              </div>

              <div className="progress-bar" style={{ marginBottom: '0.75rem' }}>
                <div
                  className="progress-fill"
                  style={{ width: `${(settledCount / rows.length) * 100}%`, background: 'var(--primary)' }}
                />
              </div>

              {rows.map(row => {
                // BUG-54 fix: current-month items should show primary, not warning (overdue)
                const statusColor = row.isSettled
                  ? 'var(--green)'
                  : row.isCurrent
                  ? 'var(--primary)'
                  : row.isPast
                  ? 'var(--warning)'
                  : 'var(--text-muted)';
                return (
                  <div key={row.transactionId} style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '0.5rem 0', borderBottom: '1px solid var(--outline)' }}>
                    <div style={{
                      width: 28, height: 28, borderRadius: '50%',
                      background: `${statusColor}22`,
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                      fontSize: '0.75rem', fontWeight: 700, color: statusColor,
                      flexShrink: 0,
                    }}>
                      {row.emiIndex + 1}
                    </div>
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div className="text-sm font-semibold">EMI {row.emiIndex + 1}/{row.emiTotal}</div>
                      <div className="text-muted text-xs">{formatDate(row.transactionDate)}</div>
                    </div>
                    <div style={{ textAlign: 'right', flexShrink: 0 }}>
                      <div style={{ fontWeight: 700, color: statusColor }}>{formatMoney(row.amount)}</div>
                      <div className="text-xs" style={{ color: statusColor }}>
                        {row.isSettled ? '✓ Settled' : row.isCurrent ? 'Current' : row.isPast ? 'Overdue' : 'Upcoming'}
                      </div>
                    </div>
                    <button
                      className="btn btn-ghost btn-sm"
                      style={{ color: row.isSettled ? 'var(--green)' : 'var(--text-muted)', padding: '4px 8px' }}
                      onClick={async () => await toggleTransactionSettled(row.transactionId, !row.isSettled)}
                      title={row.isSettled ? 'Mark unsettled' : 'Mark settled'}
                    >
                      {row.isSettled ? '✓' : '○'}
                    </button>
                  </div>
                );
              })}
            </div>
          );
        })
      )}
    </div>
  );
}
