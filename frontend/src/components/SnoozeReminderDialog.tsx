import { type FormEvent, useEffect, useRef, useState } from 'react'

export interface SnoozeReminderTarget {
  company: string
  jobTitle: string
  thresholdDays?: number
  currentSnoozedUntil?: string | null
}

interface SnoozeReminderDialogProps {
  item: SnoozeReminderTarget
  isSaving: boolean
  error: string | null
  onClose: () => void
  onSubmit: (snoozedUntil: string) => void
}

const presets = [
  { days: 1, label: 'Tomorrow' },
  { days: 3, label: 'In 3 days' },
  { days: 7, label: 'In 1 week' },
] as const

export function SnoozeReminderDialog({
  item,
  isSaving,
  error,
  onClose,
  onSubmit,
}: SnoozeReminderDialogProps) {
  const dateRef = useRef<HTMLInputElement>(null)
  const tomorrow = toDateInputValue(addDays(new Date(), 1))
  const isRescheduling = Boolean(item.currentSnoozedUntil)
  const [snoozedUntil, setSnoozedUntil] = useState(
    item.currentSnoozedUntil && item.currentSnoozedUntil >= tomorrow
      ? item.currentSnoozedUntil
      : tomorrow,
  )

  useEffect(() => {
    dateRef.current?.focus()

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape' && !isSaving) {
        onClose()
      }
    }

    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [isSaving, onClose])

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    onSubmit(snoozedUntil)
  }

  return (
    <div
      className="quick-follow-up-backdrop"
      role="presentation"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget && !isSaving) {
          onClose()
        }
      }}
    >
      <section
        className="quick-follow-up-dialog snooze-reminder-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="snooze-reminder-heading"
      >
        <header className="quick-follow-up-dialog__heading">
          <div>
            <p>{isRescheduling ? 'Adjust one reminder' : 'Pause one reminder'}</p>
            <h2 id="snooze-reminder-heading">
              {isRescheduling ? 'Change snooze date' : 'Snooze until later'}
            </h2>
            <span>{item.jobTitle} · {item.company}</span>
          </div>
          <button
            type="button"
            aria-label="Close snooze reminder"
            disabled={isSaving}
            onClick={onClose}
          >
            ×
          </button>
        </header>

        <form className="quick-follow-up-form snooze-reminder-form" onSubmit={handleSubmit}>
          <p className="snooze-reminder-form__intro">
            {isRescheduling
              ? 'Choose a new date for this reminder to return to Needs attention.'
              : 'Hide this job from Needs attention without changing your stage rules.'}
          </p>

          <div className="snooze-reminder-presets" aria-label="Quick snooze dates">
            {presets.map((preset) => {
              const value = toDateInputValue(addDays(new Date(), preset.days))
              return (
                <button
                  className={snoozedUntil === value
                    ? 'snooze-reminder-preset snooze-reminder-preset--selected'
                    : 'snooze-reminder-preset'}
                  type="button"
                  key={preset.days}
                  aria-pressed={snoozedUntil === value}
                  disabled={isSaving}
                  onClick={() => setSnoozedUntil(value)}
                >
                  <strong>{preset.label}</strong>
                  <span>{formatDateLabel(value)}</span>
                </button>
              )
            })}
          </div>

          <label>
            Custom date
            <input
              ref={dateRef}
              type="date"
              min={tomorrow}
              required
              disabled={isSaving}
              value={snoozedUntil}
              onChange={(event) => setSnoozedUntil(event.target.value)}
            />
          </label>

          <p className="snooze-reminder-form__note">
            If the job is still overdue, it will return on {formatDateLabel(snoozedUntil)}.
          </p>

          {error && <p className="quick-follow-up-form__error" role="alert">{error}</p>}

          <div className="quick-follow-up-form__actions snooze-reminder-form__actions">
            <span>
              {isRescheduling && item.currentSnoozedUntil
                ? 'Currently: ' + formatDateLabel(item.currentSnoozedUntil)
                : 'Current rule: ' + item.thresholdDays + ' days'}
            </span>
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
                {isSaving
                  ? (isRescheduling ? 'Updating…' : 'Snoozing…')
                  : (isRescheduling ? 'Update date' : 'Snooze reminder')}
              </button>
            </div>
          </div>
        </form>
      </section>
    </div>
  )
}

function addDays(date: Date, days: number): Date {
  const result = new Date(date)
  result.setDate(result.getDate() + days)
  return result
}

function toDateInputValue(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return [year, month, day].join('-')
}

function formatDateLabel(value: string): string {
  return new Intl.DateTimeFormat('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  }).format(new Date(value + 'T12:00:00'))
}
