import type { JobAttentionItem, JobAttentionSettings } from '../types/jobActivity'
import { formatDate } from '../utils/formatDate'
import { getJobStatusConfig } from '../utils/jobStatus'

interface NeedsAttentionPanelProps {
  items: JobAttentionItem[]
  settings: JobAttentionSettings
  onConfigure: () => void
  onFollowUp: (item: JobAttentionItem) => void
}

export function NeedsAttentionPanel({
  items,
  settings,
  onConfigure,
  onFollowUp,
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
          <button type="button" onClick={onConfigure}>
            Reminder rules
          </button>
        </div>
      </div>

      {items.length === 0 ? (
        <div className="attention-panel__empty">
          <span aria-hidden="true">✓</span>
          <div>
            <strong>Your active applications are up to date.</strong>
            <p>Jobs with recent or scheduled activity will stay out of this list.</p>
          </div>
        </div>
      ) : (
        <div className="attention-list">
          {items.map((item) => {
            const status = getJobStatusConfig(item.status)

            return (
              <button
                className="attention-card"
                type="button"
                key={item.jobId}
                aria-label={[
                  'Follow up on ' + item.jobTitle + ' at ' + item.company,
                  item.daysWithoutActivity + ' days without activity',
                  'reminder set to ' + item.thresholdDays + ' days',
                ].join(', ')}
                onClick={() => onFollowUp(item)}
              >
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
                <span className="attention-card__action">
                  Follow up <span aria-hidden="true">→</span>
                </span>
              </button>
            )
          })}
        </div>
      )}
    </section>
  )
}
