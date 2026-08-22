import { useEffect, useState } from 'react'
import { ResumeCard } from '../components/ResumeCard'
import { getResumes } from '../services/resumeApi'
import type { Resume } from '../types/resume'
import { getErrorMessage, isAbortError } from '../utils/errors'

interface ResumesPageProps {
  notice?: string
  onAddResume: () => void
}

export function ResumesPage({ notice, onAddResume }: ResumesPageProps) {
  const [resumes, setResumes] = useState<Resume[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    const controller = new AbortController()

    async function loadResumes() {
      setIsLoading(true)
      setError(null)

      try {
        setResumes(await getResumes(controller.signal))
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

    void loadResumes()
    return () => controller.abort()
  }, [reloadKey])

  return (
    <div className="page">
      <header className="page-header page-header--row">
        <div>
          <p className="page-header__eyebrow">Resume library</p>
          <h1>Your resumes</h1>
          <p>
            {resumes.length === 0
              ? 'Save a resume to prepare for tailored job matching.'
              : `${resumes.length} saved ${resumes.length === 1 ? 'resume' : 'resumes'} ready for matching.`}
          </p>
        </div>
        <button className="button button--primary" type="button" onClick={onAddResume}>
          <span aria-hidden="true">＋</span> Add resume
        </button>
      </header>

      {notice && (
        <div className="alert alert--success" role="status">
          <span aria-hidden="true">✓</span> {notice}
        </div>
      )}

      {isLoading && (
        <div className="state-card" role="status">
          <div className="spinner" aria-hidden="true" />
          <h2>Loading your resumes</h2>
          <p>Gathering the latest versions in your library.</p>
        </div>
      )}

      {!isLoading && error && (
        <div className="state-card state-card--error" role="alert">
          <div className="state-card__icon" aria-hidden="true">!</div>
          <h2>We couldn’t load your resumes</h2>
          <p>{error}</p>
          <button className="button button--secondary" type="button" onClick={() => setReloadKey((key) => key + 1)}>
            Try again
          </button>
        </div>
      )}

      {!isLoading && !error && resumes.length === 0 && (
        <div className="state-card">
          <div className="state-card__icon" aria-hidden="true">CV</div>
          <h2>No resumes saved yet</h2>
          <p>Add your first resume so future job analysis has a strong starting point.</p>
          <button className="button button--primary" type="button" onClick={onAddResume}>
            Add your first resume
          </button>
        </div>
      )}

      {!isLoading && !error && resumes.length > 0 && (
        <section className="resume-list" aria-label="Saved resumes">
          {resumes.map((resume) => (
            <ResumeCard key={resume.id} resume={resume} />
          ))}
        </section>
      )}
    </div>
  )
}
