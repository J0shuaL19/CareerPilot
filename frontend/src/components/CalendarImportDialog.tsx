import { type FormEvent, useEffect, useMemo, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  importJobActivityCalendar,
  previewJobActivityCalendar,
} from '../services/jobActivityApi'
import type { Job } from '../types/job'
import type {
  JobActivityCalendarImportPreview,
  JobActivityCalendarImportResult,
  JobActivityCalendarImportType,
} from '../types/jobActivity'
import { getErrorMessage } from '../utils/errors'

interface CalendarImportDialogProps {
  jobs: Job[]
  preferredJobId: number | null
  onClose: () => void
  onImported: (result: JobActivityCalendarImportResult) => void
}

interface EventAssignment {
  selected: boolean
  jobId: number
  type: JobActivityCalendarImportType
}

const dateTimeFormatter = new Intl.DateTimeFormat('en-US', {
  weekday: 'short',
  month: 'short',
  day: 'numeric',
  year: 'numeric',
  hour: 'numeric',
  minute: '2-digit',
})

export function CalendarImportDialog({
  jobs,
  preferredJobId,
  onClose,
  onImported,
}: CalendarImportDialogProps) {
  const fileInputRef = useRef<HTMLInputElement>(null)
  const [file, setFile] = useState<File | null>(null)
  const [preview, setPreview] = useState<JobActivityCalendarImportPreview | null>(null)
  const [assignments, setAssignments] = useState<Record<number, EventAssignment>>({})
  const [isPreviewing, setIsPreviewing] = useState(false)
  const [isImporting, setIsImporting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const sortedJobs = useMemo(() => [...jobs].sort((left, right) => (
    left.company.localeCompare(right.company) || left.title.localeCompare(right.title)
  )), [jobs])
  const defaultJobId = preferredJobId ?? sortedJobs[0]?.id ?? 0
  const selectedEvents = preview?.events.filter((event) => (
    event.importable && assignments[event.eventNumber]?.selected
  )) ?? []
  const hasMissingJob = selectedEvents.some((event) => (
    !assignments[event.eventNumber]?.jobId
  ))

  useEffect(() => {
    fileInputRef.current?.focus()
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape' && !isPreviewing && !isImporting) onClose()
    }

    document.addEventListener('keydown', handleKeyDown)
    return () => {
      document.removeEventListener('keydown', handleKeyDown)
      document.body.style.overflow = previousOverflow
    }
  }, [isImporting, isPreviewing, onClose])

  function chooseFile(nextFile: File | null) {
    setFile(nextFile)
    setPreview(null)
    setAssignments({})
    setError(null)
  }

  async function handlePreview(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!file) return

    setIsPreviewing(true)
    setError(null)
    try {
      const nextPreview = await previewJobActivityCalendar(file)
      setPreview(nextPreview)
      setAssignments(Object.fromEntries(nextPreview.events.map((calendarEvent) => [
        calendarEvent.eventNumber,
        {
          selected: calendarEvent.importable,
          jobId: defaultJobId,
          type: calendarEvent.suggestedType,
        },
      ])))
    } catch (previewError) {
      setPreview(null)
      setAssignments({})
      setError(getErrorMessage(previewError))
    } finally {
      setIsPreviewing(false)
    }
  }

  function updateAssignment(eventNumber: number, update: Partial<EventAssignment>) {
    setAssignments((current) => ({
      ...current,
      [eventNumber]: {
        ...current[eventNumber],
        ...update,
      },
    }))
  }

  async function handleImport() {
    if (!preview || selectedEvents.length === 0 || hasMissingJob) return

    setIsImporting(true)
    setError(null)
    try {
      const result = await importJobActivityCalendar({
        events: selectedEvents.map((event) => ({
          jobId: assignments[event.eventNumber].jobId,
          type: assignments[event.eventNumber].type,
          title: event.title,
          details: event.details ?? undefined,
          contact: event.contact ?? undefined,
          occurredAt: event.occurredAt!,
        })),
      })
      onImported(result)
    } catch (importError) {
      setError(getErrorMessage(importError))
    } finally {
      setIsImporting(false)
    }
  }

  return (
    <div
      className="quick-follow-up-backdrop"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget && !isPreviewing && !isImporting) onClose()
      }}
    >
      <section
        className="quick-follow-up-dialog calendar-import-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="calendar-import-heading"
      >
        <header className="quick-follow-up-dialog__heading">
          <div>
            <p>Bring in an external calendar</p>
            <h2 id="calendar-import-heading">Import ICS events</h2>
            <span>Preview first, then choose which job each event belongs to.</span>
          </div>
          <button
            type="button"
            aria-label="Close calendar import"
            disabled={isPreviewing || isImporting}
            onClick={onClose}
          >
            ×
          </button>
        </header>

        {jobs.length === 0 ? (
          <div className="calendar-activity-dialog__state">
            <strong>Add a job before importing events.</strong>
            <span>Every interview or follow-up must belong to a job in your pipeline.</span>
            <Link className="button button--primary" to="/jobs/new">Add your first job</Link>
          </div>
        ) : (
          <>
            <form className="calendar-import-picker" onSubmit={handlePreview}>
              <label>
                <span>ICS calendar file</span>
                <input
                  ref={fileInputRef}
                  type="file"
                  accept=".ics,text/calendar"
                  disabled={isPreviewing || isImporting}
                  onChange={(event) => chooseFile(event.target.files?.[0] ?? null)}
                />
                <small>UTF-8 · up to 1 MB · maximum 100 events</small>
              </label>
              <button
                className="button button--secondary"
                type="submit"
                disabled={!file || isPreviewing || isImporting}
              >
                {isPreviewing ? 'Reading calendar…' : preview ? 'Preview again' : 'Preview events'}
              </button>
            </form>

            {preview && (
              <div className="calendar-import-preview">
                <div className="calendar-import-summary">
                  <div>
                    <strong>{preview.totalEvents}</strong>
                    <span>Total events</span>
                  </div>
                  <div>
                    <strong>{preview.importableEvents}</strong>
                    <span>Ready to map</span>
                  </div>
                  <div className={preview.invalidEvents ? 'calendar-import-summary__warning' : ''}>
                    <strong>{preview.invalidEvents}</strong>
                    <span>Need attention</span>
                  </div>
                  <p title={preview.filename}>{preview.filename}</p>
                </div>

                <div className="calendar-import-events">
                  {preview.events.map((calendarEvent) => {
                    const assignment = assignments[calendarEvent.eventNumber]
                    return (
                      <article
                        key={calendarEvent.eventNumber}
                        className={'calendar-import-event'
                          + (calendarEvent.importable ? '' : ' calendar-import-event--invalid')}
                      >
                        <label className="calendar-import-event__select">
                          <input
                            type="checkbox"
                            checked={assignment?.selected ?? false}
                            disabled={!calendarEvent.importable || isImporting}
                            onChange={(event) => updateAssignment(calendarEvent.eventNumber, {
                              selected: event.target.checked,
                            })}
                          />
                          <span>Event {calendarEvent.eventNumber}</span>
                        </label>
                        <div className="calendar-import-event__content">
                          <div className="calendar-import-event__heading">
                            <div>
                              <strong>{calendarEvent.title}</strong>
                              <span>
                                {calendarEvent.occurredAt
                                  ? dateTimeFormatter.format(new Date(calendarEvent.occurredAt))
                                  : 'Time unavailable'}
                              </span>
                            </div>
                            <span className={calendarEvent.importable
                              ? 'calendar-import-event__status'
                              : 'calendar-import-event__status calendar-import-event__status--invalid'}>
                              {calendarEvent.importable ? 'Ready' : 'Cannot import'}
                            </span>
                          </div>

                          {(calendarEvent.contact || calendarEvent.details) && (
                            <p>
                              {calendarEvent.contact && <span>Location: {calendarEvent.contact}</span>}
                              {calendarEvent.details && <span>{calendarEvent.details}</span>}
                            </p>
                          )}

                          {calendarEvent.errors.length > 0 && (
                            <ul className="calendar-import-event__errors">
                              {calendarEvent.errors.map((eventError) => (
                                <li key={eventError}>{eventError}</li>
                              ))}
                            </ul>
                          )}

                          {calendarEvent.importable && assignment?.selected && (
                            <div className="calendar-import-event__mapping">
                              <label>
                                Job
                                <select
                                  value={assignment.jobId}
                                  disabled={isImporting}
                                  onChange={(event) => updateAssignment(calendarEvent.eventNumber, {
                                    jobId: Number(event.target.value),
                                  })}
                                  required
                                >
                                  <option value={0}>Choose a job</option>
                                  {sortedJobs.map((job) => (
                                    <option key={job.id} value={job.id}>
                                      {job.company} — {job.title}
                                    </option>
                                  ))}
                                </select>
                              </label>
                              <label>
                                Activity type
                                <select
                                  value={assignment.type}
                                  disabled={isImporting}
                                  onChange={(event) => updateAssignment(calendarEvent.eventNumber, {
                                    type: event.target.value as JobActivityCalendarImportType,
                                  })}
                                >
                                  <option value="INTERVIEW">Interview</option>
                                  <option value="FOLLOW_UP">Follow-up</option>
                                </select>
                              </label>
                            </div>
                          )}
                        </div>
                      </article>
                    )
                  })}
                </div>
              </div>
            )}

            {error && (
              <p className="quick-follow-up-form__error calendar-import-error" role="alert">
                {error}
              </p>
            )}

            <footer className="quick-follow-up-form__actions calendar-import-actions">
              <span>Exact duplicates for the same job, type, title, and time will be skipped.</span>
              <div>
                <button
                  className="button button--secondary"
                  type="button"
                  disabled={isPreviewing || isImporting}
                  onClick={onClose}
                >
                  Cancel
                </button>
                <button
                  className="button button--primary"
                  type="button"
                  disabled={!preview || selectedEvents.length === 0 || hasMissingJob || isImporting}
                  onClick={() => void handleImport()}
                >
                  {isImporting
                    ? 'Importing…'
                    : 'Import selected (' + selectedEvents.length + ')'}
                </button>
              </div>
            </footer>
          </>
        )}
      </section>
    </div>
  )
}