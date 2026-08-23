export type DashboardStatsRange = 'LAST_30_DAYS' | 'LAST_90_DAYS' | 'ALL_TIME'

export interface DashboardStats {
  range: DashboardStatsRange
  from: string | null
  to: string
  trackedJobs: number
  applications: number
  interviews: number
  offers: number
  applicationRate: number | null
  interviewRate: number | null
  offerRate: number | null
}
