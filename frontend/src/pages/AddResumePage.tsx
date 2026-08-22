import { useState } from 'react'
import { ResumeForm } from '../components/ResumeForm'
import { ApiError } from '../services/apiClient'
import { createResume } from '../services/resumeApi'
import type { CreateResumeInput, Resume } from '../types/resume'
import { getErrorMessage } from '../utils/errors'

interface AddResumePageProps {
  onCancel: () => void
  onCreated: (resume: Resume) => void
}

export function AddResumePage({ onCancel, onCreated }: AddResumePageProps) {
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [formError, setFormError] = useState<string | null>(null)

  async function handleSubmit(input: CreateResumeInput) {
    setIsSubmitting(true)
    setFieldErrors({})
    setFormError(null)

    try {
      const resume = await createResume(input)
      onCreated(resume)
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
        <h1>Add a resume</h1>
        <p>Save a plain-text resume now so it can be matched against job descriptions later.</p>
      </header>

      <section className="panel" aria-label="Add resume form">
        <ResumeForm
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
