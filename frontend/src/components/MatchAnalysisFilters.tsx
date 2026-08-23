import type { MatchAnalysis } from '../types/matchAnalysis'
import type {
  MatchScoreFilterValue,
  ResumeFilterValue,
} from '../utils/matchAnalysisFilters'

interface MatchAnalysisFiltersProps {
  analyses: MatchAnalysis[]
  query: string
  selectedResumeId: ResumeFilterValue
  selectedScore: MatchScoreFilterValue
  resultCount: number
  onQueryChange: (query: string) => void
  onResumeChange: (resumeId: ResumeFilterValue) => void
  onScoreChange: (score: MatchScoreFilterValue) => void
  onClear: () => void
}

const scoreOptions: ReadonlyArray<{ value: MatchScoreFilterValue; label: string }> = [
  { value: 'ALL', label: 'All scores' },
  { value: 'HIGH', label: 'Strong · 75–100' },
  { value: 'MEDIUM', label: 'Moderate · 50–74' },
  { value: 'LOW', label: 'Low · 0–49' },
]

export function MatchAnalysisFilters({
  analyses,
  query,
  selectedResumeId,
  selectedScore,
  resultCount,
  onQueryChange,
  onResumeChange,
  onScoreChange,
  onClear,
}: MatchAnalysisFiltersProps) {
  const resumeOptions = Array.from(
    new Map(analyses.map((analysis) => [analysis.resumeId, analysis.resumeName])).entries(),
  ).sort((left, right) => left[1].localeCompare(right[1]))
  const hasActiveFilters = query.trim() !== ''
    || selectedResumeId !== 'ALL'
    || selectedScore !== 'ALL'

  return (
    <section className="analysis-filters" aria-labelledby="analysis-filters-heading">
      <div className="analysis-filters__heading">
        <div>
          <p>History tools</p>
          <h2 id="analysis-filters-heading">Find an analysis</h2>
        </div>
        <span role="status" aria-live="polite">
          {resultCount} of {analyses.length} shown
        </span>
      </div>

      <div className="analysis-filters__controls">
        <div className="analysis-filters__field analysis-filters__field--search">
          <label htmlFor="analysis-search">Company or job title</label>
          <input
            id="analysis-search"
            type="search"
            value={query}
            placeholder="Search saved analyses…"
            onChange={(event) => onQueryChange(event.target.value)}
          />
        </div>

        <div className="analysis-filters__field">
          <label htmlFor="analysis-resume-filter">Resume</label>
          <select
            id="analysis-resume-filter"
            value={selectedResumeId}
            onChange={(event) => onResumeChange(
              event.target.value === 'ALL' ? 'ALL' : Number(event.target.value),
            )}
          >
            <option value="ALL">All resumes</option>
            {resumeOptions.map(([id, name]) => (
              <option key={id} value={id}>{name}</option>
            ))}
          </select>
        </div>

        <div className="analysis-filters__field">
          <label htmlFor="analysis-score-filter">Match score</label>
          <select
            id="analysis-score-filter"
            value={selectedScore}
            onChange={(event) => onScoreChange(event.target.value as MatchScoreFilterValue)}
          >
            {scoreOptions.map((option) => (
              <option key={option.value} value={option.value}>{option.label}</option>
            ))}
          </select>
        </div>

        <button
          className="button button--secondary button--compact analysis-filters__clear"
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
