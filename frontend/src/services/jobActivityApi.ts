import type {
  CompleteJobActivityInput,
  CreateJobActivityInput,
  JobAttentionHistoryEntry,
  JobAttentionItem,
  JobAttentionSettings,
  JobActivity,
  ReopenJobActivityResult,
  RescheduleJobActivityInput,
  ScheduledJobActivity,
  UpcomingJobActivity,
  UpdateJobActivityInput,
} from '../types/jobActivity'
import { apiDownload, apiRequest } from './apiClient'

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

export function updateJobActivity(
  jobId: number,
  activityId: number,
  input: UpdateJobActivityInput,
): Promise<JobActivity> {
  return apiRequest<JobActivity>(`/api/jobs/${jobId}/activities/${activityId}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(input),
  })
}

export function completeJobActivity(
  jobId: number,
  activityId: number,
  input: CompleteJobActivityInput,
): Promise<JobActivity> {
  return apiRequest<JobActivity>('/api/jobs/' + jobId + '/activities/' + activityId + '/complete', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(input),
  })
}

export function rescheduleJobActivity(
  jobId: number,
  activityId: number,
  input: RescheduleJobActivityInput,
): Promise<JobActivity> {
  return apiRequest<JobActivity>(
    '/api/jobs/' + jobId + '/activities/' + activityId + '/reschedule',
    {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(input),
    },
  )
}

export function reopenJobActivity(
  jobId: number,
  activityId: number,
): Promise<ReopenJobActivityResult> {
  return apiRequest<ReopenJobActivityResult>(
    '/api/jobs/' + jobId + '/activities/' + activityId + '/reopen',
    { method: 'PUT' },
  )
}

export function deleteJobActivity(jobId: number, activityId: number): Promise<void> {
  return apiRequest<void>(`/api/jobs/${jobId}/activities/${activityId}`, {
    method: 'DELETE',
  })
}

export function downloadJobActivityCalendar(
  jobId: number,
  activityId: number,
): Promise<Blob> {
  return apiDownload(`/api/jobs/${jobId}/activities/${activityId}/calendar`)
}

export function getOverdueJobActivities(signal?: AbortSignal): Promise<UpcomingJobActivity[]> {
  return apiRequest<UpcomingJobActivity[]>('/api/job-activities/overdue', { signal })
}

export function getUpcomingJobActivities(signal?: AbortSignal): Promise<UpcomingJobActivity[]> {
  return apiRequest<UpcomingJobActivity[]>('/api/job-activities/upcoming', { signal })
}

export function getCalendarJobActivities(
  start: string,
  end: string,
  signal?: AbortSignal,
): Promise<ScheduledJobActivity[]> {
  const query = new URLSearchParams({ start, end })
  return apiRequest<ScheduledJobActivity[]>('/api/job-activities/calendar?' + query, { signal })
}

export function getJobAttentionHistory(
  signal?: AbortSignal,
): Promise<JobAttentionHistoryEntry[]> {
  return apiRequest<JobAttentionHistoryEntry[]>('/api/job-activities/attention-history', {
    signal,
  })
}

export function getJobAttentionItems(signal?: AbortSignal): Promise<JobAttentionItem[]> {
  return apiRequest<JobAttentionItem[]>('/api/job-activities/needs-attention', { signal })
}
export function getJobAttentionSettings(signal?: AbortSignal): Promise<JobAttentionSettings> {
  return apiRequest<JobAttentionSettings>('/api/job-activities/attention-settings', { signal })
}

export function updateJobAttentionSettings(
  input: JobAttentionSettings,
): Promise<JobAttentionSettings> {
  return apiRequest<JobAttentionSettings>('/api/job-activities/attention-settings', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(input),
  })
}
