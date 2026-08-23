import { useEffect, useState } from 'react'
import { JobCard } from '../components/JobCard'
import {
  JobSearchSortControls,
  type JobSortOrder,
} from '../components/JobSearchSortControls'
import {
  JobStatusFilter,
  type JobStatusFilterValue,
} from '../components/JobStatusFilter'
import { PipelineSummary } from '../components/PipelineSummary'
import { UpcomingActivities } from '../components/UpcomingActivities'
import { getUpcomingJobActivities } from '../services/jobActivityApi'
import { deleteJob, getJobs, updateJobStatus } from '../services/jobApi'
import type { Job, JobStatus } from '../types/job'
import type { UpcomingJobActivity } from '../types/jobActivity'
import { getErrorMessage, isAbortError } from '../utils/errors'
import { getJobStatusConfig } from '../utils/jobStatus'

interface JobsPageProps {
  notice?: string
  onAddJob: () => void
  onViewJob: (jobId: number) => void
  onEditJob: (jobId: number) => void
}

export function JobsPage({ notice, onAddJob, onViewJob, onEditJob }: JobsPageProps) {
  const [jobs, setJobs] = useState<Job[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [actionNotice, setActionNotice] = useState<string | null>(null)
  const [showRouteNotice, setShowRouteNotice] = useState(Boolean(notice))
  const [updatingJobId, setUpdatingJobId] = useState<number | null>(null)
  const [deletingJobId, setDeletingJobId] = useState<number | null>(null)
  const [upcomingActivities, setUpcomingActivities] = useState<UpcomingJobActivity[]>([])
  const [isUpcomingLoading, setIsUpcomingLoading] = useState(true)
  const [upcomingError, setUpcomingError] = useState<string | null>(null)
  const [selectedStatus, setSelectedStatus] = useState<JobStatusFilterValue>('ALL')
  const [query, setQuery] = useState('')
  const [sortOrder, setSortOrder] = useState<JobSortOrder>('NEWEST')
  const [reloadKey, setReloadKey] = useState(0)
  const [upcomingReloadKey, setUpcomingReloadKey] = useState(0)

  const normalizedQuery = query.trim().toLocaleLowerCase()
  const filteredJobs = jobs
    .filter((job) => selectedStatus === 'ALL' || job.status === selectedStatus)
    .filter((job) => normalizedQuery === '' || [job.company, job.title, job.description]
      .some((value) => value.toLocaleLowerCase().includes(normalizedQuery)))
    .sort((left, right) => {
      const dateComparison = left.createdAt.localeCompare(right.createdAt)
      return sortOrder === 'OLDEST' ? dateComparison : -dateComparison
    })
  const hasActiveFilters = normalizedQuery !== ''
    || sortOrder !== 'NEWEST'
    || selectedStatus !== 'ALL'

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

  useEffect(() => {
    const controller = new AbortController()

    async function loadUpcomingActivities() {
      setIsUpcomingLoading(true)
      setUpcomingError(null)

      try {
        setUpcomingActivities(await getUpcomingJobActivities(controller.signal))
      } catch (loadError) {
        if (!isAbortError(loadError)) {
          setUpcomingError(getErrorMessage(loadError))
        }
      } finally {
        if (!controller.signal.aborted) {
          setIsUpcomingLoading(false)
        }
      }
    }

    void loadUpcomingActivities()
    return () => controller.abort()
  }, [upcomingReloadKey])

  async function handleStatusChange(jobId: number, status: JobStatus) {
    setUpdatingJobId(jobId)
    setActionError(null)
    setActionNotice(null)
    setShowRouteNotice(false)

    try {
      const updatedJob = await updateJobStatus(jobId, { status })
      setJobs((currentJobs) => currentJobs.map((job) => (
        job.id === updatedJob.id ? updatedJob : job
      )))
      setActionNotice(
        `${updatedJob.title} moved to ${getJobStatusConfig(updatedJob.status).label}.`,
      )
    } catch (updateError) {
      setActionError(getErrorMessage(updateError))
    } finally {
      setUpdatingJobId(null)
    }
  }

  async function handleDelete(job: Job) {
    setDeletingJobId(job.id)
    setActionError(null)
    setActionNotice(null)
    setShowRouteNotice(false)

    try {
      await deleteJob(job.id)
      setJobs((currentJobs) => currentJobs.filter((currentJob) => currentJob.id !== job.id))
      setUpcomingActivities((current) => current.filter((activity) => activity.jobId !== job.id))
      setActionNotice(`${job.title} at ${job.company} was deleted.`)
    } catch (deleteError) {
      setActionError(getErrorMessage(deleteError))
    } finally {
      setDeletingJobId(null)
    }
  }

  function handleResetFilters() {
    setQuery('')
    setSortOrder('NEWEST')
    setSelectedStatus('ALL')
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

      {showRouteNotice && notice && (
        <div className="alert alert--success" role="status">
          <span aria-hidden="true">✓</span> {notice}
        </div>
      )}

      {actionNotice && (
        <div className="alert alert--success" role="status">
          <span aria-hidden="true">✓</span> {actionNotice}
        </div>
      )}

      {actionError && (
        <div className="alert alert--error job-action-error" role="alert">
          {actionError}
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
        <>
          <PipelineSummary jobs={jobs} />
          <UpcomingActivities
            activities={upcomingActivities}
            isLoading={isUpcomingLoading}
            error={upcomingError}
            onRetry={() => setUpcomingReloadKey((key) => key + 1)}
            onViewJob={onViewJob}
          />
          <JobSearchSortControls
            query={query}
            sortOrder={sortOrder}
            resultCount={filteredJobs.length}
            totalCount={jobs.length}
            hasActiveFilters={hasActiveFilters}
            onQueryChange={setQuery}
            onSortOrderChange={setSortOrder}
            onReset={handleResetFilters}
          />
          <JobStatusFilter
            jobs={jobs}
            selectedStatus={selectedStatus}
            shownCount={filteredJobs.length}
            onChange={setSelectedStatus}
          />

          {filteredJobs.length > 0 ? (
            <section className="job-list" aria-label="Filtered jobs">
              {filteredJobs.map((job) => (
                <JobCard
                  key={job.id}
                  job={job}
                  isUpdating={updatingJobId === job.id}
                  isDeleting={deletingJobId === job.id}
                  onStatusChange={handleStatusChange}
                  onView={onViewJob}
                  onEdit={onEditJob}
                  onDelete={handleDelete}
                />
              ))}
            </section>
          ) : (
            <div className="state-card state-card--compact">
              <div className="state-card__icon" aria-hidden="true">⌕</div>
              <h2>{normalizedQuery === '' ? 'No jobs at this stage' : 'No jobs match'}</h2>
              <p>
                {normalizedQuery === ''
                  ? 'Choose another pipeline stage or return to your full job list.'
                  : 'Try a different company, role, description keyword, or pipeline stage.'}
              </p>
              <button
                className="button button--secondary"
                type="button"
                onClick={handleResetFilters}
              >
                Reset filters
              </button>
            </div>
          )}
        </>
      )}
    </div>
  )
}
