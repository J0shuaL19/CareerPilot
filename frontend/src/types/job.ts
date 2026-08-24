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
  attentionSnoozedUntil: string | null
}

export interface CreateJobInput {
  company: string
  title: string
  description: string
  jobUrl?: string
}

export interface UpdateJobStatusInput {
  status: JobStatus
}

export type UpdateJobInput = CreateJobInput

export type JobCsvImportRowState = 'VALID' | 'DUPLICATE' | 'INVALID'

export interface JobCsvImportRow {
  rowNumber: number
  company: string
  title: string
  description: string
  jobUrl: string | null
  status: JobStatus
  state: JobCsvImportRowState
  errors: string[]
}

export interface JobCsvImportPreview {
  filename: string
  totalRows: number
  validRows: number
  duplicateRows: number
  invalidRows: number
  rows: JobCsvImportRow[]
}

export interface JobCsvImportResult {
  imported: number
  skippedDuplicates: number
  skippedInvalid: number
}
