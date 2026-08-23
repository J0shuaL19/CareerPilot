import type { UpcomingJobActivity } from '../types/jobActivity'
import { getJobActivityTypeConfig } from '../utils/jobActivity'

interface UpcomingActivitiesProps {
  activities: UpcomingJobActivity[]
  isLoading: boolean
  error: string | null
  onRetry: () => void
  onViewJob: (jobId: number) => void
}

const dayFormatter = new Intl.DateTimeFormat('en-US', {
  month: 'short',
  day: 'numeric',
})

const timeFormatter = new Intl.DateTimeFormat('en-US', {
  hour: 'numeric',
  minute: '2-digit',
})

export function UpcomingActivities({
  activities,
  isLoading,
  error,
  onRetry,
  onViewJob,
}: UpcomingActivitiesProps) {
  return (
    <section className="upcoming-panel" aria-labelledby="upcoming-heading">
      <div className="upcoming-panel__heading">
        <div className="upcoming-panel__title">
          <span aria-hidden="true">◇</span>
          <div>
            <p>Next 14 days</p>
            <h2 id="upcoming-heading">Upcoming</h2>
          </div>
        </div>
        {!isLoading && !error && <span>{activities.length} scheduled</span>}
      </div>

      {isLoading && (
        <div className="upcoming-panel__state" role="status">
          <div className="spinner" aria-hidden="true" />
          <span>Checking your schedule…</span>
        </div>
      )}

      {!isLoading && error && (
        <div className="upcoming-panel__state upcoming-panel__state--error" role="alert">
          <span>{error}</span>
          <button type="button" onClick={onRetry}>Try again</button>
        </div>
      )}

      {!isLoading && !error && activities.length === 0 && (
        <div className="upcoming-panel__state">
          <span>No interviews or follow-ups scheduled yet.</span>
          <span>Add a future activity from any job timeline.</span>
        </div>
      )}

      {!isLoading && !error && activities.length > 0 && (
        <div className="upcoming-list">
          {activities.map((activity) => {
            const occurredAt = new Date(activity.occurredAt)
            const config = getJobActivityTypeConfig(activity.type)

            return (
              <button
                className={`upcoming-card upcoming-card--${activity.type.toLowerCase()}`}
                type="button"
                key={activity.id}
                aria-label={`View ${activity.title} for ${activity.jobTitle} at ${activity.company}`}
                onClick={() => onViewJob(activity.jobId)}
              >
                <span className="upcoming-card__date">
                  <strong>{dayFormatter.format(occurredAt)}</strong>
                  <span>{timeFormatter.format(occurredAt)}</span>
                </span>
                <span className="upcoming-card__content">
                  <span className="upcoming-card__type">{config.shortLabel}</span>
                  <strong>{activity.title}</strong>
                  <span>{activity.jobTitle} · {activity.company}</span>
                  {activity.contact && <span className="upcoming-card__contact">@ {activity.contact}</span>}
                </span>
                <span className="upcoming-card__arrow" aria-hidden="true">→</span>
              </button>
            )
          })}
        </div>
      )}
    </section>
  )
}
