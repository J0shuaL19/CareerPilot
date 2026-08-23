import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { MatchAnalysisCard } from '../components/MatchAnalysisCard'
import { deleteMatchAnalysis, getMatchAnalyses } from '../services/matchAnalysisApi'
import type { MatchAnalysis } from '../types/matchAnalysis'
import { getErrorMessage, isAbortError } from '../utils/errors'

export function MatchAnalysesPage() {
  const [analyses, setAnalyses] = useState<MatchAnalysis[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [actionNotice, setActionNotice] = useState<string | null>(null)
  const [deletingAnalysisId, setDeletingAnalysisId] = useState<number | null>(null)
  const [reloadKey, setReloadKey] = useState(0)

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

  async function handleDelete(analysis: MatchAnalysis) {
    setDeletingAnalysisId(analysis.id)
    setActionError(null)
    setActionNotice(null)

    try {
      await deleteMatchAnalysis(analysis.id)
      setAnalyses((current) => current.filter((item) => item.id !== analysis.id))
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
        <section className="analysis-history" aria-label="Saved match analyses">
          {analyses.map((analysis) => (
            <MatchAnalysisCard
              key={analysis.id}
              analysis={analysis}
              isDeleting={deletingAnalysisId === analysis.id}
              onDelete={handleDelete}
            />
          ))}
        </section>
      )}
    </div>
  )
}
