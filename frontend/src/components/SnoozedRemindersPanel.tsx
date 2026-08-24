import type { Job } from '../types/job'
import { formatDate } from '../utils/formatDate'
import { getJobStatusConfig } from '../utils/jobStatus'

interface SnoozedRemindersPanelProps {
  jobs: Job[]
  busyJobId: number | null
  error: string | null
  onChangeDate: (job: Job) => void
  onResume: (job: Job) => void
  onViewJob: (jobId: number) => void
}

export function SnoozedRemindersPanel({
  jobs,
  busyJobId,
  error,
  onChangeDate,
  onResume,
  onViewJob,
}: SnoozedRemindersPanelProps) {
  return (
    <section className="snoozed-reminders" aria-labelledby="snoozed-reminders-heading">
      <div className="snoozed-reminders__heading">
        <div className="snoozed-reminders__title">
          <span aria-hidden="true">◷</span>
          <div>
            <p>Temporarily paused</p>
            <h2 id="snoozed-reminders-heading">Snoozed reminders</h2>
          </div>
        </div>
        <span>
          {jobs.length} {jobs.length === 1 ? 'reminder' : 'reminders'}
        </span>
      </div>

      <p className="snoozed-reminders__intro">
        These applications will return to Needs attention on their scheduled dates.
      </p>

      {error && (
        <p className="snoozed-reminders__error" role="alert">
          {error}
        </p>
      )}

      <div className="snoozed-reminder-list">
        {jobs.map((job) => {
          const status = getJobStatusConfig(job.status)
          const isBusy = busyJobId === job.id

          return (
            <article className="snoozed-reminder-card" key={job.id}>
              <span className="snoozed-reminder-card__date">
                <small>Returns</small>
                <strong>
                  {formatDate((job.attentionSnoozedUntil ?? '') + 'T12:00:00')}
                </strong>
              </span>

              <button
                className="snoozed-reminder-card__job"
                type="button"
                aria-label={'View ' + job.title + ' at ' + job.company}
                onClick={() => onViewJob(job.id)}
              >
                <span className={'status-badge status-badge--' + status.tone}>
                  {status.label}
                </span>
                <strong>{job.title}</strong>
                <span>{job.company}</span>
              </button>

              <div className="snoozed-reminder-card__actions">
                <button
                  type="button"
                  disabled={isBusy}
                  onClick={() => onChangeDate(job)}
                >
                  Change date
                </button>
                <button
                  className="snoozed-reminder-card__resume"
                  type="button"
                  disabled={isBusy}
                  onClick={() => onResume(job)}
                >
                  {isBusy ? 'Resuming…' : 'Resume now'}
                </button>
              </div>
            </article>
          )
        })}
      </div>
    </section>
  )
}
