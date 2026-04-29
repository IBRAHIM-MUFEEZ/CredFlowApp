import React, { useState, useMemo, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { Search, Plus, RefreshCw, Trash2, RotateCcw, Settings } from 'lucide-react';
import { useApp } from '../context/AppContext';
import { formatMoney, getInitials } from '../utils/format';
import { CustomerSummary } from '../types/models';

function CustomerRow({ customer, onClick }: { customer: CustomerSummary; onClick: () => void }) {
  const txnCount = customer.transactions.length;
  return (
    <div
      className="flow-card"
      style={{ cursor: 'pointer', marginBottom: '0.75rem' }}
      onClick={onClick}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        <div className="avatar">
          {getInitials(customer.name)}
        </div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div className="truncate font-semibold">{customer.name}</div>
          <div className="text-muted text-sm">{txnCount} transaction{txnCount !== 1 ? 's' : ''}</div>
        </div>
        <div style={{ textAlign: 'right', flexShrink: 0 }}>
          <div style={{ fontWeight: 700, color: customer.balance > 0 ? 'var(--warning)' : 'var(--primary)' }}>
            {formatMoney(customer.balance)}
          </div>
          <div className="text-muted text-xs">Balance</div>
        </div>
      </div>
    </div>
  );
}

function DeletedCustomerRow({ customer, onRestore, onDelete }: {
  customer: CustomerSummary;
  onRestore: () => void;
  onDelete: () => void;
}) {
  return (
    <div className="flow-card" style={{ '--card-accent': 'var(--red)', marginBottom: '0.75rem' } as React.CSSProperties}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        <div className="avatar" style={{ background: 'rgba(245,87,108,0.15)', color: 'var(--red)' }}>
          {customer.name.slice(0, 2).toUpperCase()}
        </div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div className="truncate font-semibold">{customer.name}</div>
          <div className="text-muted text-sm">{customer.transactions.length} transaction(s)</div>
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          <button className="btn btn-sm btn-outline" onClick={onRestore} title="Restore">
            <RotateCcw size={14} />
          </button>
          <button className="btn btn-sm btn-danger" onClick={onDelete} title="Delete forever">
            <Trash2 size={14} />
          </button>
        </div>
      </div>
    </div>
  );
}

export default function CustomersPage() {
  const { customers, deletedCustomers, addCustomer, restoreCustomer, permanentlyDeleteCustomer, syncStatus, triggerSync } = useApp();
  const navigate = useNavigate();
  const [search, setSearch] = useState('');
  const [showRecycleBin, setShowRecycleBin] = useState(false);
  const [showAddModal, setShowAddModal] = useState(false);
  const [newName, setNewName] = useState('');
  const [adding, setAdding] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState<CustomerSummary | null>(null);
  const letterRefs = useRef<Record<string, HTMLDivElement | null>>({});

  const activeList = showRecycleBin ? deletedCustomers : customers;

  const sorted = useMemo(() =>
    [...activeList].sort((a, b) => a.name.localeCompare(b.name)),
    [activeList]
  );

  const filtered = useMemo(() =>
    search ? sorted.filter(c => c.name.toLowerCase().includes(search.toLowerCase())) : sorted,
    [sorted, search]
  );

  const letters = useMemo(() => {
    if (search) return [];
    const seen = new Set<string>();
    return filtered.map(c => {
      const l = c.name[0]?.toUpperCase() ?? '#';
      const bucket = /[A-Z]/.test(l) ? l : '#';
      if (!seen.has(bucket)) { seen.add(bucket); return bucket; }
      return null;
    }).filter(Boolean) as string[];
  }, [filtered, search]);

  const handleAdd = async () => {
    if (!newName.trim()) return;
    setAdding(true);
    try {
      const id = await addCustomer(newName);
      setShowAddModal(false);
      setNewName('');
      navigate(`/customers/${id}`);
    } finally {
      setAdding(false);
    }
  };

  const scrollToLetter = (letter: string) => {
    letterRefs.current[letter]?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  };

  // Group by letter
  const grouped = useMemo(() => {
    if (search) return [{ letter: '', items: filtered }];
    const map = new Map<string, CustomerSummary[]>();
    filtered.forEach(c => {
      const l = c.name[0]?.toUpperCase() ?? '#';
      const bucket = /[A-Z]/.test(l) ? l : '#';
      if (!map.has(bucket)) map.set(bucket, []);
      map.get(bucket)!.push(c);
    });
    return Array.from(map.entries()).map(([letter, items]) => ({ letter, items }));
  }, [filtered, search]);

  return (
    <div className="page-content" style={{ position: 'relative' }}>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: '1rem' }}>
        <div>
          <h2>{showRecycleBin ? 'Recycle Bin' : 'Customers'}</h2>
          <p className="text-muted text-sm" style={{ marginTop: 4 }}>
            {showRecycleBin ? 'Restore or permanently delete customers.' : 'Manage customer ledgers and transactions.'}
          </p>
        </div>
        <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
          {syncStatus.message && (
            <span className={`text-xs ${syncStatus.state === 'ERROR' ? 'text-error' : syncStatus.state === 'SUCCESS' ? 'text-success' : 'text-muted'}`}>
              {syncStatus.message}
            </span>
          )}
          <button className="btn btn-ghost" onClick={triggerSync} title="Sync">
            <RefreshCw size={18} className={syncStatus.state === 'SYNCING' ? 'rotating' : ''} />
          </button>
          <button className="btn btn-sm btn-outline" onClick={() => setShowRecycleBin(v => !v)}>
            {showRecycleBin ? 'Customers' : 'Recycle Bin'}
          </button>
          <button className="btn btn-ghost" onClick={() => navigate('/settings')}>
            <Settings size={18} />
          </button>
        </div>
      </div>

      {/* Search */}
      <div style={{ position: 'relative', marginBottom: '1rem' }}>
        <Search size={16} style={{ position: 'absolute', left: 14, top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
        <input
          className="form-input"
          style={{ paddingLeft: 40 }}
          placeholder="Search customers..."
          value={search}
          onChange={e => setSearch(e.target.value)}
        />
      </div>

      {/* List */}
      {filtered.length === 0 ? (
        <div className="empty-state">
          <div className="empty-state-icon">👥</div>
          <h3>{showRecycleBin ? 'Recycle bin is empty' : 'No customers yet'}</h3>
          <p>{showRecycleBin ? 'Deleted customers will appear here.' : 'Tap + to add your first customer ledger.'}</p>
        </div>
      ) : (
        grouped.map(({ letter, items }) => (
          <div key={letter || 'all'}>
            {letter && (
              <div
                ref={el => { letterRefs.current[letter] = el; }}
                style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--primary)', padding: '4px 0 2px 4px' }}
              >
                {letter}
              </div>
            )}
            {items.map(customer => (
              showRecycleBin ? (
                <DeletedCustomerRow
                  key={customer.id}
                  customer={customer}
                  onRestore={() => restoreCustomer(customer.id)}
                  onDelete={() => setConfirmDelete(customer)}
                />
              ) : (
                <CustomerRow
                  key={customer.id}
                  customer={customer}
                  onClick={() => navigate(`/customers/${customer.id}`)}
                />
              )
            ))}
          </div>
        ))
      )}

      {/* Alphabet index */}
      {!showRecycleBin && !search && letters.length > 0 && (
        <div className="alpha-index">
          {letters.map(l => (
            <div key={l} className="alpha-index-letter" onClick={() => scrollToLetter(l)}>{l}</div>
          ))}
        </div>
      )}

      {/* FAB */}
      {!showRecycleBin && (
        <button
          className="btn btn-primary"
          style={{
            position: 'fixed', bottom: 80, right: 24,
            borderRadius: 20, padding: '0.875rem 1.25rem',
            boxShadow: '0 4px 20px rgba(102,126,234,0.4)',
            zIndex: 50,
          }}
          onClick={() => setShowAddModal(true)}
        >
          <Plus size={18} /> Add Customer
        </button>
      )}

      {/* Add modal */}
      {showAddModal && (
        <div className="modal-overlay" onClick={() => setShowAddModal(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h3 className="modal-title">Add Customer</h3>
            <p className="modal-subtitle">Enter the customer's name to create a new ledger.</p>
            <div className="form-group">
              <label className="form-label">Customer Name</label>
              <input
                className="form-input"
                value={newName}
                onChange={e => setNewName(e.target.value)}
                placeholder="Enter name"
                autoFocus
                onKeyDown={e => e.key === 'Enter' && handleAdd()}
              />
            </div>
            <div className="modal-actions">
              <button className="btn btn-outline" onClick={() => setShowAddModal(false)}>Cancel</button>
              <button className="btn btn-primary" onClick={handleAdd} disabled={!newName.trim() || adding}>
                {adding ? 'Adding...' : 'Add Customer'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Confirm delete */}
      {confirmDelete && (
        <div className="modal-overlay" onClick={() => setConfirmDelete(null)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h3 className="modal-title">Delete Forever?</h3>
            <p className="modal-subtitle">
              This will permanently delete <strong>{confirmDelete.name}</strong> and all their transactions. This cannot be undone.
            </p>
            <div className="modal-actions">
              <button className="btn btn-outline" onClick={() => setConfirmDelete(null)}>Cancel</button>
              <button
                className="btn btn-danger"
                onClick={async () => {
                  await permanentlyDeleteCustomer(confirmDelete.id, confirmDelete.name);
                  setConfirmDelete(null);
                }}
              >
                Delete Forever
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
