import { useEffect } from 'react'
import { Link } from 'react-router-dom'
import type { Job } from '../types/job'
import type { ScheduledJobActivity } from '../types/jobActivity'
import { getJobActivityTypeConfig } from '../utils/jobActivity'
import { getJobStatusConfig } from '../utils/jobStatus'

interface CalendarActivityDetailsDialogProps {
  activity: ScheduledJobActivity
  job: Job | undefined
  isReopening: boolean
  error: string | null
  onClose: () => void
  onComplete: () => void
  onPrepare: () => void
  onReschedule: () => void
  onReopen: () => void
}

const fullDateFormatter = new Intl.DateTimeFormat('en-US', {
  weekday: 'long',
  month: 'long',
  day: 'numeric',
  year: 'numeric',
})
const timeFormatter = new Intl.DateTimeFormat('en-US', {
  hour: 'numeric',
  minute: '2-digit',
})

export function CalendarActivityDetailsDialog({
  activity,
  job,
  isReopening,
  error,
  onClose,
  onComplete,
  onPrepare,
  onReschedule,
  onReopen,
}: CalendarActivityDetailsDialogProps) {
  const activityConfig = getJobActivityTypeConfig(activity.type)
  const occurredAt = new Date(activity.occurredAt)
  const isCompleted = activity.completedAt !== null
  const jobStatus = job ? getJobStatusConfig(job.status) : null

  useEffect(() => {
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape' && !isReopening) onClose()
    }

    document.addEventListener('keydown', handleKeyDown)
    return () => {
      document.removeEventListener('keydown', handleKeyDown)
      document.body.style.overflow = previousOverflow
    }
  }, [isReopening, onClose])

  return (
    <div
      className="quick-follow-up-backdrop calendar-details-backdrop"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget && !isReopening) onClose()
      }}
    >
      <section
        className="quick-follow-up-dialog calendar-details-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="calendar-details-heading"
      >
        <header className="quick-follow-up-dialog__heading">
          <div>
            <p>Calendar activity</p>
            <h2 id="calendar-details-heading">{activity.title}</h2>
            <span>{activity.jobTitle} at {activity.company}</span>
          </div>
          <button
            type="button"
            aria-label="Close activity details"
            disabled={isReopening}
            onClick={onClose}
          >
            ×
          </button>
        </header>

        <div className="calendar-details-dialog__body">
          <div className="calendar-details-dialog__badges">
            <span className={'calendar-details-dialog__type calendar-details-dialog__type--'
              + activity.type.toLowerCase()}>
              <span aria-hidden="true">{activityConfig.symbol}</span> {activityConfig.shortLabel}
            </span>
            <span className={isCompleted
              ? 'calendar-details-dialog__state calendar-details-dialog__state--completed'
              : 'calendar-details-dialog__state'}>
              {isCompleted ? '✓ Completed' : 'Scheduled'}
            </span>
          </div>

          <dl className="calendar-details-dialog__facts">
            <div>
              <dt>Date</dt>
              <dd>{fullDateFormatter.format(occurredAt)}</dd>
            </div>
            <div>
              <dt>Time</dt>
              <dd>{timeFormatter.format(occurredAt)}</dd>
            </div>
            <div>
              <dt>Contact</dt>
              <dd>{activity.contact ?? 'Not added'}</dd>
            </div>
            <div>
              <dt>Job stage</dt>
              <dd>{jobStatus?.label ?? 'Unavailable'}</dd>
            </div>
          </dl>

          <p className="calendar-details-dialog__guidance">
            {isCompleted
              ? 'This activity stays on the calendar as completed history. Reopen it only if more work is still required.'
              : 'Complete the activity when the action is finished, or reschedule it to move the same timeline entry.'}
          </p>

          {error && <p className="quick-follow-up-form__error" role="alert">{error}</p>}
        </div>

        <footer className="calendar-details-dialog__actions">
          <Link className="button button--secondary" to={'/jobs/' + activity.jobId}>View job</Link>
          <div>
            {activity.type === 'INTERVIEW' && (
              <button
                className="button button--secondary"
                type="button"
                disabled={isReopening}
                onClick={onPrepare}
              >
                Prepare
              </button>
            )}
            {isCompleted ? (
              <button
                className="button button--primary"
                type="button"
                disabled={isReopening}
                onClick={onReopen}
              >
                {isReopening ? 'Reopening…' : 'Reopen activity'}
              </button>
            ) : (
              <>
                <button
                  className="button button--secondary"
                  type="button"
                  onClick={onReschedule}
                >
                  Reschedule
                </button>
                <button
                  className="button button--primary"
                  type="button"
                  disabled={!job}
                  title={job ? undefined : 'Job details are still loading.'}
                  onClick={onComplete}
                >
                  Complete activity
                </button>
              </>
            )}
          </div>
        </footer>
      </section>
    </div>
  )
}
