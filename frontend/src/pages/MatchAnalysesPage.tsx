import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { MatchAnalysisFilters } from '../components/MatchAnalysisFilters'
import { MatchAnalysisCard } from '../components/MatchAnalysisCard'
import { deleteMatchAnalysis, getMatchAnalyses } from '../services/matchAnalysisApi'
import type { MatchAnalysis } from '../types/matchAnalysis'
import { getErrorMessage, isAbortError } from '../utils/errors'
import {
  matchesScoreFilter,
  type MatchScoreFilterValue,
  type ResumeFilterValue,
} from '../utils/matchAnalysisFilters'

export function MatchAnalysesPage() {
  const [analyses, setAnalyses] = useState<MatchAnalysis[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [actionNotice, setActionNotice] = useState<string | null>(null)
  const [deletingAnalysisId, setDeletingAnalysisId] = useState<number | null>(null)
  const [query, setQuery] = useState('')
  const [selectedResumeId, setSelectedResumeId] = useState<ResumeFilterValue>('ALL')
  const [selectedScore, setSelectedScore] = useState<MatchScoreFilterValue>('ALL')
  const [reloadKey, setReloadKey] = useState(0)

  const normalizedQuery = query.trim().toLocaleLowerCase()
  const filteredAnalyses = analyses.filter((analysis) => {
    const matchesQuery = normalizedQuery === '' || [analysis.company, analysis.jobTitle]
      .some((value) => value.toLocaleLowerCase().includes(normalizedQuery))
    const matchesResume = selectedResumeId === 'ALL'
      || analysis.resumeId === selectedResumeId

    return matchesQuery
      && matchesResume
      && matchesScoreFilter(analysis.matchScore, selectedScore)
  })

  useEffect(() => {
    const controller = new AbortController()

    async function loadAnalyses() {
      setIsLoading(true)
      setError(null)

      try {
        setAnalyses(await getMatchAnalyses(controller.signal))
      } catch (loadError) {
        if (!isAbortError(loadError)) {
          setError(getErrorMessage(loadError))
        }
      } finally {
        if (!controller.signal.aborted) {
          setIsLoading(false)
        }
      }
    }

    void loadAnalyses()
    return () => controller.abort()
  }, [reloadKey])

  function handleClearFilters() {
    setQuery('')
    setSelectedResumeId('ALL')
    setSelectedScore('ALL')
  }

  async function handleDelete(analysis: MatchAnalysis) {
    setDeletingAnalysisId(analysis.id)
    setActionError(null)
    setActionNotice(null)

    try {
      await deleteMatchAnalysis(analysis.id)
      const remainingAnalyses = analyses.filter((item) => item.id !== analysis.id)
      setAnalyses(remainingAnalyses)
      if (
        selectedResumeId !== 'ALL'
        && !remainingAnalyses.some((item) => item.resumeId === selectedResumeId)
      ) {
        setSelectedResumeId('ALL')
      }
      setActionNotice(`Analysis for ${analysis.jobTitle} was deleted.`)
    } catch (deleteError) {
      setActionError(getErrorMessage(deleteError))
    } finally {
      setDeletingAnalysisId(null)
    }
  }

  return (
    <div className="page">
      <header className="page-header page-header--row">
        <div>
          <p className="page-header__eyebrow">Career intelligence</p>
          <h1>Match analyses</h1>
          <p>
            {analyses.length === 0
              ? 'Compare a role and resume to build your first focused action plan.'
              : `${analyses.length} saved ${analyses.length === 1 ? 'analysis' : 'analyses'} ready to revisit.`}
          </p>
        </div>
        <Link className="button button--primary" to="/analyses/new">
          <span aria-hidden="true">＋</span> New analysis
        </Link>
      </header>

      {actionNotice && (
        <div className="alert alert--success" role="status">
          <span aria-hidden="true">✓</span> {actionNotice}
        </div>
      )}

      {actionError && (
        <div className="alert alert--error analysis-action-error" role="alert">
          {actionError}
        </div>
      )}

      {isLoading && (
        <div className="state-card" role="status">
          <div className="spinner" aria-hidden="true" />
          <h2>Loading your analyses</h2>
          <p>Gathering your saved match results.</p>
        </div>
      )}

      {!isLoading && error && (
        <div className="state-card state-card--error" role="alert">
          <div className="state-card__icon" aria-hidden="true">!</div>
          <h2>We couldn’t load your analyses</h2>
          <p>{error}</p>
          <button
            className="button button--secondary"
            type="button"
            onClick={() => setReloadKey((key) => key + 1)}
          >
            Try again
          </button>
        </div>
      )}

      {!isLoading && !error && analyses.length === 0 && (
        <div className="state-card">
          <div className="state-card__icon" aria-hidden="true">◎</div>
          <h2>No analyses yet</h2>
          <p>Choose a saved job and resume to uncover strengths, gaps, and next steps.</p>
          <Link className="button button--primary" to="/analyses/new">
            Run your first analysis
          </Link>
        </div>
      )}

      {!isLoading && !error && analyses.length > 0 && (
        <>
          <MatchAnalysisFilters
            analyses={analyses}
            query={query}
            selectedResumeId={selectedResumeId}
            selectedScore={selectedScore}
            resultCount={filteredAnalyses.length}
            onQueryChange={setQuery}
            onResumeChange={setSelectedResumeId}
            onScoreChange={setSelectedScore}
            onClear={handleClearFilters}
          />

          {filteredAnalyses.length > 0 ? (
            <section className="analysis-history" aria-label="Filtered match analyses">
              {filteredAnalyses.map((analysis) => (
                <MatchAnalysisCard
                  key={analysis.id}
                  analysis={analysis}
                  isDeleting={deletingAnalysisId === analysis.id}
                  onDelete={handleDelete}
                />
              ))}
            </section>
          ) : (
            <div className="state-card state-card--compact">
              <div className="state-card__icon" aria-hidden="true">⌕</div>
              <h2>No analyses match</h2>
              <p>Try a different company, role, resume, or score range.</p>
              <button
                className="button button--secondary"
                type="button"
                onClick={handleClearFilters}
              >
                Clear filters
              </button>
            </div>
          )}
        </>
      )}
    </div>
  )
}
