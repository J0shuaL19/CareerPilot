export type JobStatus =
  | 'SAVED'
  | 'APPLIED'
  | 'OA'
  | 'INTERVIEW'
  | 'OFFER'
  | 'REJECTED'
  | 'WITHDRAWN'

export interface Job {
  id: number
  company: string
  title: string
  description: string
  jobUrl: string | null
  status: JobStatus
  createdAt: string
}

export interface CreateJobInput {
  company: string
  title: string
  description: string
  jobUrl?: string
}
