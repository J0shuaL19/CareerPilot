import type { JobStatus } from '../types/job'

export const jobStatusOptions: ReadonlyArray<{
  value: JobStatus
  label: string
  tone: 'neutral' | 'info' | 'warning' | 'success' | 'danger'
}> = [
  { value: 'SAVED', label: 'Saved', tone: 'neutral' },
  { value: 'APPLIED', label: 'Applied', tone: 'info' },
  { value: 'OA', label: 'Online assessment', tone: 'warning' },
  { value: 'INTERVIEW', label: 'Interview', tone: 'warning' },
  { value: 'OFFER', label: 'Offer', tone: 'success' },
  { value: 'REJECTED', label: 'Rejected', tone: 'danger' },
  { value: 'WITHDRAWN', label: 'Withdrawn', tone: 'neutral' },
]

export function getJobStatusConfig(status: JobStatus) {
  return jobStatusOptions.find((option) => option.value === status)!
}
