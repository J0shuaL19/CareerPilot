import { Link } from 'react-router-dom'
import type { MatchAnalysis } from '../types/matchAnalysis'
import { formatDate } from '../utils/formatDate'

interface MatchAnalysisCardProps {
  analysis: MatchAnalysis
}

export function MatchAnalysisCard({ analysis }: MatchAnalysisCardProps) {
  const scoreTone = getScoreTone(analysis.matchScore)

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

        <div className="analysis-history-card__footer">
          <span>{analysis.modelName}</span>
          <Link to={`/analyses/${analysis.id}`} aria-label={`View analysis for ${analysis.jobTitle}`}>
            View analysis <span aria-hidden="true">→</span>
          </Link>
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
