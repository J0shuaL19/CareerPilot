import { useEffect, useState } from 'react'
import { JobCard } from '../components/JobCard'
import { getJobs, updateJobStatus } from '../services/jobApi'
import type { Job, JobStatus } from '../types/job'
import { getErrorMessage, isAbortError } from '../utils/errors'
import { getJobStatusConfig } from '../utils/jobStatus'

interface JobsPageProps {
  notice?: string
  onAddJob: () => void
}

export function JobsPage({ notice, onAddJob }: JobsPageProps) {
  const [jobs, setJobs] = useState<Job[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [statusError, setStatusError] = useState<string | null>(null)
  const [statusNotice, setStatusNotice] = useState<string | null>(null)
  const [updatingJobId, setUpdatingJobId] = useState<number | null>(null)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    const controller = new AbortController()

    async function loadJobs() {
      setIsLoading(true)
      setError(null)

      try {
        setJobs(await getJobs(controller.signal))
      } catch (loadError) {
        if (!isAbortError(loadError)) {
          setError(getErrorMessage(loadError))
        }
      } finally {
        if (!controller.signal.aborted) {
          setIsLoading(false)
        }
      }
    }

    void loadJobs()
    return () => controller.abort()
  }, [reloadKey])

  async function handleStatusChange(jobId: number, status: JobStatus) {
    setUpdatingJobId(jobId)
    setStatusError(null)
    setStatusNotice(null)

    try {
      const updatedJob = await updateJobStatus(jobId, { status })
      setJobs((currentJobs) => currentJobs.map((job) => (
        job.id === updatedJob.id ? updatedJob : job
      )))
      setStatusNotice(
        `${updatedJob.title} moved to ${getJobStatusConfig(updatedJob.status).label}.`,
      )
    } catch (updateError) {
      setStatusError(getErrorMessage(updateError))
    } finally {
      setUpdatingJobId(null)
    }
  }

  return (
    <div className="page">
      <header className="page-header page-header--row">
        <div>
          <p className="page-header__eyebrow">Job tracker</p>
          <h1>Your jobs</h1>
          <p>
            {jobs.length === 0
              ? 'Build a focused pipeline, one opportunity at a time.'
              : `${jobs.length} ${jobs.length === 1 ? 'opportunity' : 'opportunities'} in your pipeline.`}
          </p>
        </div>
        <button className="button button--primary" type="button" onClick={onAddJob}>
          <span aria-hidden="true">＋</span> Add job
        </button>
      </header>

      {notice && (
        <div className="alert alert--success" role="status">
          <span aria-hidden="true">✓</span> {notice}
        </div>
      )}

      {statusNotice && (
        <div className="alert alert--success" role="status">
          <span aria-hidden="true">✓</span> {statusNotice}
        </div>
      )}

      {statusError && (
        <div className="alert alert--error job-status-error" role="alert">
          {statusError}
        </div>
      )}

      {isLoading && (
        <div className="state-card" role="status">
          <div className="spinner" aria-hidden="true" />
          <h2>Loading your jobs</h2>
          <p>Checking the latest opportunities in your pipeline.</p>
        </div>
      )}

      {!isLoading && error && (
        <div className="state-card state-card--error" role="alert">
          <div className="state-card__icon" aria-hidden="true">!</div>
          <h2>We couldn’t load your jobs</h2>
          <p>{error}</p>
          <button className="button button--secondary" type="button" onClick={() => setReloadKey((key) => key + 1)}>
            Try again
          </button>
        </div>
      )}

      {!isLoading && !error && jobs.length === 0 && (
        <div className="state-card">
          <div className="state-card__icon" aria-hidden="true">◎</div>
          <h2>No jobs saved yet</h2>
          <p>Add your first opportunity to start building your job-search pipeline.</p>
          <button className="button button--primary" type="button" onClick={onAddJob}>
            Add your first job
          </button>
        </div>
      )}

      {!isLoading && !error && jobs.length > 0 && (
        <section className="job-list" aria-label="Saved jobs">
          {jobs.map((job) => (
            <JobCard
              key={job.id}
              job={job}
              isUpdating={updatingJobId === job.id}
              onStatusChange={handleStatusChange}
            />
          ))}
        </section>
      )}
    </div>
  )
}
