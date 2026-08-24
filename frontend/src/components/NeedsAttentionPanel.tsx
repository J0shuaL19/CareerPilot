import type { AttentionNotificationStatus } from '../hooks/useAttentionNotifications'
import type { JobAttentionItem, JobAttentionSettings } from '../types/jobActivity'
import { formatDate } from '../utils/formatDate'
import { getJobStatusConfig } from '../utils/jobStatus'

interface NeedsAttentionPanelProps {
  items: JobAttentionItem[]
  settings: JobAttentionSettings
  onConfigure: () => void
  onFollowUp: (item: JobAttentionItem) => void
  notificationStatus: AttentionNotificationStatus
  isNotificationRequesting: boolean
  notificationError: string | null
  onToggleNotifications: () => void
  onSnooze: (item: JobAttentionItem) => void
}

export function NeedsAttentionPanel({
  items,
  settings,
  onConfigure,
  onFollowUp,
  notificationStatus,
  isNotificationRequesting,
  notificationError,
  onToggleNotifications,
  onSnooze,
}: NeedsAttentionPanelProps) {
  const rulesSummary = [
    'Applied ' + settings.appliedDays + 'd',
    'Assessment ' + settings.onlineAssessmentDays + 'd',
    'Interview ' + settings.interviewDays + 'd',
  ].join(' · ')

  return (
    <section className="attention-panel" aria-labelledby="attention-heading">
      <div className="attention-panel__heading">
        <div className="attention-panel__title">
          <span aria-hidden="true">!</span>
          <div>
            <p>{rulesSummary}</p>
            <h2 id="attention-heading">Needs attention</h2>
          </div>
        </div>
        <div className="attention-panel__controls">
          <span>{items.length} {items.length === 1 ? 'application' : 'applications'}</span>
          <button
            className={`attention-panel__notification attention-panel__notification--${notificationStatus}`}
            type="button"
            aria-pressed={notificationStatus === 'enabled'}
            title={getNotificationTitle(notificationStatus)}
            disabled={isNotificationRequesting || notificationStatus === 'unsupported'}
            onClick={onToggleNotifications}
          >
            <span aria-hidden="true">◉</span>
            {getNotificationLabel(notificationStatus, isNotificationRequesting)}
          </button>
          <button type="button" onClick={onConfigure}>
            Reminder rules
          </button>
        </div>
      </div>

      {notificationError && (
        <p className="attention-panel__notification-error" role="alert">
          {notificationError}
        </p>
      )}

      {items.length === 0 ? (
        <div className="attention-panel__empty">
          <span aria-hidden="true">✓</span>
          <div>
            <strong>Your active applications are up to date.</strong>
            <p>Jobs with recent, scheduled, or snoozed activity will stay out of this list.</p>
          </div>
        </div>
      ) : (
        <div className="attention-list">
          {items.map((item) => {
            const status = getJobStatusConfig(item.status)

            return (
              <article className="attention-card" key={item.jobId}>
                <span className="attention-card__age">
                  <strong>{item.daysWithoutActivity}</strong>
                  <span>days</span>
                </span>
                <span className="attention-card__content">
                  <span className={'status-badge status-badge--' + status.tone}>
                    {status.label}
                  </span>
                  <strong>{item.jobTitle}</strong>
                  <span>
                    {item.company} · Last activity {formatDate(item.lastActivityAt)}
                    {' '}· Rule {item.thresholdDays}d
                  </span>
                </span>
                <div className="attention-card__actions">
                  <button
                    type="button"
                    aria-label={'Follow up on ' + item.jobTitle + ' at ' + item.company}
                    onClick={() => onFollowUp(item)}
                  >
                    Follow up <span aria-hidden="true">→</span>
                  </button>
                  <button
                    type="button"
                    aria-label={'Snooze reminder for ' + item.jobTitle + ' at ' + item.company}
                    onClick={() => onSnooze(item)}
                  >
                    Snooze
                  </button>
                </div>
              </article>
            )
          })}
        </div>
      )}
    </section>
  )
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
