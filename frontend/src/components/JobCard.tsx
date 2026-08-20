import type { Job } from '../types/job'
import { formatDate } from '../utils/formatDate'
import { StatusBadge } from './StatusBadge'

interface JobCardProps {
  job: Job
}

export function JobCard({ job }: JobCardProps) {
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
