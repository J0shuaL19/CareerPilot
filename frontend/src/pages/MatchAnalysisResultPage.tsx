import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { MatchAnalysisResult } from '../components/MatchAnalysisResult'
import { getMatchAnalysis } from '../services/matchAnalysisApi'
import type { MatchAnalysis } from '../types/matchAnalysis'
import { getErrorMessage, isAbortError } from '../utils/errors'
import { NotFoundPage } from './NotFoundPage'

export function MatchAnalysisResultPage() {
  const { id } = useParams()
  const analysisId = Number(id)
  const [analysis, setAnalysis] = useState<MatchAnalysis | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [reloadKey, setReloadKey] = useState(0)

  const hasValidId = Number.isSafeInteger(analysisId) && analysisId > 0

  useEffect(() => {
    if (!hasValidId) {
      return
    }

    const controller = new AbortController()

    async function loadAnalysis() {
      setIsLoading(true)
      setError(null)

      try {
        setAnalysis(await getMatchAnalysis(analysisId, controller.signal))
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

    void loadAnalysis()
    return () => controller.abort()
  }, [analysisId, hasValidId, reloadKey])

  if (!hasValidId) {
    return <NotFoundPage />
  }

  return (
    <div className="page page--analysis-result">
      {isLoading && (
        <div className="state-card" role="status">
          <div className="spinner" aria-hidden="true" />
          <h2>Loading your analysis</h2>
          <p>Bringing back the saved match details.</p>
        </div>
      )}

      {!isLoading && error && (
        <div className="state-card state-card--error" role="alert">
          <div className="state-card__icon" aria-hidden="true">!</div>
          <h2>We couldn’t load this analysis</h2>
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

      {!isLoading && !error && analysis && (
        <>
          <MatchAnalysisResult analysis={analysis} />
          <div className="analysis-result__actions">
            <Link className="button button--primary" to="/analyses/new">
              Run another analysis
            </Link>
            <Link className="button button--secondary" to="/jobs">
              Back to jobs
            </Link>
          </div>
        </>
      )}
    </div>
  )
}
