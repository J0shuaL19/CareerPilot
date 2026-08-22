import { useState } from 'react'
import type { Job, JobStatus } from '../types/job'
import { formatDate } from '../utils/formatDate'
import { jobStatusOptions } from '../utils/jobStatus'
import { StatusBadge } from './StatusBadge'

interface JobCardProps {
  job: Job
  isUpdating: boolean
  isDeleting: boolean
  onStatusChange: (jobId: number, status: JobStatus) => Promise<void>
  onEdit: (jobId: number) => void
  onDelete: (job: Job) => Promise<void>
}

export function JobCard({
  job,
  isUpdating,
  isDeleting,
  onStatusChange,
  onEdit,
  onDelete,
}: JobCardProps) {
  const [isConfirmingDelete, setIsConfirmingDelete] = useState(false)

  return (
    <article className="job-card">
      <div className="job-card__company-mark" aria-hidden="true">
        {job.company.charAt(0).toUpperCase()}
      </div>

      <div className="job-card__content">
        <div className="job-card__heading">
          <div>
            <p className="job-card__company">{job.company}</p>
            <h2>{job.title}</h2>
          </div>
          <StatusBadge status={job.status} />
        </div>

        <p className="job-card__description">{job.description}</p>

        <div className="job-card__status-control">
          <label htmlFor={`job-status-${job.id}`}>Pipeline stage</label>
          <div className="job-card__status-select">
            <select
              id={`job-status-${job.id}`}
              value={job.status}
              disabled={isUpdating || isDeleting}
              onChange={(event) => void onStatusChange(job.id, event.target.value as JobStatus)}
            >
              {jobStatusOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
            {isUpdating && <span role="status">Updating…</span>}
          </div>
        </div>

        {isConfirmingDelete && (
          <div className="job-card__delete-confirmation" role="alert">
            <div>
              <strong>Delete this job?</strong>
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
                onClick={() => void onDelete(job)}
              >
                {isDeleting ? 'Deleting…' : 'Delete job'}
              </button>
            </div>
          </div>
        )}

        <div className="job-card__footer">
          <time dateTime={job.createdAt}>Added {formatDate(job.createdAt)}</time>
          <div className="job-card__footer-actions">
            {job.jobUrl && (
              <a href={job.jobUrl} target="_blank" rel="noreferrer">
                View posting <span aria-hidden="true">↗</span>
              </a>
            )}
            <button type="button" disabled={isDeleting} onClick={() => onEdit(job.id)}>
              Edit
            </button>
            <button
              className="job-card__delete-button"
              type="button"
              disabled={isDeleting || isConfirmingDelete}
              onClick={() => setIsConfirmingDelete(true)}
            >
              Delete
            </button>
          </div>
        </div>
      </div>
    </article>
  )
}
