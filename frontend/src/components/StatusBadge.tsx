import type { JobStatus } from '../types/job'

const statusConfig: Record<
  JobStatus,
  { label: string; tone: 'neutral' | 'info' | 'warning' | 'success' | 'danger' }
> = {
  SAVED: { label: 'Saved', tone: 'neutral' },
  APPLIED: { label: 'Applied', tone: 'info' },
  OA: { label: 'Online assessment', tone: 'warning' },
  INTERVIEW: { label: 'Interview', tone: 'warning' },
  OFFER: { label: 'Offer', tone: 'success' },
  REJECTED: { label: 'Rejected', tone: 'danger' },
  WITHDRAWN: { label: 'Withdrawn', tone: 'neutral' },
}

interface StatusBadgeProps {
  status: JobStatus
}

export function StatusBadge({ status }: StatusBadgeProps) {
  const config = statusConfig[status]

  return (
    <span className={`status-badge status-badge--${config.tone}`}>
      {config.label}
    </span>
  )
}
