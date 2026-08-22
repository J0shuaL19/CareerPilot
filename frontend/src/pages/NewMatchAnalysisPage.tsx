import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { MatchAnalysisForm } from '../components/MatchAnalysisForm'
import { getJobs } from '../services/jobApi'
import { createMatchAnalysis } from '../services/matchAnalysisApi'
import { getResumes } from '../services/resumeApi'
import type { Job } from '../types/job'
import type {
  CreateMatchAnalysisInput,
  MatchAnalysis,
} from '../types/matchAnalysis'
import type { Resume } from '../types/resume'
import { getErrorMessage, isAbortError } from '../utils/errors'

interface NewMatchAnalysisPageProps {
  onCreated: (analysis: MatchAnalysis) => void
}

export function NewMatchAnalysisPage({ onCreated }: NewMatchAnalysisPageProps) {
  const [jobs, setJobs] = useState<Job[]>([])
  const [resumes, setResumes] = useState<Resume[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    const controller = new AbortController()

    async function loadOptions() {
      setIsLoading(true)
      setLoadError(null)

      try {
        const [loadedJobs, loadedResumes] = await Promise.all([
          getJobs(controller.signal),
          getResumes(controller.signal),
        ])
        setJobs(loadedJobs)
        setResumes(loadedResumes)
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

    void loadOptions()
    return () => controller.abort()
  }, [reloadKey])

  async function handleSubmit(input: CreateMatchAnalysisInput) {
    setIsSubmitting(true)
    setSubmitError(null)

    try {
      onCreated(await createMatchAnalysis(input))
    } catch (error) {
      setSubmitError(getErrorMessage(error))
    } finally {
      setIsSubmitting(false)
    }
  }

  const hasJobs = jobs.length > 0
  const hasResumes = resumes.length > 0

  return (
    <div className="page page--analysis">
      <header className="page-header">
        <p className="page-header__eyebrow">Career intelligence</p>
        <h1>Analyze your match</h1>
        <p>
          Compare a saved resume with a role to find the strongest evidence,
          important gaps, and the clearest improvements to make next.
        </p>
      </header>

      {isLoading && (
        <div className="state-card" role="status">
          <div className="spinner" aria-hidden="true" />
          <h2>Preparing your workspace</h2>
          <p>Loading saved jobs and resume versions.</p>
        </div>
      )}

      {!isLoading && loadError && (
        <div className="state-card state-card--error" role="alert">
          <div className="state-card__icon" aria-hidden="true">!</div>
          <h2>We couldn’t load your options</h2>
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

      {!isLoading && !loadError && (!hasJobs || !hasResumes) && (
        <div className="prerequisite-grid">
          <PrerequisiteCard
            isReady={hasJobs}
            label="Step 1"
            title="Save a job"
            description={hasJobs
              ? `${jobs.length} saved ${jobs.length === 1 ? 'job is' : 'jobs are'} ready.`
              : 'Add a job description before starting an analysis.'}
            link="/jobs/new"
            linkLabel={hasJobs ? 'View jobs' : 'Add a job'}
          />
          <PrerequisiteCard
            isReady={hasResumes}
            label="Step 2"
            title="Save a resume"
            description={hasResumes
              ? `${resumes.length} saved ${resumes.length === 1 ? 'resume is' : 'resumes are'} ready.`
              : 'Add a plain-text resume to compare against the role.'}
            link="/resumes/new"
            linkLabel={hasResumes ? 'View resumes' : 'Add a resume'}
          />
        </div>
      )}

      {!isLoading && !loadError && hasJobs && hasResumes && (
        <section className="panel analysis-panel" aria-label="Create match analysis">
          <div className="analysis-panel__heading">
            <span>1</span>
            <div>
              <h2>Choose what to compare</h2>
              <p>Select one role and one resume. Analysis may take a few moments.</p>
            </div>
          </div>
          <MatchAnalysisForm
            jobs={jobs}
            resumes={resumes}
            isSubmitting={isSubmitting}
            formError={submitError}
            onSubmit={handleSubmit}
          />
        </section>
      )}
    </div>
  )
}

interface PrerequisiteCardProps {
  isReady: boolean
  label: string
  title: string
  description: string
  link: string
  linkLabel: string
}

function PrerequisiteCard({
  isReady,
  label,
  title,
  description,
  link,
  linkLabel,
}: PrerequisiteCardProps) {
  return (
    <section className={`prerequisite-card ${isReady ? 'prerequisite-card--ready' : ''}`}>
      <p>{label}</p>
      <div className="prerequisite-card__status" aria-hidden="true">
        {isReady ? '✓' : '+'}
      </div>
      <h2>{title}</h2>
      <p>{description}</p>
      <Link className="button button--secondary" to={link}>
        {linkLabel}
      </Link>
    </section>
  )
}
