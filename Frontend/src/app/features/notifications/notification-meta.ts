import { AppNotification, NotificationType } from '../../core/models';

interface NotificationMeta {
  label: string;
  icon: string;
  tone: 'primary' | 'progress' | 'done' | 'warning';
  link: string;
}

const META: Record<NotificationType, NotificationMeta> = {
  TASK_ASSIGNED: {
    label: 'Task assigned to you',
    icon: 'assignment_ind',
    tone: 'primary',
    link: '/my-tasks',
  },
  TASK_COMMENT: { label: 'New comment', icon: 'chat', tone: 'progress', link: '/my-tasks' },
  COMMENT: { label: 'New comment', icon: 'chat', tone: 'progress', link: '/my-tasks' },
  TASK_UPDATED: { label: 'Task updated', icon: 'edit_note', tone: 'progress', link: '/my-tasks' },
  PROJECT_INVITATION: {
    label: 'Project invitation',
    icon: 'group_add',
    tone: 'done',
    link: '/invitations',
  },
  SYSTEM: { label: 'System message', icon: 'campaign', tone: 'warning', link: '/notifications' },
};

export function notificationMeta(n: Pick<AppNotification, 'type'>): NotificationMeta {
  return META[n.type] ?? META.SYSTEM;
}
