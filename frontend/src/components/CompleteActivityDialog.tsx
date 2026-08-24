import { type FormEvent, useEffect, useRef, useState } from 'react'
import type { JobStatus } from '../types/job'
import type { CompleteJobActivityInput, UpcomingJobActivity } from '../types/jobActivity'
import { getJobActivityTypeConfig } from '../utils/jobActivity'
import { getJobStatusConfig, jobStatusOptions } from '../utils/jobStatus'

interface CompleteActivityDialogProps {
  activity: UpcomingJobActivity
  currentStatus: JobStatus
  isSaving: boolean
  error: string | null
  onClose: () => void
  onSubmit: (input: CompleteJobActivityInput) => void
}

const maxNoteLength = 2000

export function CompleteActivityDialog({
  activity,
  currentStatus,
  isSaving,
  error,
  onClose,
  onSubmit,
}: CompleteActivityDialogProps) {
  const noteRef = useRef<HTMLTextAreaElement>(null)
  const [note, setNote] = useState('')
  const [jobStatus, setJobStatus] = useState<JobStatus | ''>('')
  const activityConfig = getJobActivityTypeConfig(activity.type)
  const currentStatusConfig = getJobStatusConfig(currentStatus)

  useEffect(() => {
    noteRef.current?.focus()

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape' && !isSaving) onClose()
    }

    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [isSaving, onClose])

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const normalizedNote = note.trim()
    onSubmit({
      note: normalizedNote || undefined,
      jobStatus: jobStatus || undefined,
    })
  }

  return (
    <div
      className="quick-follow-up-backdrop"
      role="presentation"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget && !isSaving) onClose()
      }}
    >
      <section
        className="quick-follow-up-dialog complete-activity-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="complete-activity-heading"
      >
        <header className="quick-follow-up-dialog__heading">
          <div>
            <p>Finish scheduled action</p>
            <h2 id="complete-activity-heading">Complete {activityConfig.shortLabel.toLowerCase()}</h2>
            <span>{activity.title} · {activity.jobTitle} at {activity.company}</span>
          </div>
          <button
            type="button"
            aria-label="Close activity completion"
            disabled={isSaving}
            onClick={onClose}
          >
            ×
          </button>
        </header>

        <form className="quick-follow-up-form complete-activity-form" onSubmit={handleSubmit}>
          <p className="complete-activity-form__intro">
            Save the outcome to this job’s timeline. The completed activity will leave your
            upcoming action list.
          </p>

          <label>
            Outcome note
            <textarea
              ref={noteRef}
              rows={5}
              maxLength={maxNoteLength}
              disabled={isSaving}
              value={note}
              placeholder={
                activity.type === 'INTERVIEW'
                  ? 'What went well, what to follow up on, and the expected next step…'
                  : 'What you sent, who you contacted, and when to check back…'
              }
              onChange={(event) => setNote(event.target.value)}
            />
            <span className="complete-activity-form__count">
              {note.length}/{maxNoteLength}
            </span>
          </label>

          <label>
            Update job stage
            <select
              disabled={isSaving}
              value={jobStatus}
              onChange={(event) => setJobStatus(event.target.value as JobStatus | '')}
            >
              <option value="">Keep {currentStatusConfig.label}</option>
              {jobStatusOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}{option.value === currentStatus ? ' (current)' : ''}
                </option>
              ))}
            </select>
            <span>Optional. Choose a new stage only when this activity changed the pipeline.</span>
          </label>

          {error && <p className="quick-follow-up-form__error" role="alert">{error}</p>}

          <div className="quick-follow-up-form__actions complete-activity-form__actions">
            <span>The original scheduled time remains in the timeline.</span>
            <div>
              <button
                className="button button--secondary"
                type="button"
                disabled={isSaving}
                onClick={onClose}
              >
                Cancel
              </button>
              <button className="button button--primary" type="submit" disabled={isSaving}>
                {isSaving ? 'Completing…' : 'Complete activity'}
              </button>
            </div>
          </div>
        </form>
      </section>
    </div>
  )
}