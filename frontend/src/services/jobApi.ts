import type {
  CreateJobInput,
  Job,
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

export function deleteJob(id: number): Promise<void> {
  return apiRequest<void>(`/api/jobs/${id}`, {
    method: 'DELETE',
  })
}
