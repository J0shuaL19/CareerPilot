import type { Job, JobStatus } from '../types/job'
import { jobStatusOptions } from '../utils/jobStatus'

export type JobStatusFilterValue = JobStatus | 'ALL'

interface JobStatusFilterProps {
  jobs: Job[]
  selectedStatus: JobStatusFilterValue
  shownCount: number
  onChange: (status: JobStatusFilterValue) => void
}

export function JobStatusFilter({
  jobs,
  selectedStatus,
  shownCount,
  onChange,
}: JobStatusFilterProps) {
  const options: ReadonlyArray<{ value: JobStatusFilterValue; label: string }> = [
    { value: 'ALL', label: 'All' },
    ...jobStatusOptions,
  ]

  function countJobs(status: JobStatusFilterValue) {
    return status === 'ALL'
      ? jobs.length
      : jobs.filter((job) => job.status === status).length
  }

  return (
    <section className="job-filter" aria-labelledby="job-filter-label">
      <div className="job-filter__heading">
        <h2 id="job-filter-label">Filter by stage</h2>
        <span>{shownCount} shown</span>
      </div>
      <div className="job-filter__options">
        {options.map((option) => (
          <button
            className="job-filter__button"
            type="button"
            aria-pressed={selectedStatus === option.value}
            key={option.value}
            onClick={() => onChange(option.value)}
          >
            <span>{option.label}</span>
            <strong>{countJobs(option.value)}</strong>
          </button>
        ))}
      </div>
    </section>
  )
}
