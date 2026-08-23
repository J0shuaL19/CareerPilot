import { useState } from 'react'
import type { Resume } from '../types/resume'
import { formatDate } from '../utils/formatDate'

interface ResumeCardProps {
  resume: Resume
  isDeleting: boolean
  onView: (resumeId: number) => void
  onEdit: (resumeId: number) => void
  onDelete: (resume: Resume) => Promise<void>
}

export function ResumeCard({ resume, isDeleting, onView, onEdit, onDelete }: ResumeCardProps) {
  const [isConfirmingDelete, setIsConfirmingDelete] = useState(false)

  return (
    <article className="resume-card">
      <div className="resume-card__icon" aria-hidden="true">
        <span>CV</span>
      </div>

      <div className="resume-card__content">
        <div className="resume-card__heading">
          <div>
            <p className="resume-card__label">Saved resume</p>
            <h2>{resume.name}</h2>
          </div>
          <time dateTime={resume.createdAt}>{formatDate(resume.createdAt)}</time>
        </div>

        <p className="resume-card__preview">{resume.content}</p>

        {isConfirmingDelete && (
          <div className="resume-card__delete-confirmation" role="alert">
            <div>
              <strong>Delete this resume?</strong>
              <p>Its saved match analyses will also be permanently deleted.</p>
            </div>
            <div>
              <button
                className="button button--secondary button--compact"
                type="button"
                disabled={isDeleting}
                onClick={() => setIsConfirmingDelete(false)}
              >
                Cancel
              </button>
              <button
                className="button button--danger button--compact"
                type="button"
                disabled={isDeleting}
                onClick={() => void onDelete(resume)}
              >
                {isDeleting ? 'Deleting…' : 'Delete resume'}
              </button>
            </div>
          </div>
        )}

        <div className="resume-card__actions">
          <button type="button" disabled={isDeleting} onClick={() => onView(resume.id)}>
            View
          </button>
          <button type="button" disabled={isDeleting} onClick={() => onEdit(resume.id)}>
            Edit
          </button>
          <button
            className="resume-card__delete-button"
            type="button"
            disabled={isDeleting || isConfirmingDelete}
            onClick={() => setIsConfirmingDelete(true)}
          >
            Delete
          </button>
        </div>
      </div>
    </article>
  )
}
