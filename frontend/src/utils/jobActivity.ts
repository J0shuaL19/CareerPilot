import type { JobActivityType } from '../types/jobActivity'

export const jobActivityTypeOptions: ReadonlyArray<{
  value: JobActivityType
  label: string
  shortLabel: string
  symbol: string
}> = [
  { value: 'APPLICATION', label: 'Application submitted', shortLabel: 'Application', symbol: '↗' },
  { value: 'INTERVIEW', label: 'Interview', shortLabel: 'Interview', symbol: '◈' },
  { value: 'FOLLOW_UP', label: 'Follow-up', shortLabel: 'Follow-up', symbol: '→' },
  { value: 'NOTE', label: 'General note', shortLabel: 'Note', symbol: '•' },
]

export function getJobActivityTypeConfig(type: JobActivityType) {
  return jobActivityTypeOptions.find((option) => option.value === type)!
}

export function toLocalDateTimeValue(date = new Date()) {
  const localTime = new Date(date.getTime() - date.getTimezoneOffset() * 60_000)
  return localTime.toISOString().slice(0, 16)
}
