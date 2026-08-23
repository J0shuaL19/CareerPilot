import { useState, type FormEvent } from 'react'
import type { CreateResumeInput } from '../types/resume'

interface ResumeFormProps {
  initialValues?: CreateResumeInput
  fieldErrors?: Record<string, string>
  formError?: string | null
  isSubmitting: boolean
  submitLabel?: string
  submittingLabel?: string
  onSubmit: (input: CreateResumeInput) => Promise<void>
  onCancel: () => void
}

const emptyValues: CreateResumeInput = {
  name: '',
  content: '',
}

export function ResumeForm({
  initialValues = emptyValues,
  fieldErrors = {},
  formError,
  isSubmitting,
  submitLabel = 'Save resume',
  submittingLabel = 'Saving resume…',
  onSubmit,
  onCancel,
}: ResumeFormProps) {
  const [values, setValues] = useState<CreateResumeInput>({ ...initialValues })

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    await onSubmit(values)
  }

  function updateField(field: keyof CreateResumeInput, value: string) {
    setValues((current) => ({ ...current, [field]: value }))
  }

  return (
    <form className="resume-form" onSubmit={handleSubmit} noValidate>
      {formError && (
        <div className="alert alert--error" role="alert">
          {formError}
        </div>
      )}

      <div className="form-field">
        <label htmlFor="resume-name">Resume name</label>
        <input
          id="resume-name"
          name="name"
          value={values.name}
          onChange={(event) => updateField('name', event.target.value)}
          placeholder="e.g. Backend Engineer Resume"
          maxLength={255}
          required
          aria-invalid={Boolean(fieldErrors.name)}
          aria-describedby={fieldErrors.name ? 'resume-name-error' : undefined}
        />
        <p className="form-field__hint">
          Use a clear name so you can identify this version later.
        </p>
        {fieldErrors.name && (
          <p className="form-field__error" id="resume-name-error">
            {fieldErrors.name}
          </p>
        )}
      </div>

      <div className="form-field">
        <label htmlFor="resume-content">Resume content</label>
        <textarea
          id="resume-content"
          name="content"
          value={values.content}
          onChange={(event) => updateField('content', event.target.value)}
          placeholder="Paste the complete plain-text content of your resume here..."
          rows={18}
          required
          aria-invalid={Boolean(fieldErrors.content)}
          aria-describedby={fieldErrors.content ? 'resume-content-error' : 'resume-content-hint'}
        />
        <p className="form-field__hint" id="resume-content-hint">
          Include experience, skills, projects, and education for future job matching.
        </p>
        {fieldErrors.content && (
          <p className="form-field__error" id="resume-content-error">
            {fieldErrors.content}
          </p>
        )}
      </div>

      <div className="form-actions">
        <button className="button button--secondary" type="button" onClick={onCancel}>
          Cancel
        </button>
        <button className="button button--primary" type="submit" disabled={isSubmitting}>
          {isSubmitting ? submittingLabel : submitLabel}
        </button>
      </div>
    </form>
  )
}
