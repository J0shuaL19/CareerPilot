import { type FormEvent, useEffect, useRef, useState } from 'react'
import type { JobAttentionSettings } from '../types/jobActivity'

interface AttentionSettingsDialogProps {
  settings: JobAttentionSettings
  isSaving: boolean
  error: string | null
  onClose: () => void
  onSubmit: (settings: JobAttentionSettings) => void
}

type SettingsDraft = Record<keyof JobAttentionSettings, string>

const defaultSettings: SettingsDraft = {
  appliedDays: '7',
  onlineAssessmentDays: '7',
  interviewDays: '7',
}

const fields: ReadonlyArray<{
  key: keyof JobAttentionSettings
  label: string
  description: string
}> = [
  {
    key: 'appliedDays',
    label: 'Applied',
    description: 'After submitting an application.',
  },
  {
    key: 'onlineAssessmentDays',
    label: 'Online assessment',
    description: 'While waiting after an assessment.',
  },
  {
    key: 'interviewDays',
    label: 'Interview',
    description: 'After an interview or recruiter call.',
  },
]

export function AttentionSettingsDialog({
  settings,
  isSaving,
  error,
  onClose,
  onSubmit,
}: AttentionSettingsDialogProps) {
  const firstInputRef = useRef<HTMLInputElement>(null)
  const [draft, setDraft] = useState<SettingsDraft>({
    appliedDays: String(settings.appliedDays),
    onlineAssessmentDays: String(settings.onlineAssessmentDays),
    interviewDays: String(settings.interviewDays),
  })

  useEffect(() => {
    firstInputRef.current?.focus()

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
    onSubmit({
      appliedDays: Number(draft.appliedDays),
      onlineAssessmentDays: Number(draft.onlineAssessmentDays),
      interviewDays: Number(draft.interviewDays),
    })
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
        className="quick-follow-up-dialog attention-settings-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="attention-settings-heading"
      >
        <header className="quick-follow-up-dialog__heading">
          <div>
            <p>Reminder rules</p>
            <h2 id="attention-settings-heading">Follow-up timing</h2>
            <span>Choose when each active stage should appear in the daily action center.</span>
          </div>
          <button
            type="button"
            aria-label="Close reminder settings"
            disabled={isSaving}
            onClick={onClose}
          >
            ×
          </button>
        </header>

        <form className="quick-follow-up-form attention-settings-form" onSubmit={handleSubmit}>
          <div className="attention-settings-fields">
            {fields.map((field, index) => (
              <label key={field.key}>
                <span className="attention-settings-field__label">
                  <strong>{field.label}</strong>
                  <small>{field.description}</small>
                </span>
                <span className="attention-settings-field__input">
                  <input
                    ref={index === 0 ? firstInputRef : undefined}
                    type="number"
                    min={1}
                    max={90}
                    step={1}
                    required
                    disabled={isSaving}
                    aria-label={field.label + ' reminder days'}
                    value={draft[field.key]}
                    onChange={(event) => {
                      setDraft((current) => ({
                        ...current,
                        [field.key]: event.target.value,
                      }))
                    }}
                  />
                  <span>days</span>
                </span>
              </label>
            ))}
          </div>

          <p className="attention-settings-note">
            Scheduled future activities stay out of reminders until their date has passed.
          </p>

          {error && <p className="quick-follow-up-form__error" role="alert">{error}</p>}

          <div className="quick-follow-up-form__actions">
            <button
              className="attention-settings-reset"
              type="button"
              disabled={isSaving}
              onClick={() => setDraft(defaultSettings)}
            >
              Restore 7-day defaults
            </button>
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
                {isSaving ? 'Saving…' : 'Save rules'}
              </button>
            </div>
          </div>
        </form>
      </section>
    </div>
  )
}
