import type { CreateResumeInput, Resume, UpdateResumeInput } from '../types/resume'
import { apiRequest } from './apiClient'

export function getResumes(signal?: AbortSignal): Promise<Resume[]> {
  return apiRequest<Resume[]>('/api/resumes', { signal })
}

export function getResume(id: number, signal?: AbortSignal): Promise<Resume> {
  return apiRequest<Resume>(`/api/resumes/${id}`, { signal })
}

export function createResume(input: CreateResumeInput): Promise<Resume> {
  return apiRequest<Resume>('/api/resumes', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(input),
  })
}

export function updateResume(id: number, input: UpdateResumeInput): Promise<Resume> {
  return apiRequest<Resume>(`/api/resumes/${id}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(input),
  })
}

export function deleteResume(id: number): Promise<void> {
  return apiRequest<void>(`/api/resumes/${id}`, {
    method: 'DELETE',
  })
}
