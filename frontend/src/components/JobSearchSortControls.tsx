export type JobSortOrder = 'NEWEST' | 'OLDEST'

interface JobSearchSortControlsProps {
  query: string
  sortOrder: JobSortOrder
  resultCount: number
  totalCount: number
  hasActiveFilters: boolean
  onQueryChange: (query: string) => void
  onSortOrderChange: (sortOrder: JobSortOrder) => void
  onReset: () => void
}

export function JobSearchSortControls({
  query,
  sortOrder,
  resultCount,
  totalCount,
  hasActiveFilters,
  onQueryChange,
  onSortOrderChange,
  onReset,
}: JobSearchSortControlsProps) {
  return (
    <section className="job-search-sort" aria-labelledby="job-search-sort-title">
      <div className="job-search-sort__heading">
        <div>
          <p>Find an opportunity</p>
          <h2 id="job-search-sort-title">Search and sort</h2>
        </div>
        <span>{resultCount} of {totalCount} shown</span>
      </div>

      <div className="job-search-sort__controls">
        <div className="job-search-sort__field">
          <label htmlFor="job-search">Search jobs</label>
          <input
            id="job-search"
            type="search"
            value={query}
            placeholder="Search company, role, or description"
            onChange={(event) => onQueryChange(event.target.value)}
          />
        </div>

        <div className="job-search-sort__field">
          <label htmlFor="job-sort">Sort by</label>
          <select
            id="job-sort"
            value={sortOrder}
            onChange={(event) => onSortOrderChange(event.target.value as JobSortOrder)}
          >
            <option value="NEWEST">Newest first</option>
            <option value="OLDEST">Oldest first</option>
          </select>
        </div>

        <button
          className="button button--secondary job-search-sort__reset"
          type="button"
          disabled={!hasActiveFilters}
          onClick={onReset}
        >
          Reset all
        </button>
      </div>
    </section>
  )
}
