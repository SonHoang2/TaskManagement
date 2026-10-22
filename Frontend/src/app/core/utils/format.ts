import { MemberRole, SprintStatus, Task, TaskPriority, TaskStatus } from '../models';

export interface Meta {
  label: string;
  icon: string;
  tone: string;
}

export const STATUSES: TaskStatus[] = ['TODO', 'IN_PROGRESS', 'DONE'];
export const PRIORITIES: TaskPriority[] = ['HIGH', 'MEDIUM', 'LOW'];

export const STATUS_META: Record<TaskStatus, Meta> = {
  TODO: { label: 'To do', icon: 'radio_button_unchecked', tone: 'todo' },
  IN_PROGRESS: { label: 'In progress', icon: 'clock_loader_40', tone: 'progress' },
  DONE: { label: 'Done', icon: 'check_circle', tone: 'done' },
};

export const PRIORITY_META: Record<TaskPriority, Meta> = {
  HIGH: { label: 'High', icon: 'keyboard_double_arrow_up', tone: 'high' },
  MEDIUM: { label: 'Medium', icon: 'drag_handle', tone: 'medium' },
  LOW: { label: 'Low', icon: 'keyboard_arrow_down', tone: 'low' },
};

export const SPRINT_META: Record<SprintStatus, Meta> = {
  PLANNED: { label: 'Planned', icon: 'event', tone: 'todo' },
  ACTIVE: { label: 'Active', icon: 'bolt', tone: 'progress' },
  COMPLETED: { label: 'Completed', icon: 'task_alt', tone: 'done' },
};

export const ROLE_META: Record<MemberRole, Meta> = {
  OWNER: { label: 'Owner', icon: 'workspace_premium', tone: 'primary' },
  ADMIN: { label: 'Admin', icon: 'shield_person', tone: 'progress' },
  MEMBER: { label: 'Member', icon: 'person', tone: 'todo' },
};

export const PRIORITY_RANK: Record<TaskPriority, number> = { HIGH: 0, MEDIUM: 1, LOW: 2 };

export const LABEL_COLORS = [
  '#6366f1',
  '#8b5cf6',
  '#ec4899',
  '#ef4444',
  '#f97316',
  '#eab308',
  '#22c55e',
  '#14b8a6',
  '#06b6d4',
  '#3b82f6',
  '#64748b',
];

const AVATAR_COLORS = [
  '#6366f1',
  '#8b5cf6',
  '#d946ef',
  '#ec4899',
  '#f43f5e',
  '#f97316',
  '#f59e0b',
  '#10b981',
  '#14b8a6',
  '#0ea5e9',
  '#3b82f6',
];

export function colorFor(seed: string | null | undefined): string {
  if (!seed) return '#94a3b8';
  let h = 0;
  for (let i = 0; i < seed.length; i++) h = (h * 31 + seed.charCodeAt(i)) | 0;
  return AVATAR_COLORS[Math.abs(h) % AVATAR_COLORS.length];
}

export function initials(name: string | null | undefined): string {
  if (!name) return '?';
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (!parts.length) return '?';
  const first = parts[0][0] ?? '';
  const last = parts.length > 1 ? parts[parts.length - 1][0] : '';
  return (first + last).toUpperCase();
}

// ---------- Dates ----------
const DAY = 86_400_000;

/** Parses ISO instants, and plain `YYYY-MM-DD` dates as local (not UTC) days. */
export function parseDate(v: string): Date {
  return /^\d{4}-\d{2}-\d{2}$/.test(v) ? new Date(`${v}T00:00:00`) : new Date(v);
}

function startOfDay(d: Date): number {
  return new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime();
}

/** ISO instant -> `YYYY-MM-DD` in local time, for `<input type="date">`. */
export function toDateInput(iso: string | null | undefined): string {
  if (!iso) return '';
  const d = new Date(iso);
  if (isNaN(d.getTime())) return '';
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

/** `YYYY-MM-DD` (local) -> ISO instant at local midnight, or null. */
export function fromDateInput(value: string | null | undefined): string | null {
  if (!value) return null;
  const d = new Date(`${value}T00:00:00`);
  return isNaN(d.getTime()) ? null : d.toISOString();
}

/** Days from today (local) until the given date; negative when in the past. */
export function daysUntil(iso: string): number {
  return Math.round((startOfDay(parseDate(iso)) - startOfDay(new Date())) / DAY);
}

export function isOverdue(t: Pick<Task, 'dueDate' | 'status'>): boolean {
  return !!t.dueDate && t.status !== 'DONE' && daysUntil(t.dueDate) < 0;
}

export function dueLabel(iso: string | null | undefined): string {
  if (!iso) return '';
  const n = daysUntil(iso);
  if (n === 0) return 'Today';
  if (n === 1) return 'Tomorrow';
  if (n === -1) return 'Yesterday';
  if (n < 0 && n > -7) return `${-n} days ago`;
  if (n > 1 && n < 7) return `In ${n} days`;
  return shortDate(iso);
}

export function shortDate(iso: string | null | undefined): string {
  if (!iso) return '';
  const d = parseDate(iso);
  const sameYear = d.getFullYear() === new Date().getFullYear();
  return d.toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    ...(sameYear ? {} : { year: 'numeric' }),
  });
}

export function relativeTime(iso: string | null | undefined): string {
  if (!iso) return '';
  const diff = Date.now() - new Date(iso).getTime();
  if (diff < 45_000) return 'just now';
  const min = Math.round(diff / 60_000);
  if (min < 60) return `${min}m ago`;
  const h = Math.round(min / 60);
  if (h < 24) return `${h}h ago`;
  const d = Math.round(h / 24);
  if (d === 1) return 'yesterday';
  if (d < 7) return `${d}d ago`;
  return shortDate(iso);
}

export function greeting(): string {
  const h = new Date().getHours();
  if (h < 12) return 'Good morning';
  if (h < 18) return 'Good afternoon';
  return 'Good evening';
}

/** Sort: open before done, then priority, then nearest due date, then newest. */
export function compareTasks(a: Task, b: Task): number {
  const done = Number(a.status === 'DONE') - Number(b.status === 'DONE');
  if (done) return done;
  const pa = a.priority ? PRIORITY_RANK[a.priority] : 3;
  const pb = b.priority ? PRIORITY_RANK[b.priority] : 3;
  if (pa !== pb) return pa - pb;
  const da = a.dueDate ? new Date(a.dueDate).getTime() : Infinity;
  const db = b.dueDate ? new Date(b.dueDate).getTime() : Infinity;
  if (da !== db) return da - db;
  return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
}

export function taskKey(t: Pick<Task, 'id'>): string {
  return t.id.slice(0, 6).toUpperCase();
}
