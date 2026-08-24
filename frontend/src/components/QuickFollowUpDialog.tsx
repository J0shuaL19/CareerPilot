import { type FormEvent, useEffect, useRef, useState } from 'react'
import type { CreateJobActivityInput, JobAttentionItem } from '../types/jobActivity'
import { toLocalDateTimeValue } from '../utils/jobActivity'

interface QuickFollowUpDialogProps {
  item: JobAttentionItem
  isSaving: boolean
  error: string | null
  onClose: () => void
  onSubmit: (input: CreateJobActivityInput) => void
  onViewJob: (jobId: number) => void
}

export function QuickFollowUpDialog({
  item,
  isSaving,
  error,
  onClose,
  onSubmit,
  onViewJob,
}: QuickFollowUpDialogProps) {
  const titleRef = useRef<HTMLInputElement>(null)
  const [title, setTitle] = useState('Recruiter follow-up')
  const [occurredAt, setOccurredAt] = useState(() => toLocalDateTimeValue())
  const [contact, setContact] = useState('')
  const [details, setDetails] = useState('')

  useEffect(() => {
    titleRef.current?.focus()
  }, [])

  useEffect(() => {
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'

    return () => {
      document.body.style.overflow = previousOverflow
    }
  }, [])

  useEffect(() => {
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape' && !isSaving) onClose()
    }

    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [isSaving, onClose])

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!title.trim()) return

    onSubmit({
      type: 'FOLLOW_UP',
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
        className="quick-follow-up-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="quick-follow-up-heading"
      >
        <header className="quick-follow-up-dialog__heading">
          <div>
            <p>Quick action</p>
            <h2 id="quick-follow-up-heading">Add follow-up</h2>
            <span>{item.jobTitle} - {item.company}</span>
          </div>
          <button
            type="button"
            aria-label="Close follow-up form"
            disabled={isSaving}
            onClick={onClose}
          >
            x
          </button>
        </header>

        <form className="quick-follow-up-form" onSubmit={handleSubmit}>
          <label>
            Title
            <input
              ref={titleRef}
              type="text"
              maxLength={255}
              required
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
                value={occurredAt}
                onChange={(event) => setOccurredAt(event.target.value)}
              />
            </label>
            <label>
              Contact <span>optional</span>
              <input
                type="text"
                maxLength={255}
                placeholder="Recruiter name"
                value={contact}
                onChange={(event) => setContact(event.target.value)}
              />
            </label>
          </div>

          <label>
            Notes <span>optional</span>
            <textarea
              rows={3}
              maxLength={5000}
              placeholder="What will you ask or share?"
              value={details}
              onChange={(event) => setDetails(event.target.value)}
            />
          </label>

          {error && <p className="quick-follow-up-form__error" role="alert">{error}</p>}

          <footer className="quick-follow-up-form__actions">
            <button
              className="button button--secondary"
              type="button"
              disabled={isSaving}
              onClick={() => onViewJob(item.jobId)}
            >
              View job
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
              <button
                className="button button--primary"
                type="submit"
                disabled={isSaving || !title.trim()}
              >
                {isSaving ? 'Saving...' : 'Save follow-up'}
              </button>
            </div>
          </footer>
        </form>
      </section>
    </div>
  )
}
