import { type FormEvent, useEffect, useRef, useState } from 'react'
import type { UpcomingJobActivity } from '../types/jobActivity'
import { getJobActivityTypeConfig, toLocalDateTimeValue } from '../utils/jobActivity'

interface RescheduleActivityDialogProps {
  activity: UpcomingJobActivity
  isSaving: boolean
  error: string | null
  onClose: () => void
  onSubmit: (occurredAt: string) => void
}

export function RescheduleActivityDialog({
  activity,
  isSaving,
  error,
  onClose,
  onSubmit,
}: RescheduleActivityDialogProps) {
  const inputRef = useRef<HTMLInputElement>(null)
  const [occurredAt, setOccurredAt] = useState(() => getSuggestedTime(activity.occurredAt))
  const [wasOverdue] = useState(() => new Date(activity.occurredAt) < new Date())
  const [minimumTime] = useState(() => toLocalDateTimeValue(new Date(Date.now() + 60_000)))
  const [validationError, setValidationError] = useState<string | null>(null)
  const activityConfig = getJobActivityTypeConfig(activity.type)

  useEffect(() => {
    inputRef.current?.focus()

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape' && !isSaving) onClose()
    }

    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [isSaving, onClose])

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const nextTime = new Date(occurredAt)
    if (Number.isNaN(nextTime.getTime()) || nextTime <= new Date()) {
      setValidationError('Choose a future date and time.')
      return
    }
    setValidationError(null)
    onSubmit(nextTime.toISOString())
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
        className="quick-follow-up-dialog reschedule-activity-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="reschedule-activity-heading"
      >
        <header className="quick-follow-up-dialog__heading">
          <div>
            <p>{wasOverdue ? 'Move overdue action' : 'Change scheduled time'}</p>
            <h2 id="reschedule-activity-heading">Reschedule {activityConfig.shortLabel.toLowerCase()}</h2>
            <span>{activity.title} · {activity.jobTitle} at {activity.company}</span>
          </div>
          <button
            type="button"
            aria-label="Close activity reschedule"
            disabled={isSaving}
            onClick={onClose}
          >
            ×
          </button>
        </header>

        <form className="quick-follow-up-form" onSubmit={handleSubmit}>
          <p className="reschedule-activity-dialog__intro">
            Pick the next time this action should return to your daily plan.
          </p>
          <label>
            New date and time
            <input
              ref={inputRef}
              type="datetime-local"
              min={minimumTime}
              disabled={isSaving}
              value={occurredAt}
              onChange={(event) => {
                setOccurredAt(event.target.value)
                setValidationError(null)
              }}
              required
            />
            <span>The original timeline entry is updated instead of creating a duplicate.</span>
          </label>

          {(validationError || error) && (
            <p className="quick-follow-up-form__error" role="alert">
              {validationError ?? error}
            </p>
          )}

          <div className="quick-follow-up-form__actions">
            <button
              className="button button--secondary"
              type="button"
              disabled={isSaving}
              onClick={onClose}
            >
              Cancel
            </button>
            <button className="button button--primary" type="submit" disabled={isSaving}>
              {isSaving ? 'Rescheduling…' : 'Save new time'}
            </button>
          </div>
        </form>
      </section>
    </div>
  )
}

function getSuggestedTime(occurredAt: string): string {
  const suggested = new Date(occurredAt)
  const now = new Date()
  while (suggested <= now) suggested.setDate(suggested.getDate() + 1)
  return toLocalDateTimeValue(suggested)
}