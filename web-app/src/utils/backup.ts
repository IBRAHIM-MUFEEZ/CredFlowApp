import { FirestoreBackupPayload, BackupRecord } from '../types/models';

export function backupToJson(payload: FirestoreBackupPayload): string {
  return JSON.stringify(payload, null, 2);
}

export function backupFromJson(json: string): FirestoreBackupPayload {
  if (json.length > 50 * 1024 * 1024) {
    throw new Error('Backup file is too large to restore safely.');
  }
  const data = JSON.parse(json);
  return {
    version: data.version ?? 1,
    exportedAt: data.exportedAt ?? '',
    profile: data.profile ?? {},
    settings: data.settings ?? {},
    customers: (data.customers ?? []) as BackupRecord[],
    accounts: (data.accounts ?? []) as BackupRecord[],
    transactions: (data.transactions ?? []) as BackupRecord[],
    payments: (data.payments ?? []) as BackupRecord[],
    savings: (data.savings ?? []) as BackupRecord[],
  };
}

export function downloadJsonFile(json: string, filename: string): void {
  const blob = new Blob([json], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}

export function readJsonFile(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = e => resolve(e.target?.result as string);
    reader.onerror = () => reject(new Error('Failed to read file'));
    reader.readAsText(file);
  });
}
