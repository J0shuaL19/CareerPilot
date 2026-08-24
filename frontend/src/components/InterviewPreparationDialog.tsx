import { type FormEvent, useEffect, useState } from 'react'
import {
  getInterviewPreparation,
  saveInterviewPreparation,
} from '../services/jobActivityApi'
import type {
  InterviewPreparation,
  InterviewPreparationInput,
  ScheduledJobActivity,
} from '../types/jobActivity'
import { getErrorMessage, isAbortError } from '../utils/errors'

interface InterviewPreparationDialogProps {
  activity: ScheduledJobActivity
  onClose: () => void
  onSaved: (preparation: InterviewPreparation) => void
}

type NotesField = 'companyResearch' | 'rolePriorities' | 'starStories' | 'questionsToAsk'
type DoneField =
  | 'companyResearchDone'
  | 'rolePrioritiesDone'
  | 'starStoriesDone'
  | 'questionsToAskDone'

const emptyPreparation: InterviewPreparationInput = {
  companyResearch: '',
  companyResearchDone: false,
  rolePriorities: '',
  rolePrioritiesDone: false,
  starStories: '',
  starStoriesDone: false,
  questionsToAsk: '',
  questionsToAskDone: false,
}

const sections: Array<{
  number: string
  title: string
  prompt: string
  placeholder: string
  notesField: NotesField
  doneField: DoneField
}> = [
  {
    number: '01',
    title: 'Know the company',
    prompt: 'Capture what the company does, recent developments, and why this team interests you.',
    placeholder: 'Business model, product, customers, recent news, people you will meet…',
    notesField: 'companyResearch',
    doneField: 'companyResearchDone',
  },
  {
    number: '02',
    title: 'Read the role closely',
    prompt: 'Identify the role’s priorities and connect each one to evidence from your experience.',
    placeholder: 'Top responsibilities, required skills, likely evaluation areas, your proof points…',
    notesField: 'rolePriorities',
    doneField: 'rolePrioritiesDone',
  },
  {
    number: '03',
    title: 'Rehearse STAR stories',
    prompt: 'Prepare concise examples for impact, collaboration, ambiguity, and a difficult challenge.',
    placeholder: 'Situation, task, actions, measurable result, and what you learned…',
    notesField: 'starStories',
    doneField: 'starStoriesDone',
  },
  {
    number: '04',
    title: 'Questions to ask',
    prompt: 'Write questions that reveal expectations, team dynamics, and what success looks like.',
    placeholder: 'What should this person achieve in 90 days? How does the team make decisions?…',
    notesField: 'questionsToAsk',
    doneField: 'questionsToAskDone',
  },
]

export function InterviewPreparationDialog({
  activity,
  onClose,
  onSaved,
}: InterviewPreparationDialogProps) {
  const [form, setForm] = useState<InterviewPreparationInput>(emptyPreparation)
  const [isLoading, setIsLoading] = useState(true)
  const [isSaving, setIsSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const completedSections = sections.filter((section) => form[section.doneField]).length
  const progressPercent = completedSections * 25

  useEffect(() => {
    const controller = new AbortController()

    async function loadPreparation() {
      setIsLoading(true)
      setError(null)
      try {
        const preparation = await getInterviewPreparation(
          activity.jobId,
          activity.id,
          controller.signal,
        )
        setForm({
          companyResearch: preparation.companyResearch ?? '',
          companyResearchDone: preparation.companyResearchDone,
          rolePriorities: preparation.rolePriorities ?? '',
          rolePrioritiesDone: preparation.rolePrioritiesDone,
          starStories: preparation.starStories ?? '',
          starStoriesDone: preparation.starStoriesDone,
          questionsToAsk: preparation.questionsToAsk ?? '',
          questionsToAskDone: preparation.questionsToAskDone,
        })
      } catch (loadError) {
        if (!isAbortError(loadError)) setError(getErrorMessage(loadError))
      } finally {
        if (!controller.signal.aborted) setIsLoading(false)
      }
    }

    void loadPreparation()
    return () => controller.abort()
  }, [activity.id, activity.jobId])

  useEffect(() => {
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

  function updateNotes(notesField: NotesField, doneField: DoneField, value: string) {
    setForm((current) => ({
      ...current,
      [notesField]: value,
      ...(value.trim() ? {} : { [doneField]: false }),
    }))
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setIsSaving(true)
    setError(null)
    try {
      const saved = await saveInterviewPreparation(activity.jobId, activity.id, form)
      onSaved(saved)
    } catch (saveError) {
      setError(getErrorMessage(saveError))
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <div
      className="quick-follow-up-backdrop"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget && !isSaving) onClose()
      }}
    >
      <section
        className="quick-follow-up-dialog interview-preparation-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="interview-preparation-heading"
      >
        <header className="quick-follow-up-dialog__heading">
          <div>
            <p>Interview workspace</p>
            <h2 id="interview-preparation-heading">Prepare with intention</h2>
            <span>{activity.title} · {activity.jobTitle} at {activity.company}</span>
          </div>
          <button
            type="button"
            aria-label="Close interview preparation"
            disabled={isSaving}
            onClick={onClose}
          >
            ×
          </button>
        </header>

        {isLoading ? (
          <div className="interview-preparation-dialog__state">Loading preparation…</div>
        ) : (
          <form className="interview-preparation-form" onSubmit={handleSubmit}>
            <div className="interview-preparation-progress">
              <div>
                <span>Preparation progress</span>
                <strong>{completedSections} of 4 sections ready</strong>
              </div>
              <span>{progressPercent}%</span>
              <div
                className="interview-preparation-progress__track"
                role="progressbar"
                aria-label="Interview preparation progress"
                aria-valuemin={0}
                aria-valuemax={100}
                aria-valuenow={progressPercent}
              >
                <span style={{ width: progressPercent + '%' }} />
              </div>
            </div>

            <div className="interview-preparation-sections">
              {sections.map((section) => {
                const notes = form[section.notesField] ?? ''
                const completed = form[section.doneField]
                return (
                  <section
                    className={'interview-preparation-section'
                      + (completed ? ' interview-preparation-section--done' : '')}
                    key={section.notesField}
                  >
                    <div className="interview-preparation-section__number">{section.number}</div>
                    <div className="interview-preparation-section__content">
                      <header>
                        <div>
                          <h3>{section.title}</h3>
                          <p>{section.prompt}</p>
                        </div>
                        <label className="interview-preparation-section__check">
                          <input
                            type="checkbox"
                            checked={completed}
                            disabled={!notes.trim() || isSaving}
                            onChange={(event) => setForm((current) => ({
                              ...current,
                              [section.doneField]: event.target.checked,
                            }))}
                          />
                          <span>{completed ? 'Ready' : 'Mark ready'}</span>
                        </label>
                      </header>
                      <textarea
                        rows={4}
                        maxLength={5000}
                        value={notes}
                        disabled={isSaving}
                        placeholder={section.placeholder}
                        aria-label={section.title + ' notes'}
                        onChange={(event) => updateNotes(
                          section.notesField,
                          section.doneField,
                          event.target.value,
                        )}
                      />
                      <span className="interview-preparation-section__count">
                        {notes.length.toLocaleString()} / 5,000
                      </span>
                    </div>
                  </section>
                )
              })}
            </div>

            {error && <p className="quick-follow-up-form__error" role="alert">{error}</p>}

            <footer className="interview-preparation-actions">
              <span>Your notes stay attached to this interview activity.</span>
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
                  {isSaving ? 'Saving preparation…' : 'Save preparation'}
                </button>
              </div>
            </footer>
          </form>
        )}
      </section>
    </div>
  )
}