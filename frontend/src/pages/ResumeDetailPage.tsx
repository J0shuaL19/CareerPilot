import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { getResume } from '../services/resumeApi'
import type { CreateResumeInput, Resume } from '../types/resume'
import { getErrorMessage, isAbortError } from '../utils/errors'
import { formatDate } from '../utils/formatDate'
import { NotFoundPage } from './NotFoundPage'

interface ResumeDraftNavigationState {
  resumeDraft: CreateResumeInput
  draftNotice: string
}

export function ResumeDetailPage() {
  const resumeId = Number(useParams().id)
  const hasValidId = Number.isSafeInteger(resumeId) && resumeId > 0
  const navigate = useNavigate()
  const [resume, setResume] = useState<Resume | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    if (!hasValidId) {
      return
    }

    const controller = new AbortController()

    async function loadResume() {
      setIsLoading(true)
      setLoadError(null)

      try {
        setResume(await getResume(resumeId, controller.signal))
      } catch (error) {
        if (!isAbortError(error)) {
          setLoadError(getErrorMessage(error))
        }
      } finally {
        if (!controller.signal.aborted) {
          setIsLoading(false)
        }
      }
    }

    void loadResume()
    return () => controller.abort()
  }, [hasValidId, resumeId, reloadKey])

  if (!hasValidId) {
    return <NotFoundPage />
  }

  function duplicateResume() {
    if (!resume) {
      return
    }

    const state: ResumeDraftNavigationState = {
      resumeDraft: {
        name: `${resume.name.slice(0, 250)} Copy`,
        content: resume.content,
      },
      draftNotice: `A copy of ${resume.name} is ready. Review it before saving the new version.`,
    }
    navigate('/resumes/new', { state })
  }

  return (
    <div className="page page--narrow resume-detail-page">
      {isLoading && (
        <div className="state-card" role="status">
          <div className="spinner" aria-hidden="true" />
          <h2>Loading resume</h2>
          <p>Preparing the complete saved version.</p>
        </div>
      )}

      {!isLoading && loadError && (
        <div className="state-card state-card--error" role="alert">
          <div className="state-card__icon" aria-hidden="true">!</div>
          <h2>We couldn’t load this resume</h2>
          <p>{loadError}</p>
          <button
            className="button button--secondary"
            type="button"
            onClick={() => setReloadKey((key) => key + 1)}
          >
            Try again
          </button>
        </div>
      )}

      {!isLoading && !loadError && resume && (
        <>
          <Link className="resume-detail__back" to="/resumes">← Back to resumes</Link>

          <header className="resume-detail__hero">
            <div className="resume-detail__icon" aria-hidden="true">CV</div>
            <div className="resume-detail__heading">
              <p>Saved resume</p>
              <h1>{resume.name}</h1>
              <div className="resume-detail__metadata">
                <span>Saved {formatDate(resume.createdAt)}</span>
                <span>{resume.content.length.toLocaleString()} characters</span>
              </div>
            </div>
            <div className="resume-detail__actions">
              <button className="button button--secondary" type="button" onClick={duplicateResume}>
                Duplicate as new
              </button>
              <Link className="button button--primary" to={`/resumes/${resume.id}/edit`}>
                Edit resume
              </Link>
            </div>
          </header>

          <article className="resume-document" aria-labelledby="resume-content-title">
            <div className="resume-document__heading">
              <div>
                <p>Full text</p>
                <h2 id="resume-content-title">Resume content</h2>
              </div>
              <span>Plain-text preview</span>
            </div>
            <div className="resume-document__content">{resume.content}</div>
          </article>
        </>
      )}
    </div>
  )
}
