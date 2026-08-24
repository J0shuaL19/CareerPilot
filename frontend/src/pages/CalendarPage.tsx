import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { RescheduleActivityDialog } from '../components/RescheduleActivityDialog'
import {
  getCalendarJobActivities,
  rescheduleJobActivity,
} from '../services/jobActivityApi'
import type { ScheduledJobActivity } from '../types/jobActivity'
import { getErrorMessage, isAbortError } from '../utils/errors'
import { getJobActivityTypeConfig } from '../utils/jobActivity'

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

  const activitiesByDay = useMemo(() => groupActivitiesByDay(activities), [activities])
  const agendaDays = useMemo(
    () => days.filter((day) => (
      day.getMonth() === visibleMonth.getMonth()
      && day.getFullYear() === visibleMonth.getFullYear()
      && (activitiesByDay.get(toDateKey(day))?.length ?? 0) > 0
    )),
    [activitiesByDay, days, visibleMonth],
  )

  function moveMonth(offset: number) {
    setNotice(null)
    setVisibleMonth((current) => new Date(current.getFullYear(), current.getMonth() + offset, 1))
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
          <span>See scheduled work in context and move unfinished actions without leaving the month.</span>
        </div>
        <Link className="button button--secondary" to="/dashboard">Back to dashboard</Link>
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
                <span className="calendar-day__number">{day.getDate()}</span>
                <div className="calendar-day__events">
                  {dayActivities.map((activity) => (
                    <CalendarEvent
                      key={activity.id}
                      activity={activity}
                      onReschedule={setReschedulingActivity}
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
              <strong>No scheduled activities this month.</strong>
              <span>Add an interview or follow-up from a job page to see it here.</span>
            </div>
          ) : agendaDays.map((day) => (
            <section className="calendar-agenda__day" key={toDateKey(day)}>
              <h3>{dayHeadingFormatter.format(day)}</h3>
              <div>
                {(activitiesByDay.get(toDateKey(day)) ?? []).map((activity) => (
                  <CalendarAgendaItem
                    key={activity.id}
                    activity={activity}
                    onReschedule={setReschedulingActivity}
                  />
                ))}
              </div>
            </section>
          ))}
        </div>

        {isLoading && <div className="calendar-loading" aria-label="Loading calendar activities" />}
      </section>

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
  onReschedule,
}: {
  activity: ScheduledJobActivity
  onReschedule: (activity: ScheduledJobActivity) => void
}) {
  const config = getJobActivityTypeConfig(activity.type)
  const content = (
    <>
      <span>{timeFormatter.format(new Date(activity.occurredAt))}</span>
      <strong>{activity.title}</strong>
      <small>{activity.company}</small>
    </>
  )

  return activity.completedAt ? (
    <Link
      className={'calendar-event calendar-event--' + activity.type.toLowerCase()
        + ' calendar-event--completed'}
      to={'/jobs/' + activity.jobId}
      title={'Completed · ' + activity.jobTitle + ' at ' + activity.company}
    >
      {content}<span className="calendar-event__check" aria-label="Completed">✓</span>
    </Link>
  ) : (
    <button
      className={'calendar-event calendar-event--' + activity.type.toLowerCase()}
      type="button"
      title={'Reschedule ' + config.shortLabel.toLowerCase() + ' · ' + activity.jobTitle
        + ' at ' + activity.company}
      onClick={() => onReschedule(activity)}
    >
      {content}
    </button>
  )
}

function CalendarAgendaItem({
  activity,
  onReschedule,
}: {
  activity: ScheduledJobActivity
  onReschedule: (activity: ScheduledJobActivity) => void
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
        {activity.completedAt ? (
          <span className="calendar-agenda-item__done">✓ Completed</span>
        ) : (
          <button type="button" onClick={() => onReschedule(activity)}>Reschedule</button>
        )}
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