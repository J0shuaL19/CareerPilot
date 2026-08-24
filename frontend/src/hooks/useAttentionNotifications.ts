import { useEffect, useRef, useState } from 'react'
import type { JobAttentionItem } from '../types/jobActivity'

export type AttentionNotificationStatus =
  | 'unsupported'
  | 'disabled'
  | 'enabled'
  | 'blocked'

interface UseAttentionNotificationsResult {
  status: AttentionNotificationStatus
  isRequesting: boolean
  error: string | null
  toggle: () => Promise<void>
}

interface DailyNotificationRecord {
  date: string
  jobIds: number[]
}

const enabledStorageKey = 'careerpilot.attentionNotifications.enabled'
const notifiedStorageKey = 'careerpilot.attentionNotifications.notified'

export function useAttentionNotifications(
  items: JobAttentionItem[],
): UseAttentionNotificationsResult {
  const [status, setStatus] = useState<AttentionNotificationStatus>(getStatus)
  const [isRequesting, setIsRequesting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const sessionNotifiedIds = useRef(new Set<number>())

  useEffect(() => {
    function syncStatus() {
      setStatus(getStatus())
    }

    window.addEventListener('focus', syncStatus)
    return () => window.removeEventListener('focus', syncStatus)
  }, [])

  useEffect(() => {
    if (status !== 'enabled' || items.length === 0) return

    const today = toLocalDateKey(new Date())
    const storedRecord = readDailyRecord(today)
    const alreadyNotified = new Set([
      ...storedRecord.jobIds,
      ...sessionNotifiedIds.current,
    ])
    const newItems = items.filter((item) => !alreadyNotified.has(item.jobId))
    if (newItems.length === 0) return

    try {
      const notification = createAttentionNotification(newItems, today)
      notification.onclick = () => {
        window.focus()
        notification.close()
      }
      newItems.forEach((item) => sessionNotifiedIds.current.add(item.jobId))
      writeDailyRecord({
        date: today,
        jobIds: [...new Set([...storedRecord.jobIds, ...newItems.map((item) => item.jobId)])],
      })
    } catch {
      const errorTimeout = window.setTimeout(() => {
        setError('CareerPilot could not show a browser alert. Check this site’s notification settings.')
      }, 0)
      return () => window.clearTimeout(errorTimeout)
    }
  }, [items, status])

  async function toggle() {
    setError(null)

    if (!supportsNotifications()) {
      setStatus('unsupported')
      return
    }

    if (status === 'enabled') {
      writeEnabledPreference(false)
      setStatus('disabled')
      return
    }

    if (Notification.permission === 'denied') {
      setStatus('blocked')
      setError('Notifications are blocked. Allow them in your browser’s site settings first.')
      return
    }

    setIsRequesting(true)
    try {
      const permission = Notification.permission === 'granted'
        ? 'granted'
        : await Notification.requestPermission()
      if (permission === 'granted') {
        writeEnabledPreference(true)
        setStatus('enabled')
      } else {
        writeEnabledPreference(false)
        setStatus(permission === 'denied' ? 'blocked' : 'disabled')
        if (permission === 'denied') {
          setError('Notifications are blocked. Allow them in your browser’s site settings first.')
        }
      }
    } catch {
      setError('CareerPilot could not request notification permission.')
    } finally {
      setIsRequesting(false)
    }
  }

  return { status, isRequesting, error, toggle }
}

function getStatus(): AttentionNotificationStatus {
  if (!supportsNotifications()) return 'unsupported'
  if (Notification.permission === 'denied') return 'blocked'
  return Notification.permission === 'granted' && readEnabledPreference()
    ? 'enabled'
    : 'disabled'
}

function supportsNotifications(): boolean {
  return typeof window !== 'undefined'
    && window.isSecureContext
    && 'Notification' in window
}

function createAttentionNotification(
  items: JobAttentionItem[],
  date: string,
): Notification {
  if (items.length === 1) {
    const [item] = items
    return new Notification(`Follow up with ${item.company}`, {
      body: `${item.jobTitle} has been waiting ${item.daysWithoutActivity} days.`,
      tag: `careerpilot-attention-${date}-${item.jobId}`,
    })
  }

  const companies = items.slice(0, 3).map((item) => item.company).join(', ')
  const remaining = items.length - 3
  return new Notification(`${items.length} applications need attention`, {
    body: remaining > 0 ? `${companies}, and ${remaining} more` : companies,
    tag: `careerpilot-attention-${date}-${items.map((item) => item.jobId).join('-')}`,
  })
}

function readEnabledPreference(): boolean {
  try {
    return localStorage.getItem(enabledStorageKey) === 'true'
  } catch {
    return false
  }
}

function writeEnabledPreference(enabled: boolean) {
  try {
    localStorage.setItem(enabledStorageKey, String(enabled))
  } catch {
    // Notification permission still applies for the current page if storage is unavailable.
  }
}

function readDailyRecord(today: string): DailyNotificationRecord {
  try {
    const value = localStorage.getItem(notifiedStorageKey)
    if (value === null) return { date: today, jobIds: [] }
    const record = JSON.parse(value) as Partial<DailyNotificationRecord>
    if (record.date !== today || !Array.isArray(record.jobIds)) {
      return { date: today, jobIds: [] }
    }
    return {
      date: today,
      jobIds: record.jobIds.filter((id): id is number => Number.isInteger(id)),
    }
  } catch {
    return { date: today, jobIds: [] }
  }
}

function writeDailyRecord(record: DailyNotificationRecord) {
  try {
    localStorage.setItem(notifiedStorageKey, JSON.stringify(record))
  } catch {
    // The in-memory set still prevents duplicates during this page session.
  }
}

function toLocalDateKey(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return [year, month, day].join('-')
}