import type { DashboardStats, DashboardStatsRange } from '../types/dashboard'

interface DashboardFunnelProps {
  stats: DashboardStats | null
  range: DashboardStatsRange
  isLoading: boolean
  error: string | null
  onRangeChange: (range: DashboardStatsRange) => void
  onRetry: () => void
}

const rangeOptions: Array<{ value: DashboardStatsRange; label: string }> = [
  { value: 'LAST_30_DAYS', label: '30 days' },
  { value: 'LAST_90_DAYS', label: '90 days' },
  { value: 'ALL_TIME', label: 'All time' },
]

export function DashboardFunnel({
  stats,
  range,
  isLoading,
  error,
  onRangeChange,
  onRetry,
}: DashboardFunnelProps) {
  const stages = stats === null ? [] : [
    {
      label: 'Tracked',
      value: stats.trackedJobs,
      rate: stats.trackedJobs === 0 ? null : 100,
      detail: 'Starting opportunities',
      tone: 'tracked',
    },
    {
      label: 'Applied',
      value: stats.applications,
      rate: stats.applicationRate,
      detail: 'of tracked jobs',
      tone: 'applied',
    },
    {
      label: 'Interviewed',
      value: stats.interviews,
      rate: stats.interviewRate,
      detail: 'of applications',
      tone: 'interview',
    },
    {
      label: 'Offers',
      value: stats.offers,
      rate: stats.offerRate,
      detail: 'of interviews',
      tone: 'offer',
    },
  ] as const

  return (
    <section className="dashboard-funnel" aria-labelledby="dashboard-funnel-heading">
      <div className="dashboard-funnel__heading">
        <div>
          <p>Conversion insights</p>
          <h2 id="dashboard-funnel-heading">Application funnel</h2>
          <span>Jobs are grouped by when they were first tracked.</span>
        </div>
        <div className="dashboard-funnel__ranges" aria-label="Funnel time range">
          {rangeOptions.map((option) => (
            <button
              type="button"
              key={option.value}
              aria-pressed={range === option.value}
              className={range === option.value ? 'is-active' : undefined}
              onClick={() => onRangeChange(option.value)}
            >
              {option.label}
            </button>
          ))}
        </div>
      </div>

      {isLoading && (
        <div className="dashboard-funnel__state" role="status">
          <div className="spinner" aria-hidden="true" />
          <span>Calculating conversion rates…</span>
        </div>
      )}

      {!isLoading && error && (
        <div className="dashboard-funnel__state dashboard-funnel__state--error" role="alert">
          <span>{error}</span>
          <button type="button" onClick={onRetry}>Try again</button>
        </div>
      )}

      {!isLoading && !error && stats && (
        <div className="dashboard-funnel__stages" aria-live="polite">
          {stages.map((stage, index) => (
            <div className="dashboard-funnel__stage-wrap" key={stage.label}>
              {index > 0 && <span className="dashboard-funnel__arrow" aria-hidden="true">→</span>}
              <article className={`dashboard-funnel__stage dashboard-funnel__stage--${stage.tone}`}>
                <span>{stage.label}</span>
                <strong>{stage.value}</strong>
                <p>
                  <b>{stage.rate === null ? '—' : `${stage.rate}%`}</b>
                  {' '}{stage.detail}
                </p>
              </article>
            </div>
          ))}
        </div>
      )}
    </section>
  )
}
