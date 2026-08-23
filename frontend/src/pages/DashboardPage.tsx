import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { PipelineSummary } from '../components/PipelineSummary'
import { UpcomingActivities } from '../components/UpcomingActivities'
import { getUpcomingJobActivities } from '../services/jobActivityApi'
import { getJobs } from '../services/jobApi'
import { getMatchAnalyses } from '../services/matchAnalysisApi'
import { getResumes } from '../services/resumeApi'
import type { Job } from '../types/job'
import type { UpcomingJobActivity } from '../types/jobActivity'
import type { MatchAnalysis } from '../types/matchAnalysis'
import type { Resume } from '../types/resume'
import { getErrorMessage, isAbortError } from '../utils/errors'
import { formatDate } from '../utils/formatDate'

interface DashboardData {
  jobs: Job[]
  resumes: Resume[]
  analyses: MatchAnalysis[]
  upcomingActivities: UpcomingJobActivity[]
}

const emptyData: DashboardData = {
  jobs: [],
  resumes: [],
  analyses: [],
  upcomingActivities: [],
}

export function DashboardPage() {
  const navigate = useNavigate()
  const [data, setData] = useState<DashboardData>(emptyData)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    const controller = new AbortController()

    async function loadDashboard() {
      setIsLoading(true)
      setError(null)

      try {
        const [jobs, resumes, analyses, upcomingActivities] = await Promise.all([
          getJobs(controller.signal),
          getResumes(controller.signal),
          getMatchAnalyses(controller.signal),
          getUpcomingJobActivities(controller.signal),
        ])
        setData({ jobs, resumes, analyses, upcomingActivities })
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

    void loadDashboard()
    return () => controller.abort()
  }, [reloadKey])

  const activeJobs = data.jobs.filter(({ status }) => (
    ['APPLIED', 'OA', 'INTERVIEW'].includes(status)
  )).length
  const averageScore = data.analyses.length === 0
    ? null
    : Math.round(
        data.analyses.reduce((total, analysis) => total + analysis.matchScore, 0)
          / data.analyses.length,
      )
  const metrics = [
    {
      label: 'Jobs tracked',
      value: data.jobs.length,
      detail: `${activeJobs} currently in progress`,
      tone: 'jobs',
    },
    {
      label: 'Resumes saved',
      value: data.resumes.length,
      detail: 'Ready for tailored matching',
      tone: 'resumes',
    },
    {
      label: 'Saved analyses',
      value: data.analyses.length,
      detail: 'Insights ready to revisit',
      tone: 'analyses',
    },
    {
      label: 'Average match',
      value: averageScore === null ? '—' : `${averageScore}%`,
      detail: 'Across saved analyses',
      tone: 'score',
    },
  ] as const

  return (
    <div className="page dashboard-page">
      <header className="page-header page-header--row">
        <div>
          <p className="page-header__eyebrow">Career workspace</p>
          <h1>Dashboard</h1>
          <p>Your pipeline, next actions, and latest career intelligence in one place.</p>
        </div>
        <Link className="button button--primary" to="/analyses/new">
          <span aria-hidden="true">＋</span> New analysis
        </Link>
      </header>

      {isLoading && (
        <div className="state-card" role="status">
          <div className="spinner" aria-hidden="true" />
          <h2>Building your dashboard</h2>
          <p>Gathering your latest jobs, activities, resumes, and analyses.</p>
        </div>
      )}

      {!isLoading && error && (
        <div className="state-card state-card--error" role="alert">
          <div className="state-card__icon" aria-hidden="true">!</div>
          <h2>We couldn’t load your dashboard</h2>
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

      {!isLoading && !error && (
        <>
          <section className="dashboard-metrics" aria-label="Workspace overview">
            {metrics.map((metric) => (
              <article
                className={`dashboard-metric dashboard-metric--${metric.tone}`}
                key={metric.label}
              >
                <span>{metric.label}</span>
                <strong>{metric.value}</strong>
                <p>{metric.detail}</p>
              </article>
            ))}
          </section>

          <section className="dashboard-section" aria-labelledby="dashboard-pipeline-heading">
            <div className="dashboard-section__heading">
              <div>
                <p>Application flow</p>
                <h2 id="dashboard-pipeline-heading">Pipeline snapshot</h2>
              </div>
              <Link to="/jobs">Manage jobs <span aria-hidden="true">→</span></Link>
            </div>
            <PipelineSummary jobs={data.jobs} />
          </section>

          <UpcomingActivities
            activities={data.upcomingActivities}
            isLoading={false}
            error={null}
            onRetry={() => setReloadKey((key) => key + 1)}
            onViewJob={(jobId) => navigate(`/jobs/${jobId}`)}
          />

          <div className="dashboard-grid">
            <RecentAnalyses analyses={data.analyses.slice(0, 3)} />
            <LatestResumes resumes={data.resumes.slice(0, 3)} />
          </div>
        </>
      )}
    </div>
  )
}

function RecentAnalyses({ analyses }: { analyses: MatchAnalysis[] }) {
  return (
    <section className="dashboard-collection" aria-labelledby="recent-analyses-heading">
      <div className="dashboard-section__heading">
        <div>
          <p>Career intelligence</p>
          <h2 id="recent-analyses-heading">Recent analyses</h2>
        </div>
        <Link to="/analyses">View all <span aria-hidden="true">→</span></Link>
      </div>

      {analyses.length === 0 ? (
        <div className="dashboard-collection__empty">
          <span aria-hidden="true">◎</span>
          <p>No analyses yet. Compare a job and resume to get focused guidance.</p>
          <Link to="/analyses/new">Run an analysis</Link>
        </div>
      ) : (
        <div className="dashboard-analysis-list">
          {analyses.map((analysis) => (
            <Link
              className="dashboard-analysis-item"
              to={`/analyses/${analysis.id}`}
              key={analysis.id}
            >
              <span className={`dashboard-analysis-item__score dashboard-analysis-item__score--${getScoreTone(analysis.matchScore)}`}>
                {analysis.matchScore}
              </span>
              <span className="dashboard-analysis-item__content">
                <span>{analysis.company}</span>
                <strong>{analysis.jobTitle}</strong>
                <small>{analysis.resumeName} · {formatDate(analysis.createdAt)}</small>
              </span>
              <span aria-hidden="true">→</span>
            </Link>
          ))}
        </div>
      )}
    </section>
  )
}

function LatestResumes({ resumes }: { resumes: Resume[] }) {
  return (
    <section className="dashboard-collection" aria-labelledby="latest-resumes-heading">
      <div className="dashboard-section__heading">
        <div>
          <p>Resume library</p>
          <h2 id="latest-resumes-heading">Latest resumes</h2>
        </div>
        <Link to="/resumes">View all <span aria-hidden="true">→</span></Link>
      </div>

      {resumes.length === 0 ? (
        <div className="dashboard-collection__empty">
          <span aria-hidden="true">CV</span>
          <p>No resumes saved yet. Add one to unlock tailored analysis.</p>
          <Link to="/resumes/new">Add a resume</Link>
        </div>
      ) : (
        <div className="dashboard-resume-list">
          {resumes.map((resume) => (
            <Link
              className="dashboard-resume-item"
              to={`/resumes/${resume.id}/edit`}
              key={resume.id}
            >
              <span className="dashboard-resume-item__icon" aria-hidden="true">CV</span>
              <span className="dashboard-resume-item__content">
                <strong>{resume.name}</strong>
                <small>Saved {formatDate(resume.createdAt)}</small>
              </span>
              <span aria-hidden="true">→</span>
            </Link>
          ))}
        </div>
      )}
    </section>
  )
}

function getScoreTone(score: number): 'low' | 'medium' | 'high' {
  if (score >= 75) return 'high'
  if (score >= 50) return 'medium'
  return 'low'
}
