import { useEffect, useState } from 'react'
import { getJobAttentionHistory } from '../services/jobActivityApi'
import type {
  JobAttentionHistoryEntry,
  JobAttentionHistoryAction,
} from '../types/jobActivity'
import { getErrorMessage, isAbortError } from '../utils/errors'
import { formatDate } from '../utils/formatDate'

interface ReminderHistoryProps {
  reloadKey: number
  onViewJob: (jobId: number) => void
}

interface HistoryActionContent {
  icon: string
  label: string
  detail: string
  tone: 'paused' | 'changed' | 'resumed' | 'restored' | 'activity'
}

const collapsedEntryCount = 5
const dateTimeFormatter = new Intl.DateTimeFormat('en-US', {
  month: 'short',
  day: 'numeric',
  hour: 'numeric',
  minute: '2-digit',
})

export function ReminderHistory({ reloadKey, onViewJob }: ReminderHistoryProps) {
  const [history, setHistory] = useState<JobAttentionHistoryEntry[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [retryKey, setRetryKey] = useState(0)
  const [isExpanded, setIsExpanded] = useState(false)

  useEffect(() => {
    const controller = new AbortController()

    async function loadHistory() {
      setIsLoading(true)
      setError(null)
      try {
        setHistory(await getJobAttentionHistory(controller.signal))
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

    void loadHistory()
    return () => controller.abort()
  }, [reloadKey, retryKey])

  const visibleHistory = isExpanded
    ? history
    : history.slice(0, collapsedEntryCount)

  return (
    <section className="reminder-history" aria-labelledby="reminder-history-heading">
      <header className="reminder-history__heading">
        <div className="reminder-history__title">
          <span aria-hidden="true">↺</span>
          <div>
            <p>Recent changes</p>
            <h2 id="reminder-history-heading">Reminder history</h2>
          </div>
        </div>
        <span>{history.length === 0 ? 'No changes yet' : `${history.length} recent`}</span>
      </header>

      {isLoading && history.length === 0 && (
        <div className="reminder-history__state" role="status">
          <span className="spinner" aria-hidden="true" />
          Loading reminder history…
        </div>
      )}

      {!isLoading && error && history.length === 0 && (
        <div className="reminder-history__state reminder-history__state--error" role="alert">
          <span>{error}</span>
          <button type="button" onClick={() => setRetryKey((key) => key + 1)}>
            Try again
          </button>
        </div>
      )}

      {!isLoading && !error && history.length === 0 && (
        <div className="reminder-history__empty">
          <span aria-hidden="true">○</span>
          <div>
            <strong>No reminder changes yet</strong>
            <p>Pauses, date changes, resumes, and Undo actions will appear here.</p>
          </div>
        </div>
      )}

      {visibleHistory.length > 0 && (
        <ol className="reminder-history__list">
          {visibleHistory.map((entry) => {
            const content = getActionContent(entry)
            return (
              <li className="reminder-history__entry" key={entry.id}>
                <span
                  className={`reminder-history__icon reminder-history__icon--${content.tone}`}
                  aria-hidden="true"
                >
                  {content.icon}
                </span>
                <div className="reminder-history__content">
                  <strong>{content.label}</strong>
                  <span>{content.detail}</span>
                  <small>{dateTimeFormatter.format(new Date(entry.createdAt))}</small>
                </div>
                <button
                  className="reminder-history__job"
                  type="button"
                  aria-label={`View ${entry.jobTitle} at ${entry.company}`}
                  onClick={() => onViewJob(entry.jobId)}
                >
                  <strong>{entry.jobTitle}</strong>
                  <span>{entry.company}</span>
                  <small aria-hidden="true">View job →</small>
                </button>
              </li>
            )
          })}
        </ol>
      )}

      {history.length > collapsedEntryCount && (
        <button
          className="reminder-history__toggle"
          type="button"
          aria-expanded={isExpanded}
          onClick={() => setIsExpanded((current) => !current)}
        >
          {isExpanded ? 'Show less' : `Show all ${history.length} changes`}
        </button>
      )}
    </section>
  )
}

function getActionContent(entry: JobAttentionHistoryEntry): HistoryActionContent {
  const previousDate = formatOptionalDate(entry.previousSnoozedUntil)
  const newDate = formatOptionalDate(entry.newSnoozedUntil)
  const contentByAction: Record<JobAttentionHistoryAction, HistoryActionContent> = {
    SNOOZED: {
      icon: '◷',
      label: `Paused until ${newDate}`,
      detail: 'Reminder hidden temporarily',
      tone: 'paused',
    },
    RESCHEDULED: {
      icon: '↻',
      label: 'Return date changed',
      detail: `${previousDate} → ${newDate}`,
      tone: 'changed',
    },
    RESUMED: {
      icon: '▶',
      label: 'Reminder resumed',
      detail: `Was paused until ${previousDate}`,
      tone: 'resumed',
    },
    RESTORED: {
      icon: '↶',
      label: 'Undo restored reminder',
      detail: entry.previousSnoozedUntil === null
        ? `Restored to ${newDate}`
        : `${previousDate} → ${newDate}`,
      tone: 'restored',
    },
    CLEARED_BY_ACTIVITY: {
      icon: '✓',
      label: 'Cleared after new activity',
      detail: `Was paused until ${previousDate}`,
      tone: 'activity',
    },
  }
  return contentByAction[entry.action]
}

function formatOptionalDate(value: string | null): string {
  return value === null ? 'not paused' : formatDate(value + 'T12:00:00')
}