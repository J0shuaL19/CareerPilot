import { useEffect, useState } from 'react'
import { ResumeForm } from '../components/ResumeForm'
import { ApiError } from '../services/apiClient'
import { getResume, updateResume } from '../services/resumeApi'
import type { Resume, UpdateResumeInput } from '../types/resume'
import { getErrorMessage, isAbortError } from '../utils/errors'

interface EditResumePageProps {
  resumeId: number
  onCancel: () => void
  onUpdated: (resume: Resume) => void
}

export function EditResumePage({ resumeId, onCancel, onUpdated }: EditResumePageProps) {
  const [resume, setResume] = useState<Resume | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    const controller = new AbortController()

    async function loadResume() {
      setIsLoading(true)
      setLoadError(null)

      try {
        setResume(await getResume(resumeId, controller.signal))
      } catch (error) {
        if (!isAbortError(error)) {
          setLoadError(getErrorMessage(error))
        }
      } finally {
        if (!controller.signal.aborted) {
          setIsLoading(false)
        }
      }
    }

    void loadResume()
    return () => controller.abort()
  }, [resumeId, reloadKey])

  async function handleSubmit(input: UpdateResumeInput) {
    setIsSubmitting(true)
    setFieldErrors({})
    setFormError(null)

    try {
      onUpdated(await updateResume(resumeId, input))
    } catch (error) {
      if (error instanceof ApiError) {
        setFieldErrors(error.fieldErrors)
      }
      setFormError(getErrorMessage(error))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="page page--narrow">
      <header className="page-header">
        <p className="page-header__eyebrow">Resume library</p>
        <h1>Edit resume</h1>
        <p>Keep this resume version accurate for future job matching.</p>
      </header>

      {isLoading && (
        <div className="state-card" role="status">
          <div className="spinner" aria-hidden="true" />
          <h2>Loading resume details</h2>
          <p>Preparing this resume for editing.</p>
        </div>
      )}

      {!isLoading && loadError && (
        <div className="state-card state-card--error" role="alert">
          <div className="state-card__icon" aria-hidden="true">!</div>
          <h2>We couldn’t load this resume</h2>
          <p>{loadError}</p>
          <button
            className="button button--secondary"
            type="button"
            onClick={() => setReloadKey((key) => key + 1)}
          >
            Try again
          </button>
        </div>
      )}

      {!isLoading && !loadError && resume && (
        <section className="panel" aria-label="Edit resume form">
          <ResumeForm
            initialValues={{
              name: resume.name,
              content: resume.content,
            }}
            fieldErrors={fieldErrors}
            formError={formError}
            isSubmitting={isSubmitting}
            submitLabel="Save changes"
            submittingLabel="Saving changes…"
            onSubmit={handleSubmit}
            onCancel={onCancel}
          />
        </section>
      )}
    </div>
  )
}
