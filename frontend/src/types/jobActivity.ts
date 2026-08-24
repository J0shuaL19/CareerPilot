import type { JobStatus } from './job'

export type JobActivityType = 'APPLICATION' | 'INTERVIEW' | 'FOLLOW_UP' | 'NOTE'

export interface JobActivity {
  id: number
  jobId: number
  type: JobActivityType
  title: string
  details: string | null
  contact: string | null
  occurredAt: string
  completedAt: string | null
  completionNote: string | null
  createdAt: string
}

export interface CreateJobActivityInput {
  type: JobActivityType
  title: string
  details?: string
  contact?: string
  occurredAt: string
}

export type UpdateJobActivityInput = CreateJobActivityInput

export interface CompleteJobActivityInput {
  note?: string
  jobStatus?: JobStatus
}

export interface UpcomingJobActivity {
  id: number
  jobId: number
  company: string
  jobTitle: string
  type: Extract<JobActivityType, 'INTERVIEW' | 'FOLLOW_UP'>
  title: string
  contact: string | null
  occurredAt: string
}

export interface JobAttentionItem {
  jobId: number
  company: string
  jobTitle: string
  status: JobStatus
  lastActivityAt: string
  daysWithoutActivity: number
  thresholdDays: number
}

export type JobAttentionHistoryAction =
  | 'SNOOZED'
  | 'RESCHEDULED'
  | 'RESUMED'
  | 'RESTORED'
  | 'CLEARED_BY_ACTIVITY'

export interface JobAttentionHistoryEntry {
  id: number
  jobId: number
  company: string
  jobTitle: string
  action: JobAttentionHistoryAction
  previousSnoozedUntil: string | null
  newSnoozedUntil: string | null
  createdAt: string
}

export interface JobAttentionSettings {
  appliedDays: number
  onlineAssessmentDays: number
  interviewDays: number
}
