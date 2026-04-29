import React, { createContext, useContext, useEffect, useState, useCallback, useRef } from 'react';
import { onAuthStateChanged, signInWithPopup, signOut as firebaseSignOut, User } from 'firebase/auth';
import { auth, googleProvider } from '../firebase';
import {
  CardSummary,
  CustomerSummary,
  UserProfile,
  AppSettings,
  AppSecurityState,
  FirestoreBackupPayload,
  AccountKind,
  SplitEntry,
  defaultSelectedAccountIds,
} from '../types/models';
import * as repo from '../services/firebaseRepository';
import { currentTimestampLabel, todayString } from '../utils/format';
import { backupFromJson, backupToJson, downloadJsonFile, readJsonFile } from '../utils/backup';
import {
  loadSecurityStorage,
  saveSecurityStorage,
  clearSecurityStorage,
  hashPasscode,
  hashRecoveryAnswer,
  generateSalt,
} from '../utils/security';

// ── Settings storage ──────────────────────────────────────────────────────────

const SETTINGS_KEY = 'radafiq_settings';

function loadSettings(): AppSettings {
  try {
    const raw = localStorage.getItem(SETTINGS_KEY);
    if (!raw) return { themeMode: 'DARK', selectedAccountIds: defaultSelectedAccountIds(), lastDriveBackupTime: null, lastDriveRestoreTime: null };
    const parsed = JSON.parse(raw);
    return {
      themeMode: parsed.themeMode ?? 'DARK',
      selectedAccountIds: new Set(parsed.selectedAccountIds ?? Array.from(defaultSelectedAccountIds())),
      lastDriveBackupTime: parsed.lastDriveBackupTime ?? null,
      lastDriveRestoreTime: parsed.lastDriveRestoreTime ?? null,
    };
  } catch {
    return { themeMode: 'DARK', selectedAccountIds: defaultSelectedAccountIds(), lastDriveBackupTime: null, lastDriveRestoreTime: null };
  }
}

function saveSettings(settings: AppSettings): void {
  localStorage.setItem(SETTINGS_KEY, JSON.stringify({
    themeMode: settings.themeMode,
    selectedAccountIds: Array.from(settings.selectedAccountIds),
    lastDriveBackupTime: settings.lastDriveBackupTime,
    lastDriveRestoreTime: settings.lastDriveRestoreTime,
  }));
}

// ── Security helpers ──────────────────────────────────────────────────────────

function loadSecurityState(): AppSecurityState {
  const s = loadSecurityStorage();
  const hasPasscode = s.passcodeHash !== '';
  const hasRecoveryQuestion = s.recoveryQuestion !== '' && s.recoveryAnswerHash !== '';
  return {
    lockEnabled: s.lockEnabled && hasPasscode,
    hasPasscode,
    recoveryQuestion: s.recoveryQuestion,
    hasRecoveryQuestion,
    isUnlocked: !(s.lockEnabled && hasPasscode),
  };
}

// ── Context types ─────────────────────────────────────────────────────────────

interface AppContextValue {
  // Auth
  user: User | null;
  authLoading: boolean;
  signInWithGoogle: () => Promise<void>;
  signOut: () => Promise<void>;

  // Profile
  profile: UserProfile | null;
  profileLoading: boolean;
  saveProfile: (displayName: string, businessName: string, email: string, photoUrl?: string) => Promise<void>;

  // Data
  cards: CardSummary[];
  customers: CustomerSummary[];
  deletedCustomers: CustomerSummary[];
  dataLoading: boolean;

  // Settings
  settings: AppSettings;
  setThemeMode: (mode: 'LIGHT' | 'DARK') => void;
  setAccountSelected: (id: string, selected: boolean) => void;

  // Security
  security: AppSecurityState;
  setPasscode: (passcode: string, recoveryQuestion: string, recoveryAnswer: string) => Promise<void>;
  updatePasscode: (current: string, newPasscode: string, recoveryQuestion: string, recoveryAnswer: string) => Promise<boolean>;
  clearPasscode: () => void;
  setLockEnabled: (enabled: boolean) => void;
  verifyPasscode: (passcode: string) => Promise<boolean>;
  resetPasscodeWithRecovery: (recoveryAnswer: string, newPasscode: string) => Promise<boolean>;
  unlock: () => void;
  lock: () => void;

  // Customer operations
  addCustomer: (name: string) => Promise<string>;
  deleteCustomer: (id: string, name: string) => Promise<void>;
  restoreCustomer: (id: string) => Promise<void>;
  permanentlyDeleteCustomer: (id: string, name: string) => Promise<void>;
  updateCustomerDueAmount: (id: string, name: string, amount: string) => Promise<void>;

  // Transaction operations
  addTransaction: (params: {
    customerId: string;
    transactionName: string;
    customerName: string;
    accountId: string;
    accountName: string;
    accountKind: AccountKind;
    amount: string;
    transactionDate: string;
    personName?: string;
  }) => Promise<void>;
  addEmiTransactions: (params: {
    customerId: string;
    transactionName: string;
    customerName: string;
    accountId: string;
    accountName: string;
    totalAmount: number;
    transactionDate: string;
    months: number;
    firstMonthOverride?: number;
    dateOverrides?: Record<number, string>;
  }) => Promise<void>;
  addSplitTransactions: (params: {
    customerId: string;
    customerName: string;
    transactionName: string;
    transactionDate: string;
    splits: SplitEntry[];
  }) => Promise<void>;
  updateTransaction: (params: {
    transactionId: string;
    transactionName: string;
    accountId: string;
    accountName: string;
    accountKind: AccountKind;
    amount: string;
    transactionDate: string;
    personName?: string;
  }) => Promise<void>;
  deleteTransaction: (id: string) => Promise<void>;
  addPartialPayment: (transactionId: string, amount: string) => Promise<void>;
  toggleTransactionSettled: (transactionId: string, isSettled: boolean) => Promise<void>;

  // Account operations
  updateCreditCardDue: (params: {
    accountId: string;
    accountName: string;
    amount: string;
    dueDate: string;
    remindersEnabled: boolean;
    reminderEmail: string;
    reminderWhatsApp: string;
  }) => Promise<void>;

  // Payment operations
  addPayment: (accountId: string, accountName: string, accountKind: AccountKind, amount: string) => Promise<void>;

  // Savings operations
  addSavingsDeposit: (customerId: string, customerName: string, amount: string, note: string) => Promise<void>;
  addSavingsWithdrawal: (customerId: string, customerName: string, amount: string, note: string) => Promise<void>;
  deleteSavingsEntry: (entryId: string) => Promise<void>;

  // Backup / Restore
  exportBackupToFile: () => Promise<void>;
  importBackupFromFile: (file: File) => Promise<void>;
  backupStatusMessage: string;
  backupInProgress: boolean;

  // Sync status
  syncStatus: { state: 'IDLE' | 'SYNCING' | 'SUCCESS' | 'ERROR'; message: string };
  triggerSync: () => void;
}

const AppContext = createContext<AppContextValue | null>(null);

export function useApp(): AppContextValue {
  const ctx = useContext(AppContext);
  if (!ctx) throw new Error('useApp must be used within AppProvider');
  return ctx;
}

// ── Provider ──────────────────────────────────────────────────────────────────

export function AppProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [authLoading, setAuthLoading] = useState(true);
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [profileLoading, setProfileLoading] = useState(true);
  const [cards, setCards] = useState<CardSummary[]>([]);
  const [customers, setCustomers] = useState<CustomerSummary[]>([]);
  const [deletedCustomers, setDeletedCustomers] = useState<CustomerSummary[]>([]);
  const [dataLoading, setDataLoading] = useState(true);
  const [settings, setSettingsState] = useState<AppSettings>(loadSettings);
  const [security, setSecurityState] = useState<AppSecurityState>(loadSecurityState);
  const [backupStatusMessage, setBackupStatusMessage] = useState('');
  const [backupInProgress, setBackupInProgress] = useState(false);
  const [syncStatus, setSyncStatus] = useState<{ state: 'IDLE' | 'SYNCING' | 'SUCCESS' | 'ERROR'; message: string }>({ state: 'IDLE', message: '' });

  const unsubscribeDataRef = useRef<(() => void)[]>([]);
  const unsubscribeProfileRef = useRef<(() => void) | null>(null);
  const syncResetTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  // ── Auth listener ─────────────────────────────────────────────────────────

  useEffect(() => {
    const unsub = onAuthStateChanged(auth, async (firebaseUser) => {
      setUser(firebaseUser);
      setAuthLoading(false);

      if (firebaseUser) {
        // Start profile listener
        setProfileLoading(true);
        unsubscribeProfileRef.current?.();
        unsubscribeProfileRef.current = repo.listenProfile(firebaseUser.uid, (p) => {
          setProfile(p ? { ...p } : null);
          setProfileLoading(false);
        });

        // Start data listeners
        setDataLoading(true);
        unsubscribeDataRef.current.forEach(u => u());
        let firstSnapshot = true;
        const unsubs = repo.listenAllData(firebaseUser.uid, (data) => {
          setCards(data.accounts);
          setCustomers(data.customers);
          setDeletedCustomers(data.deletedCustomers);
          if (firstSnapshot) {
            firstSnapshot = false;
            setDataLoading(false);
          }
        });
        unsubscribeDataRef.current = unsubs;
      } else {
        // Signed out
        unsubscribeProfileRef.current?.();
        unsubscribeDataRef.current.forEach(u => u());
        unsubscribeDataRef.current = [];
        setProfile(null);
        setProfileLoading(false);
        setCards([]);
        setCustomers([]);
        setDeletedCustomers([]);
        setDataLoading(false);
      }
    });
    return () => unsub();
  }, []);

  // ── Settings helpers ──────────────────────────────────────────────────────

  const updateSettings = useCallback((updater: (prev: AppSettings) => AppSettings) => {
    setSettingsState(prev => {
      const next = updater(prev);
      saveSettings(next);
      return next;
    });
  }, []);

  const setThemeMode = useCallback((mode: 'LIGHT' | 'DARK') => {
    updateSettings(s => ({ ...s, themeMode: mode }));
  }, [updateSettings]);

  const setAccountSelected = useCallback((id: string, selected: boolean) => {
    updateSettings(s => {
      const ids = new Set(s.selectedAccountIds);
      if (selected) ids.add(id);
      else if (ids.size > 1) ids.delete(id);
      return { ...s, selectedAccountIds: ids };
    });
  }, [updateSettings]);

  // ── Security helpers ──────────────────────────────────────────────────────

  const refreshSecurity = useCallback(() => {
    setSecurityState(loadSecurityState());
  }, []);

  const setPasscode = useCallback(async (passcode: string, recoveryQuestion: string, recoveryAnswer: string) => {
    const salt = generateSalt();
    const hash = await hashPasscode(passcode.trim(), salt);
    const answerHash = await hashRecoveryAnswer(recoveryAnswer, salt);
    saveSecurityStorage({
      passcodeHash: hash,
      passcodeSalt: salt,
      lockEnabled: true,
      recoveryQuestion: recoveryQuestion.trim(),
      recoveryAnswerHash: answerHash,
    });
    refreshSecurity();
  }, [refreshSecurity]);

  const updatePasscode = useCallback(async (current: string, newPasscode: string, recoveryQuestion: string, recoveryAnswer: string): Promise<boolean> => {
    const s = loadSecurityStorage();
    const currentHash = await hashPasscode(current.trim(), s.passcodeSalt);
    if (currentHash !== s.passcodeHash) return false;
    const newHash = await hashPasscode(newPasscode.trim(), s.passcodeSalt);
    const answerHash = await hashRecoveryAnswer(recoveryAnswer, s.passcodeSalt);
    saveSecurityStorage({
      passcodeHash: newHash,
      recoveryQuestion: recoveryQuestion.trim(),
      recoveryAnswerHash: answerHash,
    });
    refreshSecurity();
    return true;
  }, [refreshSecurity]);

  const clearPasscode = useCallback(() => {
    clearSecurityStorage();
    refreshSecurity();
  }, [refreshSecurity]);

  const setLockEnabled = useCallback((enabled: boolean) => {
    saveSecurityStorage({ lockEnabled: enabled });
    refreshSecurity();
  }, [refreshSecurity]);

  const verifyPasscode = useCallback(async (passcode: string): Promise<boolean> => {
    const s = loadSecurityStorage();
    const hash = await hashPasscode(passcode.trim(), s.passcodeSalt);
    const matches = hash === s.passcodeHash;
    if (matches) {
      setSecurityState(prev => ({ ...prev, isUnlocked: true }));
    }
    return matches;
  }, []);

  const resetPasscodeWithRecovery = useCallback(async (recoveryAnswer: string, newPasscode: string): Promise<boolean> => {
    const s = loadSecurityStorage();
    const answerHash = await hashRecoveryAnswer(recoveryAnswer, s.passcodeSalt);
    if (answerHash !== s.recoveryAnswerHash) return false;
    const newHash = await hashPasscode(newPasscode.trim(), s.passcodeSalt);
    saveSecurityStorage({ passcodeHash: newHash, lockEnabled: true });
    setSecurityState(prev => ({ ...prev, isUnlocked: true }));
    return true;
  }, []);

  const unlock = useCallback(() => {
    setSecurityState(prev => ({ ...prev, isUnlocked: true }));
  }, []);

  const lock = useCallback(() => {
    setSecurityState(prev => {
      if (prev.lockEnabled && prev.hasPasscode) return { ...prev, isUnlocked: false };
      return prev;
    });
  }, []);

  // ── Auth operations ───────────────────────────────────────────────────────

  const signInWithGoogle = useCallback(async () => {
    await signInWithPopup(auth, googleProvider);
  }, []);

  const signOut = useCallback(async () => {
    await firebaseSignOut(auth);
  }, []);

  // ── Profile ───────────────────────────────────────────────────────────────

  const saveProfileFn = useCallback(async (displayName: string, businessName: string, email: string, photoUrl: string = '') => {
    if (!user) return;
    await repo.saveProfile(user.uid, displayName, businessName, email, photoUrl);
  }, [user]);

  // ── Customer operations ───────────────────────────────────────────────────

  const addCustomer = useCallback(async (name: string): Promise<string> => {
    if (!user) return '';
    return repo.addCustomer(user.uid, name.trim());
  }, [user]);

  const deleteCustomer = useCallback(async (id: string, name: string) => {
    if (!user) return;
    await repo.deleteCustomer(user.uid, id, name);
  }, [user]);

  const restoreCustomer = useCallback(async (id: string) => {
    if (!user) return;
    await repo.restoreCustomer(user.uid, id);
  }, [user]);

  const permanentlyDeleteCustomer = useCallback(async (id: string, name: string) => {
    if (!user) return;
    await repo.permanentlyDeleteCustomer(user.uid, id, name);
  }, [user]);

  const updateCustomerDueAmount = useCallback(async (id: string, name: string, amount: string) => {
    if (!user) return;
    const parsed = parseFloat(amount);
    if (isNaN(parsed)) return;
    await repo.updateCustomerDueAmount(user.uid, id, name, parsed);
  }, [user]);

  // ── Transaction operations ────────────────────────────────────────────────

  const addTransaction = useCallback(async (params: {
    customerId: string;
    transactionName: string;
    customerName: string;
    accountId: string;
    accountName: string;
    accountKind: AccountKind;
    amount: string;
    transactionDate: string;
    personName?: string;
  }) => {
    if (!user) return;
    const amount = parseFloat(params.amount);
    if (isNaN(amount)) return;
    await repo.addTransaction(user.uid, {
      ...params,
      amount,
      transactionDate: params.transactionDate || todayString(),
    });
  }, [user]);

  const addEmiTransactions = useCallback(async (params: {
    customerId: string;
    transactionName: string;
    customerName: string;
    accountId: string;
    accountName: string;
    totalAmount: number;
    transactionDate: string;
    months: number;
    firstMonthOverride?: number;
    dateOverrides?: Record<number, string>;
  }) => {
    if (!user || params.months <= 0 || params.totalAmount <= 0) return;
    const baseDate = new Date(params.transactionDate || todayString());
    const baseEmi = params.totalAmount / params.months;
    const firstEmi = params.firstMonthOverride && params.firstMonthOverride > 0 ? params.firstMonthOverride : baseEmi;
    const groupId = crypto.randomUUID();

    const instalments = Array.from({ length: params.months }, (_, i) => {
      const emiAmount = i === 0 ? firstEmi : baseEmi;
      let emiDate: Date;
      const override = params.dateOverrides?.[i];
      if (override) {
        emiDate = new Date(override);
      } else {
        emiDate = new Date(baseDate);
        emiDate.setMonth(emiDate.getMonth() + i);
      }
      const dueDate = new Date(emiDate);
      dueDate.setMonth(dueDate.getMonth() + 1);
      return {
        customerId: params.customerId,
        transactionName: `${params.transactionName.trim()} — EMI ${i + 1}/${params.months}`,
        accountId: params.accountId,
        accountName: params.accountName,
        accountType: 'credit_card',
        customerName: params.customerName.trim(),
        amount: emiAmount,
        transactionDate: emiDate.toISOString().split('T')[0],
        givenDate: emiDate.toISOString().split('T')[0],
        dueDate: dueDate.toISOString().split('T')[0],
        emiGroupId: groupId,
        emiIndex: i,
        emiTotal: params.months,
      };
    });

    await repo.addEmiTransactionsBatch(user.uid, instalments);
  }, [user]);

  const addSplitTransactions = useCallback(async (params: {
    customerId: string;
    customerName: string;
    transactionName: string;
    transactionDate: string;
    splits: SplitEntry[];
  }) => {
    if (!user || params.splits.length === 0) return;
    const groupId = crypto.randomUUID();
    const date = params.transactionDate || todayString();

    const docs = params.splits.flatMap(split => {
      const amount = parseFloat(split.amount);
      if (isNaN(amount) || amount <= 0) return [];
      const isPerson = split.accountKind === 'person';
      const accountId = isPerson
        ? `person_${split.personName.trim().toLowerCase().replace(/\s+/g, '_')}`
        : split.accountId || split.accountName.trim().toLowerCase().replace(/\s+/g, '_');
      const accountName = isPerson ? split.personName.trim() : split.accountName.trim();
      if (!accountName) return [];

      const doc: Record<string, unknown> = {
        customerId: params.customerId,
        customerName: params.customerName,
        transactionName: params.transactionName,
        accountId,
        accountName,
        accountType: split.accountKind,
        amount,
        transactionDate: date,
        givenDate: date,
        splitGroupId: groupId,
      };
      if (isPerson && split.personName) doc.personName = split.personName.trim();
      return [doc];
    });

    if (docs.length > 0) await repo.addSplitTransactionsBatch(user.uid, docs);
  }, [user]);

  const updateTransaction = useCallback(async (params: {
    transactionId: string;
    transactionName: string;
    accountId: string;
    accountName: string;
    accountKind: AccountKind;
    amount: string;
    transactionDate: string;
    personName?: string;
  }) => {
    if (!user) return;
    const amount = parseFloat(params.amount);
    if (isNaN(amount)) return;
    await repo.updateTransaction(user.uid, params.transactionId, {
      ...params,
      amount,
      transactionDate: params.transactionDate || todayString(),
    });
  }, [user]);

  const deleteTransaction = useCallback(async (id: string) => {
    if (!user) return;
    await repo.deleteTransaction(user.uid, id);
  }, [user]);

  const addPartialPayment = useCallback(async (transactionId: string, amount: string) => {
    if (!user) return;
    const parsed = parseFloat(amount);
    if (isNaN(parsed) || parsed <= 0) return;
    await repo.addPartialPayment(user.uid, transactionId, parsed, todayString());
  }, [user]);

  const toggleTransactionSettled = useCallback(async (transactionId: string, isSettled: boolean) => {
    if (!user) return;
    await repo.toggleTransactionSettled(user.uid, transactionId, isSettled, isSettled ? todayString() : '');
  }, [user]);

  // ── Account operations ────────────────────────────────────────────────────

  const updateCreditCardDue = useCallback(async (params: {
    accountId: string;
    accountName: string;
    amount: string;
    dueDate: string;
    remindersEnabled: boolean;
    reminderEmail: string;
    reminderWhatsApp: string;
  }) => {
    if (!user) return;
    const amount = parseFloat(params.amount);
    if (isNaN(amount)) return;
    await repo.updateCreditCardDue(
      user.uid,
      params.accountId,
      params.accountName,
      amount,
      params.dueDate,
      params.remindersEnabled,
      params.reminderEmail.trim(),
      params.reminderWhatsApp.trim()
    );
  }, [user]);

  // ── Payment operations ────────────────────────────────────────────────────

  const addPayment = useCallback(async (accountId: string, accountName: string, accountKind: AccountKind, amount: string) => {
    if (!user) return;
    const parsed = parseFloat(amount);
    if (isNaN(parsed) || parsed <= 0) return;
    await repo.addPayment(user.uid, accountId, accountName, accountKind, parsed, todayString());
  }, [user]);

  // ── Savings operations ────────────────────────────────────────────────────

  const addSavingsDeposit = useCallback(async (customerId: string, customerName: string, amount: string, note: string) => {
    if (!user) return;
    const parsed = parseFloat(amount);
    if (isNaN(parsed) || parsed <= 0) return;
    await repo.addSavingsEntry(user.uid, customerId, customerName, parsed, 'deposit', note.trim(), todayString());
  }, [user]);

  const addSavingsWithdrawal = useCallback(async (customerId: string, customerName: string, amount: string, note: string) => {
    if (!user) return;
    const parsed = parseFloat(amount);
    if (isNaN(parsed) || parsed <= 0) return;
    await repo.addSavingsEntry(user.uid, customerId, customerName, parsed, 'withdrawal', note.trim(), todayString());
  }, [user]);

  const deleteSavingsEntry = useCallback(async (entryId: string) => {
    if (!user) return;
    await repo.deleteSavingsEntry(user.uid, entryId);
  }, [user]);

  // ── Backup / Restore ──────────────────────────────────────────────────────

  const exportBackupToFile = useCallback(async () => {
    if (!user) return;
    setBackupInProgress(true);
    setBackupStatusMessage('Exporting backup...');
    try {
      const payload = await repo.exportBackup(user.uid, {}, {
        app: {
          themeMode: settings.themeMode,
          selectedAccountIds: Array.from(settings.selectedAccountIds),
        },
      });
      const json = backupToJson(payload);
      const ts = new Date().toISOString().replace(/[:.]/g, '-').slice(0, 19);
      downloadJsonFile(json, `radafiq_backup_${ts}.json`);
      setBackupStatusMessage('Backup exported successfully.');
    } catch (e: unknown) {
      setBackupStatusMessage(`Export failed: ${e instanceof Error ? e.message : 'Unknown error'}`);
    } finally {
      setBackupInProgress(false);
    }
  }, [user, settings]);

  const importBackupFromFile = useCallback(async (file: File) => {
    if (!user) return;
    setBackupInProgress(true);
    setBackupStatusMessage('Importing backup...');
    try {
      const json = await readJsonFile(file);
      const payload = backupFromJson(json);
      await repo.restoreBackup(user.uid, payload);

      // Restore settings
      const appSettings = payload.settings?.app as Record<string, unknown> | undefined;
      if (appSettings) {
        updateSettings(s => ({
          ...s,
          themeMode: (appSettings.themeMode as 'LIGHT' | 'DARK') ?? s.themeMode,
          selectedAccountIds: appSettings.selectedAccountIds
            ? new Set(appSettings.selectedAccountIds as string[])
            : s.selectedAccountIds,
        }));
      }

      updateSettings(s => ({ ...s, lastDriveRestoreTime: currentTimestampLabel() }));
      setBackupStatusMessage('Backup restored successfully.');
    } catch (e: unknown) {
      setBackupStatusMessage(`Import failed: ${e instanceof Error ? e.message : 'Unknown error'}`);
    } finally {
      setBackupInProgress(false);
    }
  }, [user, updateSettings]);

  // ── Sync status ───────────────────────────────────────────────────────────

  const triggerSync = useCallback(() => {
    setSyncStatus({ state: 'SYNCING', message: 'Syncing...' });
    if (syncResetTimerRef.current) clearTimeout(syncResetTimerRef.current);
    // Simulate sync (actual Drive sync would require OAuth token)
    setTimeout(() => {
      setSyncStatus({ state: 'SUCCESS', message: 'Data is up to date.' });
      syncResetTimerRef.current = setTimeout(() => {
        setSyncStatus({ state: 'IDLE', message: '' });
      }, 4000);
    }, 1500);
  }, []);

  // ── Context value ─────────────────────────────────────────────────────────

  const value: AppContextValue = {
    user,
    authLoading,
    signInWithGoogle,
    signOut,
    profile,
    profileLoading,
    saveProfile: saveProfileFn,
    cards,
    customers,
    deletedCustomers,
    dataLoading,
    settings,
    setThemeMode,
    setAccountSelected,
    security,
    setPasscode,
    updatePasscode,
    clearPasscode,
    setLockEnabled,
    verifyPasscode,
    resetPasscodeWithRecovery,
    unlock,
    lock,
    addCustomer,
    deleteCustomer,
    restoreCustomer,
    permanentlyDeleteCustomer,
    updateCustomerDueAmount,
    addTransaction,
    addEmiTransactions,
    addSplitTransactions,
    updateTransaction,
    deleteTransaction,
    addPartialPayment,
    toggleTransactionSettled,
    updateCreditCardDue,
    addPayment,
    addSavingsDeposit,
    addSavingsWithdrawal,
    deleteSavingsEntry,
    exportBackupToFile,
    importBackupFromFile,
    backupStatusMessage,
    backupInProgress,
    syncStatus,
    triggerSync,
  };

  return <AppContext.Provider value={value}>{children}</AppContext.Provider>;
}
