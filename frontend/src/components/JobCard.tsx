import type { Job, JobStatus } from '../types/job'
import { formatDate } from '../utils/formatDate'
import { jobStatusOptions } from '../utils/jobStatus'
import { StatusBadge } from './StatusBadge'

interface JobCardProps {
  job: Job
  isUpdating: boolean
  onStatusChange: (jobId: number, status: JobStatus) => Promise<void>
}

export function JobCard({ job, isUpdating, onStatusChange }: JobCardProps) {
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
              disabled={isUpdating}
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

        <div className="job-card__footer">
          <time dateTime={job.createdAt}>Added {formatDate(job.createdAt)}</time>
          {job.jobUrl && (
            <a href={job.jobUrl} target="_blank" rel="noreferrer">
              View posting <span aria-hidden="true">↗</span>
            </a>
          )}
        </div>
      </div>
    </article>
  )
}
