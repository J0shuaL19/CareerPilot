import type { ApiErrorResponse } from '../types/api'

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '')

export class ApiError extends Error {
  readonly status: number
  readonly fieldErrors: Record<string, string>

  constructor(response: ApiErrorResponse) {
    super(response.message)
    this.name = 'ApiError'
    this.status = response.status
    this.fieldErrors = response.fieldErrors
  }
}

export async function apiRequest<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const response = await fetch(`${apiBaseUrl}${path}`, {
    ...options,
    headers: {
      Accept: 'application/json',
      ...options.headers,
    },
  })

  if (!response.ok) {
    throw await createApiError(response)
  }

  return (await response.json()) as T
}

async function createApiError(response: Response): Promise<ApiError> {
  try {
    const errorResponse = (await response.json()) as ApiErrorResponse
    return new ApiError(errorResponse)
  } catch {
    return new ApiError({
      timestamp: new Date().toISOString(),
      status: response.status,
      error: response.statusText,
      message: 'The server returned an unexpected response.',
      path: response.url,
      fieldErrors: {},
    })
  }
}
