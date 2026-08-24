import type {
  CreateJobInput,
  Job,
  JobAttentionSnoozeSnapshot,
  JobCsvImportPreview,
  JobCsvImportResult,
  UpdateJobInput,
  UpdateJobStatusInput,
} from '../types/job'
import { apiDownload, apiRequest } from './apiClient'

export function getJobs(signal?: AbortSignal): Promise<Job[]> {
  return apiRequest<Job[]>('/api/jobs', { signal })
}

export function getJob(id: number, signal?: AbortSignal): Promise<Job> {
  return apiRequest<Job>(`/api/jobs/${id}`, { signal })
}

export function exportJobs(jobIds: number[]): Promise<Blob> {
  return apiDownload('/api/jobs/export', {
    method: 'POST',
    headers: {
      Accept: 'text/csv',
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ jobIds }),
  })
}

export function previewJobCsv(file: File): Promise<JobCsvImportPreview> {
  const formData = new FormData()
  formData.append('file', file)

  return apiRequest<JobCsvImportPreview>('/api/jobs/import/preview', {
    method: 'POST',
    body: formData,
  })
}

export function importJobCsv(file: File): Promise<JobCsvImportResult> {
  const formData = new FormData()
  formData.append('file', file)

  return apiRequest<JobCsvImportResult>('/api/jobs/import', {
    method: 'POST',
    body: formData,
  })
}

export function createJob(input: CreateJobInput): Promise<Job> {
  return apiRequest<Job>('/api/jobs', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(input),
  })
}

export function updateJobStatus(
  id: number,
  input: UpdateJobStatusInput,
): Promise<Job> {
  return apiRequest<Job>(`/api/jobs/${id}/status`, {
    method: 'PATCH',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(input),
  })
}

export function updateJob(id: number, input: UpdateJobInput): Promise<Job> {
  return apiRequest<Job>(`/api/jobs/${id}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(input),
  })
}

export function snoozeJobAttention(id: number, snoozedUntil: string): Promise<Job> {
  return apiRequest<Job>('/api/jobs/' + id + '/attention-snooze', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ snoozedUntil }),
  })
}

export function clearJobAttentionSnooze(id: number): Promise<Job> {
  return apiRequest<Job>('/api/jobs/' + id + '/attention-snooze', {
    method: 'DELETE',
  })
}

export function snoozeJobAttentionBulk(
  jobIds: number[],
  snoozedUntil: string,
): Promise<Job[]> {
  return apiRequest<Job[]>('/api/jobs/attention-snooze', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ jobIds, snoozedUntil }),
  })
}

export function clearJobAttentionSnoozeBulk(jobIds: number[]): Promise<Job[]> {
  return apiRequest<Job[]>('/api/jobs/attention-snooze/clear', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ jobIds }),
  })
}

export function restoreJobAttentionSnoozes(
  reminders: JobAttentionSnoozeSnapshot[],
): Promise<Job[]> {
  return apiRequest<Job[]>('/api/jobs/attention-snooze/restore', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ reminders }),
  })
}

export function deleteJob(id: number): Promise<void> {
  return apiRequest<void>(`/api/jobs/${id}`, {
    method: 'DELETE',
  })
}
