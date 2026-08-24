import type { AttentionNotificationStatus } from '../hooks/useAttentionNotifications'
import type {
  JobAttentionItem,
  JobAttentionSettings,
  UpcomingJobActivity,
} from '../types/jobActivity'
import { formatDate } from '../utils/formatDate'
import { getJobActivityTypeConfig } from '../utils/jobActivity'
import { getJobStatusConfig } from '../utils/jobStatus'

interface DailyActionCenterProps {
  attentionItems: JobAttentionItem[]
  overdueActivities: UpcomingJobActivity[]
  upcomingActivities: UpcomingJobActivity[]
  settings: JobAttentionSettings
  notificationStatus: AttentionNotificationStatus
  isNotificationRequesting: boolean
  notificationError: string | null
  onToggleNotifications: () => void
  onConfigure: () => void
  onViewCalendar: () => void
  onFollowUp: (item: JobAttentionItem) => void
  onSnooze: (item: JobAttentionItem) => void
  onComplete: (item: UpcomingJobActivity) => void
  onReschedule: (item: UpcomingJobActivity) => void
  onViewJob: (jobId: number) => void
}

type DailyAction =
  | { kind: 'attention'; item: JobAttentionItem; priority: number; sortValue: number }
  | { kind: 'scheduled'; item: UpcomingJobActivity; priority: number; sortValue: number }

const dayFormatter = new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric' })
const timeFormatter = new Intl.DateTimeFormat('en-US', { hour: 'numeric', minute: '2-digit' })

export function DailyActionCenter({
  attentionItems,
  overdueActivities,
  upcomingActivities,
  settings,
  notificationStatus,
  isNotificationRequesting,
  notificationError,
  onToggleNotifications,
  onConfigure,
  onViewCalendar,
  onFollowUp,
  onSnooze,
  onComplete,
  onReschedule,
  onViewJob,
}: DailyActionCenterProps) {
  const now = new Date()
  const actions = buildDailyActions(attentionItems, overdueActivities, upcomingActivities, now)
  const todayCount = upcomingActivities.filter(
    (activity) => isSameLocalDay(new Date(activity.occurredAt), now),
  ).length
  const futureCount = upcomingActivities.length - todayCount
  const rulesSummary = [
    'Applied ' + settings.appliedDays + 'd',
    'Assessment ' + settings.onlineAssessmentDays + 'd',
    'Interview ' + settings.interviewDays + 'd',
  ].join(' · ')

  return (
    <section className="action-center" aria-labelledby="action-center-heading">
      <div className="action-center__heading">
        <div className="action-center__title">
          <span aria-hidden="true">✓</span>
          <div>
            <p>Today first · {rulesSummary}</p>
            <h2 id="action-center-heading">Daily action center</h2>
          </div>
        </div>
        <div className="action-center__controls">
          <span>{actions.length} {actions.length === 1 ? 'action' : 'actions'}</span>
          <button
            className={'action-center__notification action-center__notification--' + notificationStatus}
            type="button"
            aria-pressed={notificationStatus === 'enabled'}
            title={getNotificationTitle(notificationStatus)}
            disabled={isNotificationRequesting || notificationStatus === 'unsupported'}
            onClick={onToggleNotifications}
          >
            <span aria-hidden="true">◉</span>
            {getNotificationLabel(notificationStatus, isNotificationRequesting)}
          </button>
          <button type="button" onClick={onViewCalendar}>Calendar</button>
          <button type="button" onClick={onConfigure}>Reminder rules</button>
        </div>
      </div>

      <div className="action-center__summary" aria-label="Action summary">
        <span className={overdueActivities.length > 0 ? 'action-center__summary-overdue' : undefined}>
          <strong>{overdueActivities.length}</strong> overdue
        </span>
        <span><strong>{todayCount}</strong> today</span>
        <span><strong>{attentionItems.length}</strong> follow-ups due</span>
        <span><strong>{futureCount}</strong> later</span>
      </div>

      {notificationError && (
        <p className="action-center__notification-error" role="alert">{notificationError}</p>
      )}

      {actions.length === 0 ? (
        <div className="action-center__empty">
          <span aria-hidden="true">✓</span>
          <div>
            <strong>You are clear for now.</strong>
            <p>No applications need a follow-up, no activities are overdue, and nothing is scheduled in the next 14 days.</p>
          </div>
        </div>
      ) : (
        <div className="action-center__list">
          {actions.map((action) => (
            action.kind === 'attention'
              ? (
                  <AttentionAction
                    key={'attention-' + action.item.jobId}
                    item={action.item}
                    onFollowUp={onFollowUp}
                    onSnooze={onSnooze}
                  />
                )
              : (
                  <ScheduledAction
                    key={'scheduled-' + action.item.id}
                    item={action.item}
                    now={now}
                    onComplete={onComplete}
                    onReschedule={onReschedule}
                    onViewJob={onViewJob}
                  />
                )
          ))}
        </div>
      )}
    </section>
  )
}

function AttentionAction({
  item,
  onFollowUp,
  onSnooze,
}: {
  item: JobAttentionItem
  onFollowUp: (item: JobAttentionItem) => void
  onSnooze: (item: JobAttentionItem) => void
}) {
  const status = getJobStatusConfig(item.status)
  const daysPastRule = Math.max(0, item.daysWithoutActivity - item.thresholdDays)

  return (
    <article className="action-center__item action-center__item--attention">
      <span className="action-center__when action-center__when--attention">
        <strong>{daysPastRule === 0 ? 'Due' : daysPastRule + 'd'}</strong>
        <span>{daysPastRule === 0 ? 'now' : 'overdue'}</span>
      </span>
      <div className="action-center__content">
        <div className="action-center__badges">
          <span className="action-center__kind">Follow-up due</span>
          <span className={'status-badge status-badge--' + status.tone}>{status.label}</span>
        </div>
        <strong>{item.jobTitle}</strong>
        <span>{item.company} · Last activity {formatDate(item.lastActivityAt)}</span>
      </div>
      <div className="action-center__actions">
        <button type="button" onClick={() => onFollowUp(item)}>
          Follow up <span aria-hidden="true">→</span>
        </button>
        <button type="button" onClick={() => onSnooze(item)}>Snooze</button>
      </div>
    </article>
  )
}

function ScheduledAction({
  item,
  now,
  onComplete,
  onReschedule,
  onViewJob,
}: {
  item: UpcomingJobActivity
  now: Date
  onComplete: (item: UpcomingJobActivity) => void
  onReschedule: (item: UpcomingJobActivity) => void
  onViewJob: (jobId: number) => void
}) {
  const occurredAt = new Date(item.occurredAt)
  const config = getJobActivityTypeConfig(item.type)
  const isOverdue = occurredAt < now
  const dayLabel = isOverdue ? getOverdueDayLabel(occurredAt, now) : getRelativeDayLabel(occurredAt, now)

  return (
    <article className={
      'action-center__item action-center__item--' + item.type.toLowerCase()
      + (isOverdue ? ' action-center__item--overdue' : '')
    }>
      <span className={'action-center__when' + (isOverdue ? ' action-center__when--overdue' : '')}>
        <strong>{dayLabel}</strong>
        <span>{isOverdue ? 'overdue' : timeFormatter.format(occurredAt)}</span>
      </span>
      <div className="action-center__content">
        <div className="action-center__badges">
          <span className="action-center__kind">{config.shortLabel}</span>
          {isOverdue && <span className="action-center__overdue-badge">Needs decision</span>}
        </div>
        <strong>{item.title}</strong>
        <span>
          {item.jobTitle} · {item.company}{item.contact ? ' · @ ' + item.contact : ''}
        </span>
      </div>
      <div className="action-center__actions">
        <button type="button" onClick={() => onComplete(item)}>
          Complete <span aria-hidden="true">✓</span>
        </button>
        {isOverdue && (
          <button type="button" onClick={() => onReschedule(item)}>Reschedule</button>
        )}
        <button type="button" onClick={() => onViewJob(item.jobId)}>
          View job <span aria-hidden="true">→</span>
        </button>
      </div>
    </article>
  )
}

function buildDailyActions(
  attentionItems: JobAttentionItem[],
  overdueActivities: UpcomingJobActivity[],
  upcomingActivities: UpcomingJobActivity[],
  now: Date,
): DailyAction[] {
  return [
    ...overdueActivities.map((item): DailyAction => ({
      kind: 'scheduled',
      item,
      priority: 0,
      sortValue: new Date(item.occurredAt).getTime(),
    })),
    ...attentionItems.map((item): DailyAction => ({
      kind: 'attention',
      item,
      priority: 2,
      sortValue: -(item.daysWithoutActivity - item.thresholdDays),
    })),
    ...upcomingActivities.map((item): DailyAction => {
      const occurredAt = new Date(item.occurredAt)
      return {
        kind: 'scheduled',
        item,
        priority: isSameLocalDay(occurredAt, now) ? 1 : 3,
        sortValue: occurredAt.getTime(),
      }
    }),
  ].sort((first, second) => (
    first.priority - second.priority || first.sortValue - second.sortValue
  ))
}

function getOverdueDayLabel(date: Date, now: Date): string {
  const dateDay = Date.UTC(date.getFullYear(), date.getMonth(), date.getDate())
  const today = Date.UTC(now.getFullYear(), now.getMonth(), now.getDate())
  const daysLate = Math.round((today - dateDay) / 86_400_000)
  return daysLate === 0 ? 'Today' : daysLate + 'd'
}

function getRelativeDayLabel(date: Date, now: Date): string {
  if (isSameLocalDay(date, now)) return 'Today'
  const tomorrow = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1)
  if (isSameLocalDay(date, tomorrow)) return 'Tomorrow'
  return dayFormatter.format(date)
}

function isSameLocalDay(first: Date, second: Date): boolean {
  return first.getFullYear() === second.getFullYear()
    && first.getMonth() === second.getMonth()
    && first.getDate() === second.getDate()
}

function getNotificationLabel(
  status: AttentionNotificationStatus,
  isRequesting: boolean,
): string {
  if (isRequesting) return 'Enabling…'
  return {
    unsupported: 'Alerts unavailable',
    disabled: 'Enable alerts',
    enabled: 'Alerts on',
    blocked: 'Alerts blocked',
  }[status]
}

function getNotificationTitle(status: AttentionNotificationStatus): string {
  return {
    unsupported: 'Browser notifications require a supported browser and secure connection.',
    disabled: 'Notify me about newly overdue applications while CareerPilot is open.',
    enabled: 'Browser alerts are enabled. Click to turn them off.',
    blocked: 'Allow notifications in your browser’s site settings, then return here.',
  }[status]
}