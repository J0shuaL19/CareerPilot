import type { Resume } from '../types/resume'
import { formatDate } from '../utils/formatDate'

interface ResumeCardProps {
  resume: Resume
}

export function ResumeCard({ resume }: ResumeCardProps) {
  return (
    <article className="resume-card">
      <div className="resume-card__icon" aria-hidden="true">
        <span>CV</span>
      </div>

      <div className="resume-card__content">
        <div className="resume-card__heading">
          <div>
            <p className="resume-card__label">Saved resume</p>
            <h2>{resume.name}</h2>
          </div>
          <time dateTime={resume.createdAt}>{formatDate(resume.createdAt)}</time>
        </div>

        <p className="resume-card__preview">{resume.content}</p>
      </div>
    </article>
  )
}
