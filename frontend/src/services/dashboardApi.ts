import type { DashboardStats, DashboardStatsRange } from '../types/dashboard'
import { apiRequest } from './apiClient'

export function getDashboardStats(
  range: DashboardStatsRange,
  signal?: AbortSignal,
): Promise<DashboardStats> {
  return apiRequest<DashboardStats>(
    `/api/dashboard/stats?range=${encodeURIComponent(range)}`,
    { signal },
  )
}
