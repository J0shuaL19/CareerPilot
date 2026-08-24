import type { ScheduledJobActivity } from '../types/jobActivity'

interface ActivityConflictNoticeProps {
  conflicts: ScheduledJobActivity[]
  isChecking: boolean
  error: string | null
}

const conflictTimeFormatter = new Intl.DateTimeFormat('en-US', {
  hour: 'numeric',
  minute: '2-digit',
})

export function ActivityConflictNotice({
  conflicts,
  isChecking,
  error,
}: ActivityConflictNoticeProps) {
  if (isChecking) {
    return (
      <div className="activity-conflict activity-conflict--checking" role="status">
        <span className="activity-conflict__icon" aria-hidden="true">◌</span>
        <span><strong>Checking schedule…</strong> Looking one hour around this time.</span>
      </div>
    )
  }

  if (error) {
    return (
      <div className="activity-conflict activity-conflict--error" role="status">
        <span className="activity-conflict__icon" aria-hidden="true">!</span>
        <span><strong>Conflict check unavailable</strong>{error}</span>
      </div>
    )
  }

  if (conflicts.length === 0) {
    return (
      <div className="activity-conflict activity-conflict--clear" role="status">
        <span className="activity-conflict__icon" aria-hidden="true">✓</span>
        <span><strong>Time looks clear</strong>No unfinished activities within 60 minutes.</span>
      </div>
    )
  }

  return (
    <section className="activity-conflict activity-conflict--warning" role="alert">
      <span className="activity-conflict__icon" aria-hidden="true">!</span>
      <div>
        <strong>Potential time conflict</strong>
        <p>
          {conflicts.length} unfinished {conflicts.length === 1
            ? 'activity occurs'
            : 'activities occur'} within 60 minutes. You can still save this interview.
        </p>
        <ul>
          {conflicts.slice(0, 3).map((conflict) => (
            <li key={conflict.id}>
              <time dateTime={conflict.occurredAt}>
                {conflictTimeFormatter.format(new Date(conflict.occurredAt))}
              </time>
              <span>
                <strong>{conflict.title}</strong>
                <small>{conflict.jobTitle} at {conflict.company}</small>
              </span>
            </li>
          ))}
        </ul>
        {conflicts.length > 3 && (
          <small>And {conflicts.length - 3} more nearby activities.</small>
        )}
      </div>
    </section>
  )
}
