import { useState } from 'react'
import { JobForm } from '../components/JobForm'
import { ApiError } from '../services/apiClient'
import { createJob } from '../services/jobApi'
import type { CreateJobInput, Job } from '../types/job'
import { getErrorMessage } from '../utils/errors'

interface AddJobPageProps {
  onCancel: () => void
  onCreated: (job: Job) => void
}

export function AddJobPage({ onCancel, onCreated }: AddJobPageProps) {
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [formError, setFormError] = useState<string | null>(null)

  async function handleSubmit(input: CreateJobInput) {
    setIsSubmitting(true)
    setFieldErrors({})
    setFormError(null)

    try {
      const job = await createJob(input)
      onCreated(job)
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
        <h1>Add a job</h1>
        <p>Save the role now. Resume matching and AI analysis will build on this record.</p>
      </header>

      <section className="panel" aria-label="Add job form">
        <JobForm
          fieldErrors={fieldErrors}
          formError={formError}
          isSubmitting={isSubmitting}
          onSubmit={handleSubmit}
          onCancel={onCancel}
        />
      </section>
    </div>
  )
}
