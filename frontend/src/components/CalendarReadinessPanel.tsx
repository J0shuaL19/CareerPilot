import { useEffect, useState } from 'react'
import type { ScheduledJobActivity } from '../types/jobActivity'

interface CalendarReadinessPanelProps {
  activities: ScheduledJobActivity[]
  onPrepare: (activity: ScheduledJobActivity) => void
}

const dateTimeFormatter = new Intl.DateTimeFormat('en-US', {
  weekday: 'short',
  month: 'short',
  day: 'numeric',
  hour: 'numeric',
  minute: '2-digit',
})

export function CalendarReadinessPanel({
  activities,
  onPrepare,
}: CalendarReadinessPanelProps) {
  const [now, setNow] = useState(() => Date.now())

  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), 60_000)
    return () => window.clearInterval(timer)
  }, [])

  const upcomingInterviews = activities
    .filter((activity) => (
      activity.type === 'INTERVIEW'
      && activity.completedAt === null
      && new Date(activity.occurredAt).getTime() >= now
      && (activity.preparationProgressPercent ?? 0) < 100
    ))
    .sort((first, second) => (
      new Date(first.occurredAt).getTime() - new Date(second.occurredAt).getTime()
    ))

  if (upcomingInterviews.length === 0) return null

  return (
    <section className="calendar-readiness" aria-labelledby="calendar-readiness-heading">
      <header className="calendar-readiness__heading">
        <div>
          <p>Preparation focus</p>
          <h3 id="calendar-readiness-heading">Interview readiness</h3>
        </div>
        <span>
          {upcomingInterviews.length} upcoming {upcomingInterviews.length === 1
            ? 'interview needs' : 'interviews need'} attention
        </span>
      </header>

      <div className="calendar-readiness__list">
        {upcomingInterviews.slice(0, 3).map((activity) => {
          const occurredAt = new Date(activity.occurredAt)
          const hoursUntilInterview = (occurredAt.getTime() - now) / 3_600_000
          const isUrgent = hoursUntilInterview <= 24
          const completedSections = activity.preparationCompletedSections ?? 0
          const totalSections = activity.preparationTotalSections ?? 4
          const progressPercent = activity.preparationProgressPercent ?? 0

          return (
            <article
              className={'calendar-readiness-item'
                + (isUrgent ? ' calendar-readiness-item--urgent' : '')}
              key={activity.id}
            >
              <div className="calendar-readiness-item__content">
                <span className="calendar-readiness-item__time">
                  {isUrgent && <strong>Within 24 hours</strong>}
                  <time dateTime={activity.occurredAt}>{dateTimeFormatter.format(occurredAt)}</time>
                </span>
                <strong>{activity.title}</strong>
                <small>{activity.jobTitle} · {activity.company}</small>
              </div>
              <div className="calendar-readiness-item__progress">
                <span>{completedSections} of {totalSections} ready</span>
                <progress
                  value={completedSections}
                  max={totalSections}
                  aria-label={`${activity.title} preparation ${progressPercent}% complete`}
                />
              </div>
              <button type="button" onClick={() => onPrepare(activity)}>
                Continue preparation
              </button>
            </article>
          )
        })}
      </div>

      {upcomingInterviews.length > 3 && (
        <p className="calendar-readiness__more">
          +{upcomingInterviews.length - 3} more in this calendar view
        </p>
      )}
    </section>
  )
}
