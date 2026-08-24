import type { Job } from '../types/job'

export type CalendarActivityTypeFilter = 'ALL' | 'INTERVIEW' | 'FOLLOW_UP'
export type CalendarActivityStatusFilter = 'ALL' | 'SCHEDULED' | 'COMPLETED'

interface CalendarFiltersProps {
  searchQuery: string
  typeFilter: CalendarActivityTypeFilter
  statusFilter: CalendarActivityStatusFilter
  jobIdFilter: number | null
  jobs: Job[]
  shownCount: number
  totalCount: number
  hasActiveFilters: boolean
  onSearchChange: (value: string) => void
  onTypeChange: (value: CalendarActivityTypeFilter) => void
  onStatusChange: (value: CalendarActivityStatusFilter) => void
  onJobChange: (value: number | null) => void
  onClear: () => void
}

export function CalendarFilters({
  searchQuery,
  typeFilter,
  statusFilter,
  jobIdFilter,
  jobs,
  shownCount,
  totalCount,
  hasActiveFilters,
  onSearchChange,
  onTypeChange,
  onStatusChange,
  onJobChange,
  onClear,
}: CalendarFiltersProps) {
  const sortedJobs = [...jobs].sort((left, right) => (
    left.company.localeCompare(right.company) || left.title.localeCompare(right.title)
  ))

  return (
    <section className="calendar-filters" aria-labelledby="calendar-filters-heading">
      <div className="calendar-filters__heading">
        <div>
          <p>Focus view</p>
          <h3 id="calendar-filters-heading">Find activities</h3>
        </div>
        <span role="status" aria-live="polite">
          Showing {shownCount} of {totalCount} {totalCount === 1 ? 'activity' : 'activities'}
        </span>
      </div>

      <div className="calendar-filters__controls">
        <label className="calendar-filters__field calendar-filters__field--search">
          <span>Search</span>
          <span className="calendar-filters__search-input">
            <span aria-hidden="true">⌕</span>
            <input
              type="search"
              value={searchQuery}
              placeholder="Activity, company, role, or contact"
              onChange={(event) => onSearchChange(event.target.value)}
            />
          </span>
        </label>

        <label className="calendar-filters__field">
          <span>Type</span>
          <select
            value={typeFilter}
            onChange={(event) => onTypeChange(event.target.value as CalendarActivityTypeFilter)}
          >
            <option value="ALL">All types</option>
            <option value="INTERVIEW">Interviews</option>
            <option value="FOLLOW_UP">Follow-ups</option>
          </select>
        </label>

        <label className="calendar-filters__field">
          <span>Status</span>
          <select
            value={statusFilter}
            onChange={(event) => onStatusChange(event.target.value as CalendarActivityStatusFilter)}
          >
            <option value="ALL">All statuses</option>
            <option value="SCHEDULED">Scheduled</option>
            <option value="COMPLETED">Completed</option>
          </select>
        </label>

        <label className="calendar-filters__field">
          <span>Job</span>
          <select
            value={jobIdFilter ?? ''}
            onChange={(event) => onJobChange(event.target.value ? Number(event.target.value) : null)}
          >
            <option value="">All jobs</option>
            {sortedJobs.map((job) => (
              <option key={job.id} value={job.id}>{job.company} · {job.title}</option>
            ))}
          </select>
        </label>

        <button
          className="button button--secondary calendar-filters__clear"
          type="button"
          disabled={!hasActiveFilters}
          onClick={onClear}
        >
          Clear filters
        </button>
      </div>
    </section>
  )
}
