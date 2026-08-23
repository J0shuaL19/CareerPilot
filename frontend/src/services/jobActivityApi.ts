import type {
  CreateJobActivityInput,
  JobActivity,
  UpcomingJobActivity,
} from '../types/jobActivity'
import { apiRequest } from './apiClient'

export function getJobActivities(
  jobId: number,
  signal?: AbortSignal,
): Promise<JobActivity[]> {
  return apiRequest<JobActivity[]>(`/api/jobs/${jobId}/activities`, { signal })
}

export function createJobActivity(
  jobId: number,
  input: CreateJobActivityInput,
): Promise<JobActivity> {
  return apiRequest<JobActivity>(`/api/jobs/${jobId}/activities`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(input),
  })
}

export function deleteJobActivity(jobId: number, activityId: number): Promise<void> {
  return apiRequest<void>(`/api/jobs/${jobId}/activities/${activityId}`, {
    method: 'DELETE',
  })
}

export function getUpcomingJobActivities(signal?: AbortSignal): Promise<UpcomingJobActivity[]> {
  return apiRequest<UpcomingJobActivity[]>('/api/job-activities/upcoming', { signal })
}
