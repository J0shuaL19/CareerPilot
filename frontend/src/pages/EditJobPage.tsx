import { useEffect, useState } from 'react'
import { JobForm } from '../components/JobForm'
import { ApiError } from '../services/apiClient'
import { getJob, updateJob } from '../services/jobApi'
import type { Job, UpdateJobInput } from '../types/job'
import { getErrorMessage, isAbortError } from '../utils/errors'

interface EditJobPageProps {
  jobId: number
  onCancel: () => void
  onUpdated: (job: Job) => void
}

export function EditJobPage({ jobId, onCancel, onUpdated }: EditJobPageProps) {
  const [job, setJob] = useState<Job | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    const controller = new AbortController()

    async function loadJob() {
      setIsLoading(true)
      setLoadError(null)

      try {
        setJob(await getJob(jobId, controller.signal))
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

    void loadJob()
    return () => controller.abort()
  }, [jobId, reloadKey])

  async function handleSubmit(input: UpdateJobInput) {
    setIsSubmitting(true)
    setFieldErrors({})
    setFormError(null)

    try {
      onUpdated(await updateJob(jobId, input))
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
        <p className="page-header__eyebrow">Job tracker</p>
        <h1>Edit job</h1>
        <p>Keep the opportunity details accurate without changing its pipeline stage.</p>
      </header>

      {isLoading && (
        <div className="state-card" role="status">
          <div className="spinner" aria-hidden="true" />
          <h2>Loading job details</h2>
          <p>Preparing this opportunity for editing.</p>
        </div>
      )}

      {!isLoading && loadError && (
        <div className="state-card state-card--error" role="alert">
          <div className="state-card__icon" aria-hidden="true">!</div>
          <h2>We couldn’t load this job</h2>
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

      {!isLoading && !loadError && job && (
        <section className="panel" aria-label="Edit job form">
          <JobForm
            initialValues={{
              company: job.company,
              title: job.title,
              description: job.description,
              jobUrl: job.jobUrl ?? '',
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
