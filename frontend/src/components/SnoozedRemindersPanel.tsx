import { useState } from 'react'
import type { Job } from '../types/job'
import { formatDate } from '../utils/formatDate'
import { getJobStatusConfig } from '../utils/jobStatus'

interface SnoozedRemindersPanelProps {
  jobs: Job[]
  busyJobId: number | null
  isBulkBusy: boolean
  error: string | null
  onChangeDate: (job: Job) => void
  onResume: (job: Job) => void
  onBulkChangeDate: (jobs: Job[]) => void
  onBulkResume: (jobs: Job[]) => void
  onViewJob: (jobId: number) => void
}

export function SnoozedRemindersPanel({
  jobs,
  busyJobId,
  isBulkBusy,
  error,
  onChangeDate,
  onResume,
  onBulkChangeDate,
  onBulkResume,
  onViewJob,
}: SnoozedRemindersPanelProps) {
  const [selectedIds, setSelectedIds] = useState<number[]>([])

  const selectedJobs = jobs.filter((job) => selectedIds.includes(job.id))
  const allSelected = selectedJobs.length === jobs.length

  function toggleJob(jobId: number) {
    setSelectedIds((current) => (
      current.includes(jobId)
        ? current.filter((id) => id !== jobId)
        : [...current, jobId]
    ))
  }

  function toggleAll() {
    setSelectedIds(allSelected ? [] : jobs.map((job) => job.id))
  }

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
        <div className="snoozed-reminders__meta">
          <span>
            {jobs.length} {jobs.length === 1 ? 'reminder' : 'reminders'}
          </span>
          <button type="button" disabled={isBulkBusy} onClick={toggleAll}>
            {allSelected ? 'Clear selection' : 'Select all'}
          </button>
        </div>
      </div>

      <p className="snoozed-reminders__intro">
        Select reminders to give them one return date or resume them together.
      </p>

      {selectedJobs.length > 0 && (
        <div className="snoozed-reminders__bulk" role="toolbar" aria-label="Selected reminder actions">
          <strong>
            {selectedJobs.length} {selectedJobs.length === 1 ? 'reminder' : 'reminders'} selected
          </strong>
          <div>
            <button
              type="button"
              disabled={isBulkBusy}
              onClick={() => onBulkChangeDate(selectedJobs)}
            >
              Change dates
            </button>
            <button
              className="snoozed-reminders__bulk-resume"
              type="button"
              disabled={isBulkBusy}
              onClick={() => onBulkResume(selectedJobs)}
            >
              {isBulkBusy ? 'Resuming…' : 'Resume selected'}
            </button>
            <button
              className="snoozed-reminders__bulk-clear"
              type="button"
              disabled={isBulkBusy}
              onClick={() => setSelectedIds([])}
            >
              Clear
            </button>
          </div>
        </div>
      )}

      {error && (
        <p className="snoozed-reminders__error" role="alert">
          {error}
        </p>
      )}

      <div className="snoozed-reminder-list">
        {jobs.map((job) => {
          const status = getJobStatusConfig(job.status)
          const isSelected = selectedIds.includes(job.id)
          const isBusy = busyJobId === job.id || isBulkBusy

          return (
            <article
              className={isSelected
                ? 'snoozed-reminder-card snoozed-reminder-card--selected'
                : 'snoozed-reminder-card'}
              key={job.id}
            >
              <label className="snoozed-reminder-card__select">
                <input
                  type="checkbox"
                  checked={isSelected}
                  disabled={isBulkBusy}
                  aria-label={'Select reminder for ' + job.title + ' at ' + job.company}
                  onChange={() => toggleJob(job.id)}
                />
              </label>

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
                  {busyJobId === job.id ? 'Resuming…' : 'Resume now'}
                </button>
              </div>
            </article>
          )
        })}
      </div>
    </section>
  )
}
