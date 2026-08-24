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

export interface RescheduleJobActivityInput {
  occurredAt: string
}

export interface ReopenJobActivityResult {
  activity: JobActivity
  jobStatus: JobStatus
  jobStatusRestored: boolean
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

export interface ScheduledJobActivity extends UpcomingJobActivity {
  completedAt: string | null
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
export type JobActivityCalendarImportType = Extract<
  JobActivityType,
  'INTERVIEW' | 'FOLLOW_UP'
>

export interface JobActivityCalendarImportEvent {
  eventNumber: number
  title: string
  details: string | null
  contact: string | null
  occurredAt: string | null
  suggestedType: JobActivityCalendarImportType
  importable: boolean
  errors: string[]
}

export interface JobActivityCalendarImportPreview {
  filename: string
  totalEvents: number
  importableEvents: number
  invalidEvents: number
  events: JobActivityCalendarImportEvent[]
}

export interface JobActivityCalendarImportItem {
  jobId: number
  type: JobActivityCalendarImportType
  title: string
  details?: string
  contact?: string
  occurredAt: string
}

export interface JobActivityCalendarImportInput {
  events: JobActivityCalendarImportItem[]
}

export interface JobActivityCalendarImportResult {
  imported: number
  skippedDuplicates: number
}
