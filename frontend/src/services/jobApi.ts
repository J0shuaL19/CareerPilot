import type { CreateJobInput, Job } from '../types/job'
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
