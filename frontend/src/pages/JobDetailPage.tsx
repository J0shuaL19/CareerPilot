import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { JobActivityForm } from '../components/JobActivityForm'
import { JobTimeline } from '../components/JobTimeline'
import { StatusBadge } from '../components/StatusBadge'
import { ApiError } from '../services/apiClient'
import {
  createJobActivity,
  deleteJobActivity,
  downloadJobActivityCalendar,
  getJobActivities,
  reopenJobActivity,
  updateJobActivity,
} from '../services/jobActivityApi'
import { getJob } from '../services/jobApi'
import type { Job } from '../types/job'
import type { CreateJobActivityInput, JobActivity } from '../types/jobActivity'
import { getErrorMessage, isAbortError } from '../utils/errors'
import { getJobStatusConfig } from '../utils/jobStatus'
import { NotFoundPage } from './NotFoundPage'

export function JobDetailPage() {
  const jobId = Number(useParams().id)
  const hasValidId = Number.isSafeInteger(jobId) && jobId > 0
  const [job, setJob] = useState<Job | null>(null)
  const [activities, setActivities] = useState<JobActivity[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [actionNotice, setActionNotice] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [deletingActivityId, setDeletingActivityId] = useState<number | null>(null)
  const [editingActivity, setEditingActivity] = useState<JobActivity | null>(null)
  const [exportingActivityId, setExportingActivityId] = useState<number | null>(null)
  const [reopeningActivityId, setReopeningActivityId] = useState<number | null>(null)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    if (!hasValidId) {
      return
    }

    const controller = new AbortController()

    async function loadPage() {
      setIsLoading(true)
      setLoadError(null)

      try {
        const [loadedJob, loadedActivities] = await Promise.all([
          getJob(jobId, controller.signal),
          getJobActivities(jobId, controller.signal),
        ])
        setJob(loadedJob)
        setActivities(loadedActivities)
      } catch (error) {
        if (!isAbortError(error)) {
          setLoadError(getErrorMessage(error))
        }
      } finally {
        if (!controller.signal.aborted) {
          setIsLoading(false)
        }
      }
    }

    void loadPage()
    return () => controller.abort()
  }, [hasValidId, jobId, reloadKey])

  if (!hasValidId) {
    return <NotFoundPage />
  }

  async function handleSaveActivity(input: CreateJobActivityInput) {
    setIsSubmitting(true)
    setFieldErrors({})
    setFormError(null)
    setActionNotice(null)
    setActionError(null)

    try {
      if (editingActivity) {
        const updatedActivity = await updateJobActivity(jobId, editingActivity.id, input)
        setActivities((current) => current
          .map((activity) => activity.id === updatedActivity.id ? updatedActivity : activity)
          .sort(compareActivitiesNewestFirst))
        setEditingActivity(null)
        setActionNotice(`${updatedActivity.title} was updated.`)
      } else {
        const activity = await createJobActivity(jobId, input)
        setActivities((current) => [activity, ...current].sort(compareActivitiesNewestFirst))
        setActionNotice(`${activity.title} was added to the timeline.`)
      }
      return true
    } catch (error) {
      if (error instanceof ApiError) {
        setFieldErrors(error.fieldErrors)
      }
      setFormError(getErrorMessage(error))
      return false
    } finally {
      setIsSubmitting(false)
    }
  }

  async function handleDeleteActivity(activity: JobActivity) {
    setDeletingActivityId(activity.id)
    setActionNotice(null)
    setActionError(null)

    try {
      await deleteJobActivity(jobId, activity.id)
      setActivities((current) => current.filter((item) => item.id !== activity.id))
      if (editingActivity?.id === activity.id) {
        setEditingActivity(null)
      }
      setActionNotice(`${activity.title} was removed from the timeline.`)
    } catch (error) {
      setActionError(getErrorMessage(error))
    } finally {
      setDeletingActivityId(null)
    }
  }

  function handleEditActivity(activity: JobActivity) {
    setEditingActivity(activity)
    setFieldErrors({})
    setFormError(null)
    setActionNotice(null)
    setActionError(null)
  }

  async function handleReopenActivity(activity: JobActivity) {
    setReopeningActivityId(activity.id)
    setActionNotice(null)
    setActionError(null)

    try {
      const result = await reopenJobActivity(jobId, activity.id)
      setActivities((current) => current
        .map((item) => item.id === result.activity.id ? result.activity : item)
        .sort(compareActivitiesNewestFirst))
      setJob((current) => current ? { ...current, status: result.jobStatus } : current)
      const statusDetail = result.jobStatusRestored
        ? ' Job stage restored to ' + getJobStatusConfig(result.jobStatus).label + '.'
        : ''
      setActionNotice(activity.title + ' was reopened.' + statusDetail)
    } catch (error) {
      setActionError(getErrorMessage(error))
    } finally {
      setReopeningActivityId(null)
    }
  }

  async function handleExportCalendar(activity: JobActivity) {
    setExportingActivityId(activity.id)
    setActionNotice(null)
    setActionError(null)

    try {
      const calendar = await downloadJobActivityCalendar(jobId, activity.id)
      const downloadUrl = URL.createObjectURL(calendar)
      const link = document.createElement('a')
      link.href = downloadUrl
      link.download = `careerpilot-activity-${activity.id}.ics`
      document.body.appendChild(link)
      link.click()
      link.remove()
      URL.revokeObjectURL(downloadUrl)
      setActionNotice(`${activity.title} calendar file downloaded.`)
    } catch (error) {
      setActionError(getErrorMessage(error))
    } finally {
      setExportingActivityId(null)
    }
  }

  return (
    <div className="page job-detail-page">
      {isLoading && (
        <div className="state-card" role="status">
          <div className="spinner" aria-hidden="true" />
          <h2>Loading job timeline</h2>
          <p>Bringing together the opportunity and its activity history.</p>
        </div>
      )}

      {!isLoading && loadError && (
        <div className="state-card state-card--error" role="alert">
          <div className="state-card__icon" aria-hidden="true">!</div>
          <h2>We couldn’t load this job</h2>
          <p>{loadError}</p>
          <button
            className="button button--secondary"
            type="button"
            onClick={() => setReloadKey((key) => key + 1)}
          >
            Try again
          </button>
        </div>
      )}

      {!isLoading && !loadError && job && (
        <>
          <Link className="job-detail__back" to="/jobs">← Back to jobs</Link>

          <header className="job-detail__hero">
            <div className="job-detail__company-mark" aria-hidden="true">
              {job.company.charAt(0).toUpperCase()}
            </div>
            <div className="job-detail__heading">
              <p>{job.company}</p>
              <h1>{job.title}</h1>
              <StatusBadge status={job.status} />
            </div>
            <div className="job-detail__actions">
              {job.jobUrl && (
                <a className="button button--secondary" href={job.jobUrl} target="_blank" rel="noreferrer">
                  View posting <span aria-hidden="true">↗</span>
                </a>
              )}
              <Link className="button button--primary" to={`/jobs/${job.id}/edit`}>
                Edit job
              </Link>
            </div>
            <p className="job-detail__description">{job.description}</p>
          </header>

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

          <div className="job-detail__content">
            <aside className="activity-form-panel">
              <JobActivityForm
                key={editingActivity ? `edit-${editingActivity.id}` : 'create'}
                activity={editingActivity ?? undefined}
                fieldErrors={fieldErrors}
                formError={formError}
                isSubmitting={isSubmitting}
                onSubmit={handleSaveActivity}
                onCancelEdit={() => {
                  setEditingActivity(null)
                  setFieldErrors({})
                  setFormError(null)
                }}
              />
            </aside>

            <div className="timeline-panel">
              <div className="timeline-panel__heading">
                <div>
                  <p>Activity history</p>
                  <h2>Timeline</h2>
                </div>
                <span>{activities.length} {activities.length === 1 ? 'entry' : 'entries'}</span>
              </div>
              <JobTimeline
                activities={activities}
                deletingActivityId={deletingActivityId}
                editingActivityId={editingActivity?.id ?? null}
                exportingActivityId={exportingActivityId}
                reopeningActivityId={reopeningActivityId}
                onEdit={handleEditActivity}
                onExportCalendar={handleExportCalendar}
                onReopen={handleReopenActivity}
                onDelete={handleDeleteActivity}
              />
            </div>
          </div>
        </>
      )}
    </div>
  )
}

function compareActivitiesNewestFirst(left: JobActivity, right: JobActivity) {
  return right.occurredAt.localeCompare(left.occurredAt)
    || right.createdAt.localeCompare(left.createdAt)
}
