import { type FormEvent, useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { useActivityConflicts } from '../hooks/useActivityConflicts'
import type { Job } from '../types/job'
import { ActivityConflictNotice } from './ActivityConflictNotice'
import type { CreateJobActivityInput } from '../types/jobActivity'
import { toLocalDateTimeValue } from '../utils/jobActivity'

interface CalendarActivityDialogProps {
  jobs: Job[]
  initialDate: Date
  isJobsLoading: boolean
  jobsError: string | null
  isSaving: boolean
  error: string | null
  onClose: () => void
  onSubmit: (jobId: number, input: CreateJobActivityInput) => void
}

type CalendarActivityType = Extract<CreateJobActivityInput['type'], 'INTERVIEW' | 'FOLLOW_UP'>

export function CalendarActivityDialog({
  jobs,
  initialDate,
  isJobsLoading,
  jobsError,
  isSaving,
  error,
  onClose,
  onSubmit,
}: CalendarActivityDialogProps) {
  const jobSelectRef = useRef<HTMLSelectElement>(null)
  const [jobId, setJobId] = useState(() => jobs[0]?.id ?? 0)
  const [type, setType] = useState<CalendarActivityType>('INTERVIEW')
  const [title, setTitle] = useState('Interview')
  const [occurredAt, setOccurredAt] = useState(() => toLocalDateTimeValue(initialDate))
  const [contact, setContact] = useState('')
  const [details, setDetails] = useState('')
  const conflictCheck = useActivityConflicts({
    occurredAt,
    enabled: type === 'INTERVIEW',
  })

  const selectedJobId = jobId || jobs[0]?.id || 0

  useEffect(() => {
    jobSelectRef.current?.focus()
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape' && !isSaving) onClose()
    }

    document.addEventListener('keydown', handleKeyDown)
    return () => {
      document.removeEventListener('keydown', handleKeyDown)
      document.body.style.overflow = previousOverflow
    }
  }, [isSaving, onClose])

  function handleTypeChange(nextType: CalendarActivityType) {
    const defaultTitle = type === 'INTERVIEW' ? 'Interview' : 'Recruiter follow-up'
    setType(nextType)
    if (title === defaultTitle) {
      setTitle(nextType === 'INTERVIEW' ? 'Interview' : 'Recruiter follow-up')
    }
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (selectedJobId === 0 || !title.trim()) return

    onSubmit(selectedJobId, {
      type,
      title: title.trim(),
      occurredAt: new Date(occurredAt).toISOString(),
      contact: contact.trim() || undefined,
      details: details.trim() || undefined,
    })
  }

  return (
    <div
      className="quick-follow-up-backdrop"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget && !isSaving) onClose()
      }}
    >
      <section
        className="quick-follow-up-dialog calendar-activity-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="calendar-activity-heading"
      >
        <header className="quick-follow-up-dialog__heading">
          <div>
            <p>Plan from calendar</p>
            <h2 id="calendar-activity-heading">Add scheduled activity</h2>
            <span>{initialDate.toLocaleDateString('en-US', {
              weekday: 'long',
              month: 'long',
              day: 'numeric',
            })}</span>
          </div>
          <button
            type="button"
            aria-label="Close activity form"
            disabled={isSaving}
            onClick={onClose}
          >
            ×
          </button>
        </header>

        {isJobsLoading ? (
          <div className="calendar-activity-dialog__state">Loading jobs…</div>
        ) : jobs.length === 0 ? (
          <div className="calendar-activity-dialog__state">
            <strong>{jobsError ? 'Jobs could not be loaded.' : 'Add a job first.'}</strong>
            <span>{jobsError ?? 'Scheduled activities must belong to a job in your pipeline.'}</span>
            <Link className="button button--primary" to="/jobs/new">Add your first job</Link>
          </div>
        ) : (
          <form className="quick-follow-up-form calendar-activity-form" onSubmit={handleSubmit}>
            <label>
              Job
              <select
                ref={jobSelectRef}
                value={selectedJobId}
                disabled={isSaving}
                onChange={(event) => setJobId(Number(event.target.value))}
                required
              >
                {jobs.map((job) => (
                  <option key={job.id} value={job.id}>{job.company} — {job.title}</option>
                ))}
              </select>
            </label>

            <fieldset className="calendar-activity-form__types">
              <legend>Activity type</legend>
              <div>
                <button
                  type="button"
                  aria-pressed={type === 'INTERVIEW'}
                  className={type === 'INTERVIEW' ? 'calendar-activity-type--selected' : undefined}
                  onClick={() => handleTypeChange('INTERVIEW')}
                >
                  <span aria-hidden="true">◈</span>
                  <strong>Interview</strong>
                </button>
                <button
                  type="button"
                  aria-pressed={type === 'FOLLOW_UP'}
                  className={type === 'FOLLOW_UP' ? 'calendar-activity-type--selected' : undefined}
                  onClick={() => handleTypeChange('FOLLOW_UP')}
                >
                  <span aria-hidden="true">→</span>
                  <strong>Follow-up</strong>
                </button>
              </div>
            </fieldset>

            <label>
              Title
              <input
                type="text"
                maxLength={255}
                required
                disabled={isSaving}
                value={title}
                onChange={(event) => setTitle(event.target.value)}
              />
            </label>

            <div className="quick-follow-up-form__row">
              <label>
                Date and time
                <input
                  type="datetime-local"
                  required
                  disabled={isSaving}
                  value={occurredAt}
                  onChange={(event) => setOccurredAt(event.target.value)}
                />
              </label>
              <label>
                Contact <span>optional</span>
                <input
                  type="text"
                  maxLength={255}
                  disabled={isSaving}
                  placeholder="Recruiter name"
                  value={contact}
                  onChange={(event) => setContact(event.target.value)}
                />
              </label>
            </div>

            {type === 'INTERVIEW' && <ActivityConflictNotice {...conflictCheck} />}

            <label>
              Details <span>optional</span>
              <textarea
                rows={4}
                maxLength={5000}
                disabled={isSaving}
                placeholder="Agenda, questions, or next steps…"
                value={details}
                onChange={(event) => setDetails(event.target.value)}
              />
            </label>

            {(error || jobsError) && (
              <p className="quick-follow-up-form__error" role="alert">{error ?? jobsError}</p>
            )}

            <footer className="quick-follow-up-form__actions">
              <button
                className="button button--secondary"
                type="button"
                disabled={isSaving}
                onClick={onClose}
              >
                Cancel
              </button>
              <button
                className="button button--primary"
                type="submit"
                disabled={isSaving || selectedJobId === 0 || !title.trim()}
              >
                {isSaving ? 'Adding activity…' : 'Add to calendar'}
              </button>
            </footer>
          </form>
        )}
      </section>
    </div>
  )
}
