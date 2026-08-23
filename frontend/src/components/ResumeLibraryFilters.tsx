export type ResumeSortOrder = 'NEWEST' | 'OLDEST' | 'NAME_ASC'

interface ResumeLibraryFiltersProps {
  query: string
  sortOrder: ResumeSortOrder
  resultCount: number
  totalCount: number
  onQueryChange: (query: string) => void
  onSortOrderChange: (sortOrder: ResumeSortOrder) => void
  onReset: () => void
}

export function ResumeLibraryFilters({
  query,
  sortOrder,
  resultCount,
  totalCount,
  onQueryChange,
  onSortOrderChange,
  onReset,
}: ResumeLibraryFiltersProps) {
  const hasActiveFilters = query.trim() !== '' || sortOrder !== 'NEWEST'

  return (
    <section className="resume-filters" aria-labelledby="resume-filters-title">
      <div className="resume-filters__heading">
        <div>
          <p>Find a version</p>
          <h2 id="resume-filters-title">Search and sort</h2>
        </div>
        <span>{resultCount} of {totalCount} shown</span>
      </div>

      <div className="resume-filters__controls">
        <div className="resume-filters__field">
          <label htmlFor="resume-search">Search resumes</label>
          <input
            id="resume-search"
            type="search"
            value={query}
            placeholder="Search name or resume text"
            onChange={(event) => onQueryChange(event.target.value)}
          />
        </div>

        <div className="resume-filters__field">
          <label htmlFor="resume-sort">Sort by</label>
          <select
            id="resume-sort"
            value={sortOrder}
            onChange={(event) => onSortOrderChange(event.target.value as ResumeSortOrder)}
          >
            <option value="NEWEST">Newest first</option>
            <option value="OLDEST">Oldest first</option>
            <option value="NAME_ASC">Name A–Z</option>
          </select>
        </div>

        <button
          className="button button--secondary resume-filters__reset"
          type="button"
          disabled={!hasActiveFilters}
          onClick={onReset}
        >
          Reset
        </button>
      </div>
    </section>
  )
}
