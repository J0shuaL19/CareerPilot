import { useEffect, useMemo, useState } from 'react'
import { getActivityConflicts } from '../services/jobActivityApi'
import type { ScheduledJobActivity } from '../types/jobActivity'
import { isAbortError } from '../utils/errors'

interface ActivityConflictOptions {
  occurredAt: string
  enabled: boolean
  excludeActivityId?: number
}

interface ConflictCheckState {
  key: string
  conflicts: ScheduledJobActivity[]
  isChecking: boolean
  error: string | null
}

export function useActivityConflicts({
  occurredAt,
  enabled,
  excludeActivityId,
}: ActivityConflictOptions) {
  const [check, setCheck] = useState<ConflictCheckState | null>(null)
  const proposedTime = useMemo(() => new Date(occurredAt), [occurredAt])
  const isValid = !Number.isNaN(proposedTime.getTime())
  const key = `${occurredAt}:${excludeActivityId ?? 'new'}`

  useEffect(() => {
    if (!enabled || !isValid) return

    const controller = new AbortController()
    const timeout = window.setTimeout(async () => {
      setCheck({ key, conflicts: [], isChecking: true, error: null })
      try {
        const conflicts = await getActivityConflicts(
          proposedTime.toISOString(),
          excludeActivityId,
          controller.signal,
        )
        if (!controller.signal.aborted) {
          setCheck({ key, conflicts, isChecking: false, error: null })
        }
      } catch (loadError) {
        if (!isAbortError(loadError)) {
          setCheck({
            key,
            conflicts: [],
            isChecking: false,
            error: 'CareerPilot could not check nearby activities. You can still save this interview.',
          })
        }
      }
    }, 350)

    return () => {
      window.clearTimeout(timeout)
      controller.abort()
    }
  }, [enabled, excludeActivityId, isValid, key, proposedTime])

  const isCurrent = check?.key === key
  return {
    conflicts: enabled && isValid && isCurrent ? check.conflicts : [],
    isChecking: enabled && isValid && (!isCurrent || check.isChecking),
    error: enabled && isValid && isCurrent ? check.error : null,
  }
}
