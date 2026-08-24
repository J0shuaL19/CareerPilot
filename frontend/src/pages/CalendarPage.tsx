import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { CalendarActivityDialog } from '../components/CalendarActivityDialog'
import { CalendarActivityDetailsDialog } from '../components/CalendarActivityDetailsDialog'
import {
  CalendarFilters,
  type CalendarActivityStatusFilter,
  type CalendarActivityTypeFilter,
} from '../components/CalendarFilters'
import { CompleteActivityDialog } from '../components/CompleteActivityDialog'
import { RescheduleActivityDialog } from '../components/RescheduleActivityDialog'
import {
  completeJobActivity,
  createJobActivity,
  getCalendarJobActivities,
  reopenJobActivity,
  rescheduleJobActivity,
} from '../services/jobActivityApi'
import { getJobs } from '../services/jobApi'
import type { Job } from '../types/job'
import type {
  CompleteJobActivityInput,
  CreateJobActivityInput,
  ScheduledJobActivity,
} from '../types/jobActivity'
import { getErrorMessage, isAbortError } from '../utils/errors'
import { getJobActivityTypeConfig } from '../utils/jobActivity'
import { getJobStatusConfig } from '../utils/jobStatus'

const monthFormatter = new Intl.DateTimeFormat('en-US', { month: 'long', year: 'numeric' })
const dayHeadingFormatter = new Intl.DateTimeFormat('en-US', {
  weekday: 'long',
  month: 'short',
  day: 'numeric',
})
const timeFormatter = new Intl.DateTimeFormat('en-US', { hour: 'numeric', minute: '2-digit' })
const weekdayLabels = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']

export function CalendarPage() {
  const [visibleMonth, setVisibleMonth] = useState(() => startOfMonth(new Date()))
  const [activities, setActivities] = useState<ScheduledJobActivity[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [jobs, setJobs] = useState<Job[]>([])
  const [isJobsLoading, setIsJobsLoading] = useState(true)
  const [jobsError, setJobsError] = useState<string | null>(null)
  const [searchQuery, setSearchQuery] = useState('')
  const [typeFilter, setTypeFilter] = useState<CalendarActivityTypeFilter>('ALL')
  const [statusFilter, setStatusFilter] = useState<CalendarActivityStatusFilter>('ALL')
  const [jobIdFilter, setJobIdFilter] = useState<number | null>(null)
  const [createDate, setCreateDate] = useState<Date | null>(null)
  const [isCreating, setIsCreating] = useState(false)
  const [createError, setCreateError] = useState<string | null>(null)
  const [selectedActivity, setSelectedActivity] = useState<ScheduledJobActivity | null>(null)
  const [detailsError, setDetailsError] = useState<string | null>(null)
  const [isReopening, setIsReopening] = useState(false)
  const [completingActivity, setCompletingActivity] =
    useState<ScheduledJobActivity | null>(null)
  const [isCompleting, setIsCompleting] = useState(false)
  const [completionError, setCompletionError] = useState<string | null>(null)
  const [reschedulingActivity, setReschedulingActivity] =
    useState<ScheduledJobActivity | null>(null)
  const [isRescheduling, setIsRescheduling] = useState(false)
  const [rescheduleError, setRescheduleError] = useState<string | null>(null)

  const gridStart = useMemo(() => getGridStart(visibleMonth), [visibleMonth])
  const gridEnd = useMemo(() => addDays(gridStart, 42), [gridStart])
  const days = useMemo(
    () => Array.from({ length: 42 }, (_, index) => addDays(gridStart, index)),
    [gridStart],
  )

  useEffect(() => {
    const controller = new AbortController()

    async function loadCalendar() {
      setIsLoading(true)
      setError(null)
      try {
        setActivities(await getCalendarJobActivities(
          gridStart.toISOString(),
          gridEnd.toISOString(),
          controller.signal,
        ))
      } catch (loadError) {
        if (!isAbortError(loadError)) setError(getErrorMessage(loadError))
      } finally {
        if (!controller.signal.aborted) setIsLoading(false)
      }
    }

    void loadCalendar()
    return () => controller.abort()
  }, [gridEnd, gridStart])

  useEffect(() => {
    const controller = new AbortController()

    async function loadJobs() {
      setIsJobsLoading(true)
      setJobsError(null)
      try {
        setJobs(await getJobs(controller.signal))
      } catch (loadError) {
        if (!isAbortError(loadError)) setJobsError(getErrorMessage(loadError))
      } finally {
        if (!controller.signal.aborted) setIsJobsLoading(false)
      }
    }

    void loadJobs()
    return () => controller.abort()
  }, [])

  const filteredActivities = useMemo(() => {
    const normalizedQuery = searchQuery.trim().toLocaleLowerCase()
    return activities.filter((activity) => {
      const matchesSearch = !normalizedQuery || [
        activity.title,
        activity.company,
        activity.jobTitle,
        activity.contact ?? '',
      ].some((value) => value.toLocaleLowerCase().includes(normalizedQuery))
      const matchesType = typeFilter === 'ALL' || activity.type === typeFilter
      const matchesStatus = statusFilter === 'ALL'
        || (statusFilter === 'COMPLETED'
          ? activity.completedAt !== null
          : activity.completedAt === null)
      const matchesJob = jobIdFilter === null || activity.jobId === jobIdFilter
      return matchesSearch && matchesType && matchesStatus && matchesJob
    })
  }, [activities, jobIdFilter, searchQuery, statusFilter, typeFilter])
  const hasActiveFilters = searchQuery.trim() !== ''
    || typeFilter !== 'ALL'
    || statusFilter !== 'ALL'
    || jobIdFilter !== null
  const activitiesByDay = useMemo(
    () => groupActivitiesByDay(filteredActivities),
    [filteredActivities],
  )
  const agendaDays = useMemo(
    () => days.filter((day) => (
      day.getMonth() === visibleMonth.getMonth()
      && day.getFullYear() === visibleMonth.getFullYear()
      && (activitiesByDay.get(toDateKey(day))?.length ?? 0) > 0
    )),
    [activitiesByDay, days, visibleMonth],
  )

  const selectedJob = selectedActivity
    ? jobs.find((job) => job.id === selectedActivity.jobId)
    : undefined
  const completingJob = completingActivity
    ? jobs.find((job) => job.id === completingActivity.jobId)
    : undefined

  function moveMonth(offset: number) {
    setNotice(null)
    setVisibleMonth((current) => new Date(current.getFullYear(), current.getMonth() + offset, 1))
  }

  function openCreateDialog(date: Date) {
    setCreateError(null)
    setCreateDate(getSuggestedActivityDate(date))
  }

  function clearFilters() {
    setSearchQuery('')
    setTypeFilter('ALL')
    setStatusFilter('ALL')
    setJobIdFilter(null)
  }

  async function handleCreate(jobId: number, input: CreateJobActivityInput) {
    const job = jobs.find((item) => item.id === jobId)
    if (!job) return

    setIsCreating(true)
    setCreateError(null)
    try {
      const created = await createJobActivity(jobId, input)
      const scheduledActivity: ScheduledJobActivity = {
        id: created.id,
        jobId,
        company: job.company,
        jobTitle: job.title,
        type: created.type as ScheduledJobActivity['type'],
        title: created.title,
        contact: created.contact,
        occurredAt: created.occurredAt,
        completedAt: created.completedAt,
      }
      const activityTime = new Date(created.occurredAt)
      if (activityTime >= gridStart && activityTime < gridEnd) {
        setActivities((current) => [...current, scheduledActivity].sort(compareActivities))
      } else {
        setVisibleMonth(startOfMonth(activityTime))
      }
      setNotice(created.title + ' added for ' + dayHeadingFormatter.format(activityTime)
        + ' at ' + timeFormatter.format(activityTime) + '.')
      setCreateDate(null)
    } catch (saveError) {
      setCreateError(getErrorMessage(saveError))
    } finally {
      setIsCreating(false)
    }
  }

  async function handleComplete(input: CompleteJobActivityInput) {
    if (!completingActivity) return

    setIsCompleting(true)
    setCompletionError(null)
    try {
      const updated = await completeJobActivity(
        completingActivity.jobId,
        completingActivity.id,
        input,
      )
      setActivities((current) => current.map((activity) => (
        activity.id === completingActivity.id
          ? { ...activity, completedAt: updated.completedAt }
          : activity
      )))
      if (input.jobStatus) {
        const nextStatus = input.jobStatus
        setJobs((current) => current.map((job) => (
          job.id === completingActivity.jobId ? { ...job, status: nextStatus } : job
        )))
      }
      const statusDetail = input.jobStatus
        ? ' Job stage updated to ' + getJobStatusConfig(input.jobStatus).label + '.'
        : ''
      setNotice(completingActivity.title + ' completed.' + statusDetail)
      setCompletingActivity(null)
    } catch (saveError) {
      setCompletionError(getErrorMessage(saveError))
    } finally {
      setIsCompleting(false)
    }
  }

  async function handleReopen() {
    if (!selectedActivity) return

    setIsReopening(true)
    setDetailsError(null)
    try {
      const result = await reopenJobActivity(selectedActivity.jobId, selectedActivity.id)
      const reopenedActivity = { ...selectedActivity, completedAt: result.activity.completedAt }
      setActivities((current) => current.map((activity) => (
        activity.id === reopenedActivity.id ? reopenedActivity : activity
      )))
      setJobs((current) => current.map((job) => (
        job.id === selectedActivity.jobId ? { ...job, status: result.jobStatus } : job
      )))
      setSelectedActivity(reopenedActivity)
      const statusDetail = result.jobStatusRestored
        ? ' Job stage restored to ' + getJobStatusConfig(result.jobStatus).label + '.'
        : ''
      setNotice(selectedActivity.title + ' reopened.' + statusDetail)
    } catch (saveError) {
      setDetailsError(getErrorMessage(saveError))
    } finally {
      setIsReopening(false)
    }
  }

  async function handleReschedule(occurredAt: string) {
    if (!reschedulingActivity) return

    setIsRescheduling(true)
    setRescheduleError(null)
    try {
      const updated = await rescheduleJobActivity(
        reschedulingActivity.jobId,
        reschedulingActivity.id,
        { occurredAt },
      )
      const nextActivity = {
        ...reschedulingActivity,
        occurredAt: updated.occurredAt,
        completedAt: updated.completedAt,
      }
      const nextTime = new Date(nextActivity.occurredAt)
      setActivities((current) => (
        nextTime >= gridStart && nextTime < gridEnd
          ? current.map((activity) => activity.id === nextActivity.id ? nextActivity : activity)
              .sort(compareActivities)
          : current.filter((activity) => activity.id !== nextActivity.id)
      ))
      setNotice(nextActivity.title + ' moved to ' + dayHeadingFormatter.format(nextTime)
        + ' at ' + timeFormatter.format(nextTime) + '.')
      setReschedulingActivity(null)
    } catch (saveError) {
      setRescheduleError(getErrorMessage(saveError))
    } finally {
      setIsRescheduling(false)
    }
  }

  return (
    <div className="page calendar-page">
      <header className="calendar-page__header">
        <div>
          <p className="calendar-page__eyebrow">Interviews and follow-ups</p>
          <h1>Activity calendar</h1>
          <span>Plan new activities, review scheduled work, and move unfinished actions without leaving the month.</span>
        </div>
        <div className="calendar-page__actions">
          <button
            className="button button--primary"
            type="button"
            onClick={() => openCreateDialog(getDefaultCreateDate(visibleMonth))}
          >
            <span aria-hidden="true">＋</span> Add activity
          </button>
          <Link className="button button--secondary" to="/dashboard">Back to dashboard</Link>
        </div>
      </header>

      <section className="calendar-shell" aria-labelledby="calendar-month-heading">
        <div className="calendar-toolbar">
          <div>
            <p>Schedule</p>
            <h2 id="calendar-month-heading">{monthFormatter.format(visibleMonth)}</h2>
          </div>
          <div className="calendar-toolbar__actions">
            <button type="button" aria-label="Previous month" onClick={() => moveMonth(-1)}>←</button>
            <button type="button" onClick={() => {
              setNotice(null)
              setVisibleMonth(startOfMonth(new Date()))
            }}>Today</button>
            <button type="button" aria-label="Next month" onClick={() => moveMonth(1)}>→</button>
          </div>
        </div>

        {notice && <p className="calendar-notice" role="status">{notice}</p>}
        {error && (
          <div className="calendar-error" role="alert">
            <strong>Calendar could not be loaded.</strong>
            <span>{error}</span>
          </div>
        )}

        <CalendarFilters
          searchQuery={searchQuery}
          typeFilter={typeFilter}
          statusFilter={statusFilter}
          jobIdFilter={jobIdFilter}
          jobs={jobs}
          shownCount={filteredActivities.length}
          totalCount={activities.length}
          hasActiveFilters={hasActiveFilters}
          onSearchChange={setSearchQuery}
          onTypeChange={setTypeFilter}
          onStatusChange={setStatusFilter}
          onJobChange={setJobIdFilter}
          onClear={clearFilters}
        />

        <div className="calendar-weekdays" aria-hidden="true">
          {weekdayLabels.map((label) => <span key={label}>{label}</span>)}
        </div>
        <div className="calendar-grid" aria-label={monthFormatter.format(visibleMonth)}>
          {days.map((day) => {
            const dayActivities = activitiesByDay.get(toDateKey(day)) ?? []
            const isOutsideMonth = day.getMonth() !== visibleMonth.getMonth()
              || day.getFullYear() !== visibleMonth.getFullYear()
            return (
              <section
                className={'calendar-day' + (isOutsideMonth ? ' calendar-day--outside' : '')
                  + (isToday(day) ? ' calendar-day--today' : '')}
                key={toDateKey(day)}
                aria-label={dayHeadingFormatter.format(day)}
              >
                <button
                  className="calendar-day__number calendar-day__add"
                  type="button"
                  aria-label={'Add activity on ' + dayHeadingFormatter.format(day)}
                  title="Add activity"
                  onClick={() => openCreateDialog(day)}
                >
                  {day.getDate()}
                </button>
                <div className="calendar-day__events">
                  {dayActivities.map((activity) => (
                    <CalendarEvent
                      key={activity.id}
                      activity={activity}
                      onOpen={(item) => {
                        setDetailsError(null)
                        setSelectedActivity(item)
                      }}
                    />
                  ))}
                </div>
              </section>
            )
          })}
        </div>

        <div className="calendar-agenda" aria-label={monthFormatter.format(visibleMonth) + ' agenda'}>
          {isLoading ? (
            <div className="calendar-agenda__state">Loading activities…</div>
          ) : agendaDays.length === 0 ? (
            <div className="calendar-agenda__state">
              {hasActiveFilters ? (
                <>
                  <strong>No activities match these filters.</strong>
                  <span>Try a broader search or clear the current filters.</span>
                  <button type="button" onClick={clearFilters}>Clear filters</button>
                </>
              ) : (
                <>
                  <strong>No scheduled activities this month.</strong>
                  <span>Add an interview or follow-up directly from this calendar.</span>
                  <button type="button" onClick={() => openCreateDialog(getDefaultCreateDate(visibleMonth))}>
                    Add activity
                  </button>
                </>
              )}
            </div>
          ) : agendaDays.map((day) => (
            <section className="calendar-agenda__day" key={toDateKey(day)}>
              <header>
                <h3>{dayHeadingFormatter.format(day)}</h3>
                <button
                  type="button"
                  aria-label={'Add activity on ' + dayHeadingFormatter.format(day)}
                  onClick={() => openCreateDialog(day)}
                >
                  ＋ Add
                </button>
              </header>
              <div>
                {(activitiesByDay.get(toDateKey(day)) ?? []).map((activity) => (
                  <CalendarAgendaItem
                    key={activity.id}
                    activity={activity}
                    onOpen={(item) => {
                      setDetailsError(null)
                      setSelectedActivity(item)
                    }}
                  />
                ))}
              </div>
            </section>
          ))}
        </div>

        {isLoading && <div className="calendar-loading" aria-label="Loading calendar activities" />}
      </section>

      {selectedActivity && (
        <CalendarActivityDetailsDialog
          activity={selectedActivity}
          job={selectedJob}
          isReopening={isReopening}
          error={detailsError}
          onClose={() => {
            setDetailsError(null)
            setSelectedActivity(null)
          }}
          onComplete={() => {
            setCompletionError(null)
            setCompletingActivity(selectedActivity)
            setSelectedActivity(null)
          }}
          onReschedule={() => {
            setRescheduleError(null)
            setReschedulingActivity(selectedActivity)
            setSelectedActivity(null)
          }}
          onReopen={() => void handleReopen()}
        />
      )}

      {createDate && (
        <CalendarActivityDialog
          jobs={jobs}
          initialDate={createDate}
          isJobsLoading={isJobsLoading}
          jobsError={jobsError}
          isSaving={isCreating}
          error={createError}
          onClose={() => {
            setCreateError(null)
            setCreateDate(null)
          }}
          onSubmit={(jobId, input) => void handleCreate(jobId, input)}
        />
      )}

      {completingActivity && completingJob && (
        <CompleteActivityDialog
          activity={completingActivity}
          currentStatus={completingJob.status}
          isSaving={isCompleting}
          error={completionError}
          onClose={() => {
            setCompletionError(null)
            setCompletingActivity(null)
          }}
          onSubmit={(input) => void handleComplete(input)}
        />
      )}

      {reschedulingActivity && (
        <RescheduleActivityDialog
          activity={reschedulingActivity}
          isSaving={isRescheduling}
          error={rescheduleError}
          onClose={() => {
            setRescheduleError(null)
            setReschedulingActivity(null)
          }}
          onSubmit={(occurredAt) => void handleReschedule(occurredAt)}
        />
      )}
    </div>
  )
}

function CalendarEvent({
  activity,
  onOpen,
}: {
  activity: ScheduledJobActivity
  onOpen: (activity: ScheduledJobActivity) => void
}) {
  const config = getJobActivityTypeConfig(activity.type)
  return (
    <button
      className={'calendar-event calendar-event--' + activity.type.toLowerCase()
        + (activity.completedAt ? ' calendar-event--completed' : '')}
      type="button"
      title={'Open ' + config.shortLabel.toLowerCase() + ' details · ' + activity.jobTitle
        + ' at ' + activity.company}
      onClick={() => onOpen(activity)}
    >
      <span>{timeFormatter.format(new Date(activity.occurredAt))}</span>
      <strong>{activity.title}</strong>
      <small>{activity.company}</small>
      {activity.completedAt && (
        <span className="calendar-event__check" aria-label="Completed">✓</span>
      )}
    </button>
  )
}

function CalendarAgendaItem({
  activity,
  onOpen,
}: {
  activity: ScheduledJobActivity
  onOpen: (activity: ScheduledJobActivity) => void
}) {
  const config = getJobActivityTypeConfig(activity.type)
  return (
    <article className={'calendar-agenda-item calendar-agenda-item--' + activity.type.toLowerCase()
      + (activity.completedAt ? ' calendar-agenda-item--completed' : '')}>
      <span className="calendar-agenda-item__time">
        <strong>{timeFormatter.format(new Date(activity.occurredAt))}</strong>
        <small>{config.shortLabel}</small>
      </span>
      <span className="calendar-agenda-item__content">
        <strong>{activity.title}</strong>
        <small>{activity.jobTitle} · {activity.company}</small>
      </span>
      <span className="calendar-agenda-item__actions">
        {activity.completedAt && (
          <span className="calendar-agenda-item__done">✓ Completed</span>
        )}
        <button type="button" onClick={() => onOpen(activity)}>Details</button>
        <Link to={'/jobs/' + activity.jobId}>View job</Link>
      </span>
    </article>
  )
}

function startOfMonth(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth(), 1)
}

function getGridStart(month: Date): Date {
  return new Date(month.getFullYear(), month.getMonth(), 1 - month.getDay())
}

function addDays(date: Date, days: number): Date {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate() + days)
}

function toDateKey(date: Date): string {
  return [
    date.getFullYear(),
    String(date.getMonth() + 1).padStart(2, '0'),
    String(date.getDate()).padStart(2, '0'),
  ].join('-')
}

function groupActivitiesByDay(activities: ScheduledJobActivity[]) {
  const grouped = new Map<string, ScheduledJobActivity[]>()
  activities.forEach((activity) => {
    const key = toDateKey(new Date(activity.occurredAt))
    grouped.set(key, [...(grouped.get(key) ?? []), activity])
  })
  return grouped
}

function compareActivities(first: ScheduledJobActivity, second: ScheduledJobActivity): number {
  return new Date(first.occurredAt).getTime() - new Date(second.occurredAt).getTime()
}

function isToday(date: Date): boolean {
  const today = new Date()
  return toDateKey(date) === toDateKey(today)
}

function getDefaultCreateDate(visibleMonth: Date): Date {
  const today = new Date()
  return visibleMonth.getFullYear() === today.getFullYear()
    && visibleMonth.getMonth() === today.getMonth()
    ? today
    : visibleMonth
}

function getSuggestedActivityDate(date: Date): Date {
  const suggested = new Date(date.getFullYear(), date.getMonth(), date.getDate(), 9)
  const now = new Date()
  if (isToday(suggested) && suggested <= now) {
    suggested.setHours(now.getHours() + 1, 0, 0, 0)
  }
  return suggested
}
