import { useState, type FormEvent } from 'react'
import type {
  CreateJobActivityInput,
  JobActivity,
  JobActivityType,
} from '../types/jobActivity'
import { jobActivityTypeOptions, toLocalDateTimeValue } from '../utils/jobActivity'

interface ActivityFormValues {
  type: JobActivityType
  title: string
  details: string
  contact: string
  occurredAt: string
}

interface JobActivityFormProps {
  fieldErrors?: Record<string, string>
  formError?: string | null
  isSubmitting: boolean
  activity?: JobActivity
  onSubmit: (input: CreateJobActivityInput) => Promise<boolean>
  onCancelEdit?: () => void
}

function initialValues(activity?: JobActivity): ActivityFormValues {
  if (activity) {
    return {
      type: activity.type,
      title: activity.title,
      details: activity.details ?? '',
      contact: activity.contact ?? '',
      occurredAt: toLocalDateTimeValue(new Date(activity.occurredAt)),
    }
  }

  return {
    type: 'NOTE',
    title: '',
    details: '',
    contact: '',
    occurredAt: toLocalDateTimeValue(),
  }
}

export function JobActivityForm({
  fieldErrors = {},
  formError,
  isSubmitting,
  activity,
  onSubmit,
  onCancelEdit,
}: JobActivityFormProps) {
  const [values, setValues] = useState<ActivityFormValues>(() => initialValues(activity))
  const isEditing = Boolean(activity)

  function updateField<Field extends keyof ActivityFormValues>(
    field: Field,
    value: ActivityFormValues[Field],
  ) {
    setValues((current) => ({ ...current, [field]: value }))
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const didSave = await onSubmit({
      ...values,
      occurredAt: new Date(values.occurredAt).toISOString(),
    })
    if (didSave && !isEditing) {
      setValues(initialValues())
    }
  }

  return (
    <form className="activity-form" onSubmit={handleSubmit}>
      <div className="activity-form__heading">
        <div>
          <p>{isEditing ? 'Editing timeline entry' : 'New timeline entry'}</p>
          <h2>{isEditing ? 'Update activity' : 'Record an activity'}</h2>
        </div>
        <span aria-hidden="true">{isEditing ? '✎' : '＋'}</span>
      </div>

      {formError && (
        <div className="alert alert--error" role="alert">
          {formError}
        </div>
      )}

      <div className="form-field">
        <label htmlFor="activity-type">Activity type</label>
        <select
          id="activity-type"
          value={values.type}
          onChange={(event) => updateField('type', event.target.value as JobActivityType)}
          aria-invalid={Boolean(fieldErrors.type)}
        >
          {jobActivityTypeOptions.map((option) => (
            <option key={option.value} value={option.value}>{option.label}</option>
          ))}
        </select>
        {fieldErrors.type && <p className="form-field__error">{fieldErrors.type}</p>}
      </div>

      <div className="form-field">
        <label htmlFor="activity-title">Title</label>
        <input
          id="activity-title"
          value={values.title}
          onChange={(event) => updateField('title', event.target.value)}
          placeholder="e.g. Recruiter phone screen"
          maxLength={255}
          required
          aria-invalid={Boolean(fieldErrors.title)}
        />
        {fieldErrors.title && <p className="form-field__error">{fieldErrors.title}</p>}
      </div>

      <div className="form-field">
        <label htmlFor="activity-time">Date and time</label>
        <input
          id="activity-time"
          type="datetime-local"
          value={values.occurredAt}
          onChange={(event) => updateField('occurredAt', event.target.value)}
          required
          aria-invalid={Boolean(fieldErrors.occurredAt)}
        />
        {fieldErrors.occurredAt && <p className="form-field__error">{fieldErrors.occurredAt}</p>}
      </div>

      <div className="form-field">
        <label htmlFor="activity-contact">
          Contact <span>Optional</span>
        </label>
        <input
          id="activity-contact"
          value={values.contact}
          onChange={(event) => updateField('contact', event.target.value)}
          placeholder="e.g. Alex Chen, recruiter"
          maxLength={255}
          aria-invalid={Boolean(fieldErrors.contact)}
        />
        {fieldErrors.contact && <p className="form-field__error">{fieldErrors.contact}</p>}
      </div>

      <div className="form-field">
        <label htmlFor="activity-details">
          Details <span>Optional</span>
        </label>
        <textarea
          id="activity-details"
          value={values.details}
          onChange={(event) => updateField('details', event.target.value)}
          placeholder="Questions, outcomes, or next steps..."
          rows={5}
          maxLength={5000}
          aria-invalid={Boolean(fieldErrors.details)}
        />
        {fieldErrors.details && <p className="form-field__error">{fieldErrors.details}</p>}
      </div>

      {isEditing ? (
        <div className="activity-form__actions">
          <button
            className="button button--secondary"
            type="button"
            disabled={isSubmitting}
            onClick={onCancelEdit}
          >
            Cancel
          </button>
          <button className="button button--primary" type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Saving changes…' : 'Save changes'}
          </button>
        </div>
      ) : (
        <button className="button button--primary" type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Adding activity…' : 'Add to timeline'}
        </button>
      )}
    </form>
  )
}
