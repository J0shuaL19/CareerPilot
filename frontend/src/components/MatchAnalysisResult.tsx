import type { MatchAnalysis } from '../types/matchAnalysis'
import { formatDate } from '../utils/formatDate'

interface MatchAnalysisResultProps {
  analysis: MatchAnalysis
}

export function MatchAnalysisResult({ analysis }: MatchAnalysisResultProps) {
  return (
    <article className="analysis-result">
      <section className="analysis-result__hero" aria-labelledby="analysis-summary-title">
        <div
          className="score-card"
          aria-label={`Match score ${analysis.matchScore} out of 100`}
        >
          <span className="score-card__value">{analysis.matchScore}</span>
          <span className="score-card__scale">/ 100</span>
          <span className="score-card__label">Match score</span>
        </div>

        <div className="analysis-result__summary">
          <p className="analysis-result__eyebrow">AI match analysis</p>
          <h1 id="analysis-summary-title">{analysis.jobTitle}</h1>
          <p className="analysis-result__context">
            {analysis.company} <span aria-hidden="true">·</span> {analysis.resumeName}
          </p>
          <p className="analysis-result__summary-text">{analysis.summary}</p>
          <p className="analysis-result__meta">
            Analyzed {formatDate(analysis.createdAt)} with {analysis.modelName}
          </p>
        </div>
      </section>

      <div className="analysis-result__grid">
        <ResultSection
          className="result-section--strengths"
          eyebrow="Strong signals"
          title="Strengths"
          content={analysis.strengths}
          icon="✓"
        />
        <ResultSection
          className="result-section--gaps"
          eyebrow="Missing evidence"
          title="Gaps"
          content={analysis.gaps}
          icon="△"
        />
      </div>

      <ResultSection
        className="result-section--recommendations result-section--wide"
        eyebrow="Next best actions"
        title="Recommendations"
        content={analysis.recommendations}
        icon="→"
      />
    </article>
  )
}

interface ResultSectionProps {
  className: string
  eyebrow: string
  title: string
  content: string
  icon: string
}

function ResultSection({
  className,
  eyebrow,
  title,
  content,
  icon,
}: ResultSectionProps) {
  return (
    <section className={`result-section ${className}`}>
      <div className="result-section__icon" aria-hidden="true">{icon}</div>
      <div>
        <p className="result-section__eyebrow">{eyebrow}</p>
        <h2>{title}</h2>
        <p>{content}</p>
      </div>
    </section>
  )
}
