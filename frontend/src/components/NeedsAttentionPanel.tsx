import type { JobAttentionItem } from '../types/jobActivity'
import { formatDate } from '../utils/formatDate'
import { getJobStatusConfig } from '../utils/jobStatus'

interface NeedsAttentionPanelProps {
  items: JobAttentionItem[]
  onViewJob: (jobId: number) => void
}

export function NeedsAttentionPanel({ items, onViewJob }: NeedsAttentionPanelProps) {
  return (
    <section className="attention-panel" aria-labelledby="attention-heading">
      <div className="attention-panel__heading">
        <div className="attention-panel__title">
          <span aria-hidden="true">!</span>
          <div>
            <p>7+ days without activity</p>
            <h2 id="attention-heading">Needs attention</h2>
          </div>
        </div>
        <span>{items.length} {items.length === 1 ? 'application' : 'applications'}</span>
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
                aria-label={`View ${item.jobTitle} at ${item.company}, ${item.daysWithoutActivity} days without activity`}
                onClick={() => onViewJob(item.jobId)}
              >
                <span className="attention-card__age">
                  <strong>{item.daysWithoutActivity}</strong>
                  <span>days</span>
                </span>
                <span className="attention-card__content">
                  <span className={`status-badge status-badge--${status.tone}`}>
                    {status.label}
                  </span>
                  <strong>{item.jobTitle}</strong>
                  <span>{item.company} · Last activity {formatDate(item.lastActivityAt)}</span>
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
