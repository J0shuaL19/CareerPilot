import { useState, type FormEvent } from 'react'
import type { CreateJobInput } from '../types/job'

interface JobFormProps {
  initialValues?: CreateJobInput
  fieldErrors?: Record<string, string>
  formError?: string | null
  isSubmitting: boolean
  submitLabel?: string
  submittingLabel?: string
  onSubmit: (input: CreateJobInput) => Promise<void>
  onCancel: () => void
}

const emptyValues: CreateJobInput = {
  company: '',
  title: '',
  description: '',
  jobUrl: '',
}

export function JobForm({
  initialValues = emptyValues,
  fieldErrors = {},
  formError,
  isSubmitting,
  submitLabel = 'Save job',
  submittingLabel = 'Saving job…',
  onSubmit,
  onCancel,
}: JobFormProps) {
  const [values, setValues] = useState<CreateJobInput>({ ...initialValues })

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    await onSubmit(values)
  }

  function updateField(field: keyof CreateJobInput, value: string) {
    setValues((current) => ({ ...current, [field]: value }))
  }

  return (
    <form className="job-form" onSubmit={handleSubmit} noValidate>
      {formError && (
        <div className="alert alert--error" role="alert">
          {formError}
        </div>
      )}

      <div className="form-grid">
        <div className="form-field">
          <label htmlFor="company">Company</label>
          <input
            id="company"
            name="company"
            value={values.company}
            onChange={(event) => updateField('company', event.target.value)}
            placeholder="e.g. OpenAI"
            maxLength={255}
            required
            aria-invalid={Boolean(fieldErrors.company)}
            aria-describedby={fieldErrors.company ? 'company-error' : undefined}
          />
          {fieldErrors.company && (
            <p className="form-field__error" id="company-error">
              {fieldErrors.company}
            </p>
          )}
        </div>

        <div className="form-field">
          <label htmlFor="title">Job title</label>
          <input
            id="title"
            name="title"
            value={values.title}
            onChange={(event) => updateField('title', event.target.value)}
            placeholder="e.g. Software Engineer"
            maxLength={255}
            required
            aria-invalid={Boolean(fieldErrors.title)}
            aria-describedby={fieldErrors.title ? 'title-error' : undefined}
          />
          {fieldErrors.title && (
            <p className="form-field__error" id="title-error">
              {fieldErrors.title}
            </p>
          )}
        </div>
      </div>

      <div className="form-field">
        <label htmlFor="jobUrl">
          Job posting URL <span>Optional</span>
        </label>
        <input
          id="jobUrl"
          name="jobUrl"
          type="url"
          value={values.jobUrl}
          onChange={(event) => updateField('jobUrl', event.target.value)}
          placeholder="https://company.com/jobs/..."
          maxLength={2048}
          aria-invalid={Boolean(fieldErrors.jobUrl)}
          aria-describedby={fieldErrors.jobUrl ? 'job-url-error' : undefined}
        />
        {fieldErrors.jobUrl && (
          <p className="form-field__error" id="job-url-error">
            {fieldErrors.jobUrl}
          </p>
        )}
      </div>

      <div className="form-field">
        <label htmlFor="description">Job description</label>
        <textarea
          id="description"
          name="description"
          value={values.description}
          onChange={(event) => updateField('description', event.target.value)}
          placeholder="Paste the complete job description here..."
          rows={12}
          required
          aria-invalid={Boolean(fieldErrors.description)}
          aria-describedby={fieldErrors.description ? 'description-error' : undefined}
        />
        <p className="form-field__hint">
          Include the full description so matching can be added later.
        </p>
        {fieldErrors.description && (
          <p className="form-field__error" id="description-error">
            {fieldErrors.description}
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
