import type { JobStatus } from '../types/job'
import { getJobStatusConfig } from '../utils/jobStatus'

interface StatusBadgeProps {
  status: JobStatus
}

export function StatusBadge({ status }: StatusBadgeProps) {
  const config = getJobStatusConfig(status)

  return (
    <span className={`status-badge status-badge--${config.tone}`}>
      {config.label}
    </span>
  )
}
