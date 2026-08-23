export type JobActivityType = 'APPLICATION' | 'INTERVIEW' | 'FOLLOW_UP' | 'NOTE'

export interface JobActivity {
  id: number
  jobId: number
  type: JobActivityType
  title: string
  details: string | null
  contact: string | null
  occurredAt: string
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
