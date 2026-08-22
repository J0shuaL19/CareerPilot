import type { CreateJobInput, Job, UpdateJobStatusInput } from '../types/job'
import { apiRequest } from './apiClient'

export function getJobs(signal?: AbortSignal): Promise<Job[]> {
  return apiRequest<Job[]>('/api/jobs', { signal })
}

export function getJob(id: number, signal?: AbortSignal): Promise<Job> {
  return apiRequest<Job>(`/api/jobs/${id}`, { signal })
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
