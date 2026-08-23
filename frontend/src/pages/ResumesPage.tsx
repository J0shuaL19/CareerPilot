import { useEffect, useState } from 'react'
import { ResumeCard } from '../components/ResumeCard'
import {
  ResumeLibraryFilters,
  type ResumeSortOrder,
} from '../components/ResumeLibraryFilters'
import { deleteResume, getResumes } from '../services/resumeApi'
import type { Resume } from '../types/resume'
import { getErrorMessage, isAbortError } from '../utils/errors'

interface ResumesPageProps {
  notice?: string
  onAddResume: () => void
  onViewResume: (resumeId: number) => void
  onEditResume: (resumeId: number) => void
}

export function ResumesPage({
  notice,
  onAddResume,
  onViewResume,
  onEditResume,
}: ResumesPageProps) {
  const [resumes, setResumes] = useState<Resume[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [actionNotice, setActionNotice] = useState<string | null>(null)
  const [showRouteNotice, setShowRouteNotice] = useState(Boolean(notice))
  const [deletingResumeId, setDeletingResumeId] = useState<number | null>(null)
  const [query, setQuery] = useState('')
  const [sortOrder, setSortOrder] = useState<ResumeSortOrder>('NEWEST')
  const [reloadKey, setReloadKey] = useState(0)

  const normalizedQuery = query.trim().toLocaleLowerCase()
  const visibleResumes = resumes
    .filter((resume) => normalizedQuery === '' || [resume.name, resume.content]
      .some((value) => value.toLocaleLowerCase().includes(normalizedQuery)))
    .sort((left, right) => {
      if (sortOrder === 'NAME_ASC') {
        return left.name.localeCompare(right.name, undefined, { sensitivity: 'base' })
      }

      const dateComparison = left.createdAt.localeCompare(right.createdAt)
      return sortOrder === 'OLDEST' ? dateComparison : -dateComparison
    })

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

  async function handleDelete(resume: Resume) {
    setDeletingResumeId(resume.id)
    setActionError(null)
    setActionNotice(null)
    setShowRouteNotice(false)

    try {
      await deleteResume(resume.id)
      setResumes((current) => current.filter((item) => item.id !== resume.id))
      setActionNotice(`${resume.name} was deleted.`)
    } catch (deleteError) {
      setActionError(getErrorMessage(deleteError))
    } finally {
      setDeletingResumeId(null)
    }
  }

  function handleResetFilters() {
    setQuery('')
    setSortOrder('NEWEST')
  }

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

      {showRouteNotice && notice && (
        <div className="alert alert--success" role="status">
          <span aria-hidden="true">✓</span> {notice}
        </div>
      )}

      {actionNotice && (
        <div className="alert alert--success" role="status">
          <span aria-hidden="true">✓</span> {actionNotice}
        </div>
      )}

      {actionError && (
        <div className="alert alert--error resume-action-error" role="alert">
          {actionError}
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
        <>
          <ResumeLibraryFilters
            query={query}
            sortOrder={sortOrder}
            resultCount={visibleResumes.length}
            totalCount={resumes.length}
            onQueryChange={setQuery}
            onSortOrderChange={setSortOrder}
            onReset={handleResetFilters}
          />

          {visibleResumes.length > 0 ? (
            <section className="resume-list" aria-label="Filtered saved resumes">
              {visibleResumes.map((resume) => (
                <ResumeCard
                  key={resume.id}
                  resume={resume}
                  isDeleting={deletingResumeId === resume.id}
                  onView={onViewResume}
                  onEdit={onEditResume}
                  onDelete={handleDelete}
                />
              ))}
            </section>
          ) : (
            <div className="state-card state-card--compact">
              <div className="state-card__icon" aria-hidden="true">⌕</div>
              <h2>No resumes match</h2>
              <p>Try a different name or keyword from the resume text.</p>
              <button
                className="button button--secondary"
                type="button"
                onClick={handleResetFilters}
              >
                Reset search
              </button>
            </div>
          )}
        </>
      )}
    </div>
  )
}
