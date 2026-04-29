import React, { useState, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Plus, Trash2, Check, PiggyBank, ChevronDown } from 'lucide-react';
import { useApp } from '../context/AppContext';
import { formatMoney, formatDate, todayString, evaluateExpression } from '../utils/format';
import { CustomerTransaction, isVisibleInTransactions, isEmi, isSplit, AccountKind, SplitEntry, ACCOUNT_KIND_LABELS, getAccountOptions, ALL_ACCOUNTS } from '../types/models';

type TxnFilter = 'ALL' | 'bank_account' | 'credit_card' | 'person';

function TransactionRow({
  txn,
  onSettle,
  onPartialPay,
  onDelete,
  onEdit,
}: {
  txn: CustomerTransaction;
  onSettle: (id: string, settled: boolean) => void;
  onPartialPay: (id: string) => void;
  onDelete: (id: string) => void;
  onEdit: (txn: CustomerTransaction) => void;
}) {
  const statusColor = txn.isSettled ? 'var(--green)' : txn.partialPaidAmount > 0 ? 'var(--orange)' : 'var(--red)';
  const remaining = Math.max(0, txn.amount - txn.partialPaidAmount);

  return (
    <div className="txn-row">
      <div className="txn-dot" style={{ background: statusColor }} />
      <div className="txn-info">
        <div className="txn-name">{txn.name}</div>
        <div className="txn-meta">
          {txn.accountName} • {formatDate(txn.transactionDate)}
          {txn.isSettled && ' • ✓ Settled'}
          {!txn.isSettled && txn.partialPaidAmount > 0 && ` • Partial ${formatMoney(txn.partialPaidAmount)}`}
          {isEmi(txn) && ` • EMI ${txn.emiIndex + 1}/${txn.emiTotal}`}
          {isSplit(txn) && ' • Split'}
        </div>
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 4 }}>
        <div className="txn-amount" style={{ color: statusColor }}>{formatMoney(txn.amount)}</div>
        {!txn.isSettled && txn.partialPaidAmount > 0 && (
          <div className="text-xs text-muted">Due: {formatMoney(remaining)}</div>
        )}
        <div style={{ display: 'flex', gap: 4 }}>
          <button
            className="btn btn-ghost btn-sm"
            style={{ padding: '2px 6px', fontSize: '0.75rem', color: txn.isSettled ? 'var(--green)' : 'var(--text-muted)' }}
            onClick={() => onSettle(txn.id, !txn.isSettled)}
            title={txn.isSettled ? 'Mark unsettled' : 'Mark settled'}
          >
            <Check size={12} />
          </button>
          {!txn.isSettled && (
            <button
              className="btn btn-ghost btn-sm"
              style={{ padding: '2px 6px', fontSize: '0.75rem' }}
              onClick={() => onPartialPay(txn.id)}
              title="Add partial payment"
            >
              ₹
            </button>
          )}
          <button
            className="btn btn-ghost btn-sm"
            style={{ padding: '2px 6px', fontSize: '0.75rem' }}
            onClick={() => onEdit(txn)}
            title="Edit"
          >
            ✏️
          </button>
          <button
            className="btn btn-ghost btn-sm"
            style={{ padding: '2px 6px', fontSize: '0.75rem', color: 'var(--red)' }}
            onClick={() => onDelete(txn.id)}
            title="Delete"
          >
            <Trash2 size={12} />
          </button>
        </div>
      </div>
    </div>
  );
}

interface AddTxnForm {
  transactionName: string;
  accountKind: AccountKind;
  accountId: string;
  accountName: string;
  personName: string;
  amount: string;
  transactionDate: string;
  mode: 'simple' | 'split' | 'emi';
  splits: SplitEntry[];
  emiMonths: string;
  emiFirstMonth: string;
}

const defaultForm = (): AddTxnForm => ({
  transactionName: '',
  accountKind: 'credit_card',
  accountId: '',
  accountName: '',
  personName: '',
  amount: '',
  transactionDate: todayString(),
  mode: 'simple',
  splits: [
    { accountKind: 'credit_card', accountId: '', accountName: '', personName: '', amount: '' },
    { accountKind: 'credit_card', accountId: '', accountName: '', personName: '', amount: '' },
  ],
  emiMonths: '',
  emiFirstMonth: '',
});

export default function CustomerDetail() {
  const { customerId } = useParams<{ customerId: string }>();
  const navigate = useNavigate();
  const {
    customers, settings,
    addTransaction, addEmiTransactions, addSplitTransactions,
    updateTransaction, deleteTransaction, addPartialPayment,
    toggleTransactionSettled, deleteCustomer, updateCustomerDueAmount,
  } = useApp();

  const customer = customers.find(c => c.id === customerId);
  const [filter, setFilter] = useState<TxnFilter>('ALL');
  const [showAddTxn, setShowAddTxn] = useState(false);
  const [editTxn, setEditTxn] = useState<CustomerTransaction | null>(null);
  const [form, setForm] = useState<AddTxnForm>(defaultForm);
  const [partialPayId, setPartialPayId] = useState<string | null>(null);
  const [partialAmount, setPartialAmount] = useState('');
  const [showDueEditor, setShowDueEditor] = useState(false);
  const [dueAmount, setDueAmount] = useState('');
  const [saving, setSaving] = useState(false);
  const [confirmDeleteCustomer, setConfirmDeleteCustomer] = useState(false);

  const visibleTxns = useMemo(() => {
    if (!customer) return [];
    const today = new Date();
    return customer.transactions
      .filter(t => isVisibleInTransactions(t, today))
      .filter(t => filter === 'ALL' || t.accountKind === filter)
      .sort((a, b) => b.transactionDate.localeCompare(a.transactionDate));
  }, [customer, filter]);

  const accountOptions = useMemo(() =>
    getAccountOptions(form.accountKind, settings.selectedAccountIds),
    [form.accountKind, settings.selectedAccountIds]
  );

  if (!customer) {
    return (
      <div className="page-content">
        <button className="btn btn-ghost" onClick={() => navigate('/customers')}>
          <ArrowLeft size={18} /> Back
        </button>
        <div className="empty-state"><h3>Customer not found</h3></div>
      </div>
    );
  }

  const handleSaveTxn = async () => {
    if (!form.transactionName.trim()) return;
    setSaving(true);
    try {
      if (form.mode === 'split') {
        await addSplitTransactions({
          customerId: customer.id,
          customerName: customer.name,
          transactionName: form.transactionName,
          transactionDate: form.transactionDate,
          splits: form.splits,
        });
      } else if (form.mode === 'emi') {
        const total = evaluateExpression(form.amount) ?? parseFloat(form.amount);
        const months = parseInt(form.emiMonths);
        if (!isNaN(total) && !isNaN(months) && months > 0) {
          await addEmiTransactions({
            customerId: customer.id,
            transactionName: form.transactionName,
            customerName: customer.name,
            accountId: form.accountId || form.accountName,
            accountName: form.accountName,
            totalAmount: total,
            transactionDate: form.transactionDate,
            months,
            firstMonthOverride: form.emiFirstMonth ? parseFloat(form.emiFirstMonth) : undefined,
          });
        }
      } else {
        const amount = evaluateExpression(form.amount) ?? parseFloat(form.amount);
        if (!isNaN(amount)) {
          await addTransaction({
            customerId: customer.id,
            transactionName: form.transactionName,
            customerName: customer.name,
            accountId: form.accountKind === 'person'
              ? `person_${form.personName.trim().toLowerCase().replace(/\s+/g, '_')}`
              : form.accountId,
            accountName: form.accountKind === 'person' ? form.personName : form.accountName,
            accountKind: form.accountKind,
            amount: String(amount),
            transactionDate: form.transactionDate,
            personName: form.accountKind === 'person' ? form.personName : undefined,
          });
        }
      }
      setShowAddTxn(false);
      setForm(defaultForm());
    } finally {
      setSaving(false);
    }
  };

  const handleUpdateTxn = async () => {
    if (!editTxn || !form.transactionName.trim()) return;
    setSaving(true);
    try {
      const amount = evaluateExpression(form.amount) ?? parseFloat(form.amount);
      if (!isNaN(amount)) {
        await updateTransaction({
          transactionId: editTxn.id,
          transactionName: form.transactionName,
          accountId: form.accountKind === 'person'
            ? `person_${form.personName.trim().toLowerCase().replace(/\s+/g, '_')}`
            : form.accountId,
          accountName: form.accountKind === 'person' ? form.personName : form.accountName,
          accountKind: form.accountKind,
          amount: String(amount),
          transactionDate: form.transactionDate,
          personName: form.accountKind === 'person' ? form.personName : undefined,
        });
      }
      setEditTxn(null);
      setForm(defaultForm());
    } finally {
      setSaving(false);
    }
  };

  const openEdit = (txn: CustomerTransaction) => {
    setEditTxn(txn);
    const acctOption = ALL_ACCOUNTS.find(a => a.id === txn.accountId);
    setForm({
      ...defaultForm(),
      transactionName: txn.name,
      accountKind: txn.accountKind,
      accountId: txn.accountId,
      accountName: txn.accountName,
      personName: txn.personName,
      amount: String(txn.amount),
      transactionDate: txn.transactionDate,
      mode: 'simple',
    });
  };

  return (
    <div className="page-content">
      {/* Back */}
      <button className="btn btn-ghost" style={{ marginBottom: '1rem' }} onClick={() => navigate('/customers')}>
        <ArrowLeft size={18} /> Customers
      </button>

      {/* Customer header */}
      <div className="flow-card" style={{ marginBottom: '1rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: '1rem' }}>
          <div className="avatar" style={{ width: 52, height: 52, fontSize: '1.125rem' }}>
            {customer.name.slice(0, 2).toUpperCase()}
          </div>
          <div style={{ flex: 1 }}>
            <h2 style={{ fontSize: '1.375rem' }}>{customer.name}</h2>
            <p className="text-muted text-sm">{customer.transactions.length} transaction(s)</p>
          </div>
          <div style={{ display: 'flex', gap: 8 }}>
            <button
              className="btn btn-ghost btn-sm"
              onClick={() => navigate(`/customers/${customer.id}/savings`)}
              title="Savings"
            >
              <PiggyBank size={16} />
            </button>
            <button
              className="btn btn-ghost btn-sm"
              style={{ color: 'var(--red)' }}
              onClick={() => setConfirmDeleteCustomer(true)}
              title="Delete customer"
            >
              <Trash2 size={16} />
            </button>
          </div>
        </div>

        <div className="two-col" style={{ marginBottom: '0.75rem' }}>
          <div className="metric-pill">
            <span className="label">Total Used</span>
            <span className="value text-primary">{formatMoney(customer.totalAmount)}</span>
          </div>
          <div className="metric-pill">
            <span className="label">Customer Paid</span>
            <span className="value" style={{ color: 'var(--secondary)' }}>{formatMoney(customer.creditDueAmount)}</span>
          </div>
        </div>

        <div className="accent-row">
          <span className="accent-label">Balance Remaining</span>
          <span className="accent-value" style={{ color: customer.balance > 0 ? 'var(--warning)' : 'var(--primary)' }}>
            {formatMoney(customer.balance)}
          </span>
        </div>

        <p className="text-muted text-xs" style={{ marginTop: 8 }}>
          Manual paid {formatMoney(customer.manualPaidAmount)} • Settled {formatMoney(customer.settledTransactionAmount)} • Partial {formatMoney(customer.partialPaidAmount)}
        </p>

        <div style={{ display: 'flex', gap: 8, marginTop: '1rem' }}>
          <button className="btn btn-sm btn-outline" onClick={() => { setDueAmount(String(customer.creditDueAmount)); setShowDueEditor(true); }}>
            Adjust Paid
          </button>
        </div>
      </div>

      {/* Transactions */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.75rem' }}>
        <h3>Transactions</h3>
        <button className="btn btn-primary btn-sm" onClick={() => { setShowAddTxn(true); setForm(defaultForm()); }}>
          <Plus size={14} /> Add
        </button>
      </div>

      {/* Filter */}
      <div style={{ display: 'flex', gap: 8, marginBottom: '1rem', flexWrap: 'wrap' }}>
        {(['ALL', 'bank_account', 'credit_card', 'person'] as TxnFilter[]).map(f => (
          <button
            key={f}
            className={`btn btn-sm ${filter === f ? 'btn-primary' : 'btn-outline'}`}
            onClick={() => setFilter(f)}
          >
            {f === 'ALL' ? 'All' : ACCOUNT_KIND_LABELS[f as AccountKind]}
          </button>
        ))}
      </div>

      {visibleTxns.length === 0 ? (
        <div className="empty-state">
          <div className="empty-state-icon">📋</div>
          <h3>No transactions</h3>
          <p>Add a transaction to start tracking this customer's ledger.</p>
        </div>
      ) : (
        <div className="flow-card">
          {visibleTxns.map(txn => (
            <TransactionRow
              key={txn.id}
              txn={txn}
              onSettle={(id, settled) => toggleTransactionSettled(id, settled)}
              onPartialPay={(id) => { setPartialPayId(id); setPartialAmount(''); }}
              onDelete={(id) => deleteTransaction(id)}
              onEdit={openEdit}
            />
          ))}
        </div>
      )}

      {/* Add/Edit Transaction Modal */}
      {(showAddTxn || editTxn) && (
        <div className="modal-overlay" onClick={() => { setShowAddTxn(false); setEditTxn(null); }}>
          <div className="modal" style={{ maxWidth: 560 }} onClick={e => e.stopPropagation()}>
            <h3 className="modal-title">{editTxn ? 'Edit Transaction' : 'Add Transaction'}</h3>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.875rem' }}>
              {!editTxn && (
                <div style={{ display: 'flex', gap: 8 }}>
                  {(['simple', 'split', 'emi'] as const).map(m => (
                    <button
                      key={m}
                      className={`btn btn-sm ${form.mode === m ? 'btn-primary' : 'btn-outline'}`}
                      onClick={() => setForm(f => ({ ...f, mode: m }))}
                    >
                      {m === 'simple' ? 'Simple' : m === 'split' ? 'Split' : 'EMI'}
                    </button>
                  ))}
                </div>
              )}

              <div className="form-group">
                <label className="form-label">Transaction Name</label>
                <input className="form-input" value={form.transactionName} onChange={e => setForm(f => ({ ...f, transactionName: e.target.value }))} placeholder="e.g. Grocery, Rent" />
              </div>

              {form.mode !== 'split' && (
                <>
                  <div className="form-group">
                    <label className="form-label">Account Type</label>
                    <select className="form-select" value={form.accountKind} onChange={e => setForm(f => ({ ...f, accountKind: e.target.value as AccountKind, accountId: '', accountName: '' }))}>
                      <option value="credit_card">Credit Card</option>
                      <option value="bank_account">Bank Account</option>
                      <option value="person">Person</option>
                    </select>
                  </div>

                  {form.accountKind === 'person' ? (
                    <div className="form-group">
                      <label className="form-label">Person Name</label>
                      <input className="form-input" value={form.personName} onChange={e => setForm(f => ({ ...f, personName: e.target.value }))} placeholder="Enter person name" />
                    </div>
                  ) : (
                    <div className="form-group">
                      <label className="form-label">{form.accountKind === 'credit_card' ? 'Credit Card' : 'Bank Account'}</label>
                      <select
                        className="form-select"
                        value={form.accountId}
                        onChange={e => {
                          const opt = accountOptions.find(a => a.id === e.target.value);
                          setForm(f => ({ ...f, accountId: e.target.value, accountName: opt?.name ?? '' }));
                        }}
                      >
                        <option value="">Select account</option>
                        {accountOptions.map(a => <option key={a.id} value={a.id}>{a.name}</option>)}
                      </select>
                    </div>
                  )}

                  <div className="form-group">
                    <label className="form-label">Amount (supports expressions like 100+50)</label>
                    <input className="form-input" value={form.amount} onChange={e => setForm(f => ({ ...f, amount: e.target.value }))} placeholder="0.00" />
                  </div>

                  {form.mode === 'emi' && (
                    <>
                      <div className="form-group">
                        <label className="form-label">Number of EMI Months</label>
                        <input className="form-input" type="number" min="1" value={form.emiMonths} onChange={e => setForm(f => ({ ...f, emiMonths: e.target.value }))} placeholder="e.g. 12" />
                      </div>
                      <div className="form-group">
                        <label className="form-label">First Month Override (optional)</label>
                        <input className="form-input" value={form.emiFirstMonth} onChange={e => setForm(f => ({ ...f, emiFirstMonth: e.target.value }))} placeholder="Leave blank for equal EMIs" />
                      </div>
                    </>
                  )}
                </>
              )}

              {form.mode === 'split' && (
                <div>
                  <label className="form-label" style={{ marginBottom: 8, display: 'block' }}>Split Entries</label>
                  {form.splits.map((split, i) => (
                    <div key={i} style={{ background: 'var(--bg-soft)', borderRadius: 14, padding: '0.75rem', marginBottom: 8 }}>
                      <div style={{ display: 'flex', gap: 8, marginBottom: 8 }}>
                        <select
                          className="form-select"
                          style={{ flex: 1 }}
                          value={split.accountKind}
                          onChange={e => {
                            const splits = [...form.splits];
                            splits[i] = { ...splits[i], accountKind: e.target.value as AccountKind, accountId: '', accountName: '' };
                            setForm(f => ({ ...f, splits }));
                          }}
                        >
                          <option value="credit_card">Credit Card</option>
                          <option value="bank_account">Bank Account</option>
                          <option value="person">Person</option>
                        </select>
                        <input
                          className="form-input"
                          style={{ width: 100 }}
                          value={split.amount}
                          onChange={e => {
                            const splits = [...form.splits];
                            splits[i] = { ...splits[i], amount: e.target.value };
                            setForm(f => ({ ...f, splits }));
                          }}
                          placeholder="Amount"
                        />
                      </div>
                      {split.accountKind === 'person' ? (
                        <input
                          className="form-input"
                          value={split.personName}
                          onChange={e => {
                            const splits = [...form.splits];
                            splits[i] = { ...splits[i], personName: e.target.value };
                            setForm(f => ({ ...f, splits }));
                          }}
                          placeholder="Person name"
                        />
                      ) : (
                        <select
                          className="form-select"
                          value={split.accountId}
                          onChange={e => {
                            const opt = getAccountOptions(split.accountKind, settings.selectedAccountIds).find(a => a.id === e.target.value);
                            const splits = [...form.splits];
                            splits[i] = { ...splits[i], accountId: e.target.value, accountName: opt?.name ?? '' };
                            setForm(f => ({ ...f, splits }));
                          }}
                        >
                          <option value="">Select account</option>
                          {getAccountOptions(split.accountKind, settings.selectedAccountIds).map(a => (
                            <option key={a.id} value={a.id}>{a.name}</option>
                          ))}
                        </select>
                      )}
                    </div>
                  ))}
                  <button
                    className="btn btn-outline btn-sm"
                    onClick={() => setForm(f => ({ ...f, splits: [...f.splits, { accountKind: 'credit_card', accountId: '', accountName: '', personName: '', amount: '' }] }))}
                  >
                    + Add Split
                  </button>
                </div>
              )}

              <div className="form-group">
                <label className="form-label">Date</label>
                <input className="form-input" type="date" value={form.transactionDate} onChange={e => setForm(f => ({ ...f, transactionDate: e.target.value }))} />
              </div>
            </div>

            <div className="modal-actions">
              <button className="btn btn-outline" onClick={() => { setShowAddTxn(false); setEditTxn(null); }}>Cancel</button>
              <button
                className="btn btn-primary"
                onClick={editTxn ? handleUpdateTxn : handleSaveTxn}
                disabled={saving || !form.transactionName.trim()}
              >
                {saving ? 'Saving...' : editTxn ? 'Update' : 'Add Transaction'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Partial payment modal */}
      {partialPayId && (
        <div className="modal-overlay" onClick={() => setPartialPayId(null)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h3 className="modal-title">Add Partial Payment</h3>
            <div className="form-group" style={{ marginBottom: '1rem' }}>
              <label className="form-label">Amount</label>
              <input
                className="form-input"
                type="number"
                value={partialAmount}
                onChange={e => setPartialAmount(e.target.value)}
                placeholder="0.00"
                autoFocus
              />
            </div>
            <div className="modal-actions">
              <button className="btn btn-outline" onClick={() => setPartialPayId(null)}>Cancel</button>
              <button
                className="btn btn-primary"
                onClick={() => {
                  addPartialPayment(partialPayId, partialAmount);
                  setPartialPayId(null);
                }}
                disabled={!partialAmount || parseFloat(partialAmount) <= 0}
              >
                Add Payment
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Adjust paid modal */}
      {showDueEditor && (
        <div className="modal-overlay" onClick={() => setShowDueEditor(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h3 className="modal-title">Adjust Customer Paid Amount</h3>
            <p className="modal-subtitle">Set the total amount this customer has paid manually.</p>
            <div className="form-group" style={{ marginBottom: '1rem' }}>
              <label className="form-label">Paid Amount</label>
              <input
                className="form-input"
                type="number"
                value={dueAmount}
                onChange={e => setDueAmount(e.target.value)}
                placeholder="0.00"
                autoFocus
              />
            </div>
            <div className="modal-actions">
              <button className="btn btn-outline" onClick={() => setShowDueEditor(false)}>Cancel</button>
              <button
                className="btn btn-primary"
                onClick={() => {
                  updateCustomerDueAmount(customer.id, customer.name, dueAmount);
                  setShowDueEditor(false);
                }}
              >
                Save
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Confirm delete customer */}
      {confirmDeleteCustomer && (
        <div className="modal-overlay" onClick={() => setConfirmDeleteCustomer(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h3 className="modal-title">Delete Customer?</h3>
            <p className="modal-subtitle">
              <strong>{customer.name}</strong> will be moved to the recycle bin. You can restore them later.
            </p>
            <div className="modal-actions">
              <button className="btn btn-outline" onClick={() => setConfirmDeleteCustomer(false)}>Cancel</button>
              <button
                className="btn btn-danger"
                onClick={() => {
                  deleteCustomer(customer.id, customer.name);
                  navigate('/customers');
                }}
              >
                Delete
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
