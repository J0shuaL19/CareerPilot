import { useState } from 'react'
import type { JobActivity } from '../types/jobActivity'
import { getJobActivityTypeConfig } from '../utils/jobActivity'

interface JobTimelineProps {
  activities: JobActivity[]
  deletingActivityId: number | null
  editingActivityId: number | null
  exportingActivityId: number | null
  onEdit: (activity: JobActivity) => void
  onExportCalendar: (activity: JobActivity) => Promise<void>
  onDelete: (activity: JobActivity) => Promise<void>
}

const dateTimeFormatter = new Intl.DateTimeFormat('en-US', {
  month: 'short',
  day: 'numeric',
  year: 'numeric',
  hour: 'numeric',
  minute: '2-digit',
})

export function JobTimeline({
  activities,
  deletingActivityId,
  editingActivityId,
  exportingActivityId,
  onEdit,
  onExportCalendar,
  onDelete,
}: JobTimelineProps) {
  const [confirmingActivityId, setConfirmingActivityId] = useState<number | null>(null)

  if (activities.length === 0) {
    return (
      <div className="timeline-empty">
        <span aria-hidden="true">◎</span>
        <h2>No activity yet</h2>
        <p>Add the first update to build a reliable history for this opportunity.</p>
      </div>
    )
  }

  return (
    <section className="job-timeline" aria-label="Job activity timeline">
      {activities.map((activity) => {
        const config = getJobActivityTypeConfig(activity.type)
        const isDeleting = deletingActivityId === activity.id
        const isConfirming = confirmingActivityId === activity.id
        const isEditing = editingActivityId === activity.id
        const isExporting = exportingActivityId === activity.id
        const canExportCalendar = activity.type === 'INTERVIEW' || activity.type === 'FOLLOW_UP'

        return (
          <article className={`timeline-entry timeline-entry--${activity.type.toLowerCase()}`} key={activity.id}>
            <div className="timeline-entry__marker" aria-hidden="true">{config.symbol}</div>
            <div className={`timeline-entry__card${isEditing ? ' timeline-entry__card--editing' : ''}`}>
              <div className="timeline-entry__meta">
                <span>{config.shortLabel}</span>
                <time dateTime={activity.occurredAt}>
                  {dateTimeFormatter.format(new Date(activity.occurredAt))}
                </time>
              </div>
              <h3>{activity.title}</h3>
              {activity.contact && (
                <p className="timeline-entry__contact">
                  <span aria-hidden="true">@</span> {activity.contact}
                </p>
              )}
              {activity.details && <p className="timeline-entry__details">{activity.details}</p>}

              <div className="timeline-entry__actions">
                {isConfirming ? (
                  <>
                    <span>Delete this entry?</span>
                    <button
                      type="button"
                      disabled={isDeleting}
                      onClick={() => setConfirmingActivityId(null)}
                    >
                      Cancel
                    </button>
                    <button
                      className="timeline-entry__confirm-delete"
                      type="button"
                      disabled={isDeleting}
                      onClick={() => void onDelete(activity)}
                    >
                      {isDeleting ? 'Deleting…' : 'Confirm delete'}
                    </button>
                  </>
                ) : (
                  <>
                    {canExportCalendar && (
                      <button
                        type="button"
                        disabled={isExporting}
                        onClick={() => void onExportCalendar(activity)}
                      >
                        {isExporting ? 'Downloading…' : 'Calendar ↓'}
                      </button>
                    )}
                    <button
                      type="button"
                      disabled={isDeleting || isEditing}
                      onClick={() => onEdit(activity)}
                    >
                      {isEditing ? 'Editing…' : 'Edit'}
                    </button>
                    <button type="button" onClick={() => setConfirmingActivityId(activity.id)}>
                      Delete
                    </button>
                  </>
                )}
              </div>
            </div>
          </article>
        )
      })}
    </section>
  )
}
