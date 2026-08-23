import { useState } from 'react'
import { Link } from 'react-router-dom'
import type { MatchAnalysis } from '../types/matchAnalysis'
import { formatDate } from '../utils/formatDate'

interface MatchAnalysisCardProps {
  analysis: MatchAnalysis
  isDeleting: boolean
  onDelete: (analysis: MatchAnalysis) => Promise<void>
}

export function MatchAnalysisCard({ analysis, isDeleting, onDelete }: MatchAnalysisCardProps) {
  const scoreTone = getScoreTone(analysis.matchScore)
  const [isConfirmingDelete, setIsConfirmingDelete] = useState(false)

  return (
    <article className="analysis-history-card">
      <div className={`analysis-history-card__score analysis-history-card__score--${scoreTone}`}>
        <strong>{analysis.matchScore}</strong>
        <span>/ 100</span>
      </div>

      <div className="analysis-history-card__content">
        <div className="analysis-history-card__heading">
          <div>
            <p>{analysis.company}</p>
            <h2>{analysis.jobTitle}</h2>
          </div>
          <time dateTime={analysis.createdAt}>{formatDate(analysis.createdAt)}</time>
        </div>

        <p className="analysis-history-card__resume">
          <span aria-hidden="true">CV</span> {analysis.resumeName}
        </p>
        <p className="analysis-history-card__summary">{analysis.summary}</p>

        {isConfirmingDelete && (
          <div className="analysis-history-card__delete-confirmation" role="alert">
            <div>
              <strong>Delete this analysis?</strong>
              <p>The saved result will be permanently deleted. Its job and resume will stay intact.</p>
            </div>
            <div>
              <button
                className="button button--secondary button--compact"
                type="button"
                disabled={isDeleting}
                onClick={() => setIsConfirmingDelete(false)}
              >
                Cancel
              </button>
              <button
                className="button button--danger button--compact"
                type="button"
                disabled={isDeleting}
                onClick={() => void onDelete(analysis)}
              >
                {isDeleting ? 'Deleting…' : 'Delete analysis'}
              </button>
            </div>
          </div>
        )}

        <div className="analysis-history-card__footer">
          <span>{analysis.modelName}</span>
          <div className="analysis-history-card__footer-actions">
            <Link to={`/analyses/${analysis.id}`} aria-label={`View analysis for ${analysis.jobTitle}`}>
              View analysis <span aria-hidden="true">→</span>
            </Link>
            <button
              type="button"
              disabled={isDeleting || isConfirmingDelete}
              onClick={() => setIsConfirmingDelete(true)}
            >
              Delete
            </button>
          </div>
        </div>
      </div>
    </article>
  )
}

function getScoreTone(score: number): 'low' | 'medium' | 'high' {
  if (score >= 75) {
    return 'high'
  }
  if (score >= 50) {
    return 'medium'
  }
  return 'low'
}
