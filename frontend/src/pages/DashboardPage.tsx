import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { AttentionSettingsDialog } from '../components/AttentionSettingsDialog'
import { DashboardFunnel } from '../components/DashboardFunnel'
import { NeedsAttentionPanel } from '../components/NeedsAttentionPanel'
import { PipelineSummary } from '../components/PipelineSummary'
import { QuickFollowUpDialog } from '../components/QuickFollowUpDialog'
import { ReminderHistory } from '../components/ReminderHistory'
import {
  SnoozeReminderDialog,
  type SnoozeReminderTarget,
} from '../components/SnoozeReminderDialog'
import { SnoozedRemindersPanel } from '../components/SnoozedRemindersPanel'
import { UpcomingActivities } from '../components/UpcomingActivities'
import { useAttentionNotifications } from '../hooks/useAttentionNotifications'
import { getDashboardStats } from '../services/dashboardApi'
import {
  createJobActivity,
  getJobAttentionItems,
  getJobAttentionSettings,
  getUpcomingJobActivities,
  updateJobAttentionSettings,
} from '../services/jobActivityApi'
import {
  clearJobAttentionSnooze,
  clearJobAttentionSnoozeBulk,
  getJobs,
  restoreJobAttentionSnooze,
  restoreJobAttentionSnoozes,
  snoozeJobAttention,
  snoozeJobAttentionBulk,
} from '../services/jobApi'
import { getMatchAnalyses } from '../services/matchAnalysisApi'
import { getResumes } from '../services/resumeApi'
import type { Job, JobAttentionSnoozeSnapshot } from '../types/job'
import type { DashboardStats, DashboardStatsRange } from '../types/dashboard'
import type {
  CreateJobActivityInput,
  JobAttentionItem,
  JobAttentionSettings,
  UpcomingJobActivity,
} from '../types/jobActivity'
import type { MatchAnalysis } from '../types/matchAnalysis'
import type { Resume } from '../types/resume'
import { getErrorMessage, isAbortError } from '../utils/errors'
import { formatDate } from '../utils/formatDate'

interface SnoozeConfirmation {
  jobId: number
  company: string
  snoozedUntil: string
  previousSnoozedUntil: string | null
}

interface SnoozeDialogItem extends SnoozeReminderTarget {
  jobId: number
}

interface BulkSnoozeConfirmation {
  message: string
  reminders: JobAttentionSnoozeSnapshot[]
}

interface DashboardData {
  jobs: Job[]
  resumes: Resume[]
  analyses: MatchAnalysis[]
  attentionItems: JobAttentionItem[]
  attentionSettings: JobAttentionSettings
  upcomingActivities: UpcomingJobActivity[]
}

const emptyData: DashboardData = {
  jobs: [],
  resumes: [],
  analyses: [],
  attentionItems: [],
  attentionSettings: {
    appliedDays: 7,
    onlineAssessmentDays: 7,
    interviewDays: 7,
  },
  upcomingActivities: [],
}

export function DashboardPage() {
  const navigate = useNavigate()
  const [data, setData] = useState<DashboardData>(emptyData)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [reloadKey, setReloadKey] = useState(0)
  const [stats, setStats] = useState<DashboardStats | null>(null)
  const [statsRange, setStatsRange] = useState<DashboardStatsRange>('LAST_90_DAYS')
  const [isStatsLoading, setIsStatsLoading] = useState(true)
  const [statsError, setStatsError] = useState<string | null>(null)
  const [statsReloadKey, setStatsReloadKey] = useState(0)
  const [followUpItem, setFollowUpItem] = useState<JobAttentionItem | null>(null)
  const [isFollowUpSaving, setIsFollowUpSaving] = useState(false)
  const [followUpError, setFollowUpError] = useState<string | null>(null)
  const [followUpSuccess, setFollowUpSuccess] = useState<string | null>(null)
  const [isAttentionSettingsOpen, setIsAttentionSettingsOpen] = useState(false)
  const [isAttentionSettingsSaving, setIsAttentionSettingsSaving] = useState(false)
  const [attentionSettingsError, setAttentionSettingsError] = useState<string | null>(null)
  const [snoozeItem, setSnoozeItem] = useState<SnoozeDialogItem | null>(null)
  const [isSnoozeSaving, setIsSnoozeSaving] = useState(false)
  const [snoozeError, setSnoozeError] = useState<string | null>(null)
  const [snoozeConfirmation, setSnoozeConfirmation] = useState<SnoozeConfirmation | null>(null)
  const [isSnoozeUndoing, setIsSnoozeUndoing] = useState(false)
  const [snoozeUndoError, setSnoozeUndoError] = useState<string | null>(null)
  const [resumingSnoozeJobId, setResumingSnoozeJobId] = useState<number | null>(null)
  const [snoozedRemindersError, setSnoozedRemindersError] = useState<string | null>(null)
  const [bulkSnoozeJobs, setBulkSnoozeJobs] = useState<Job[] | null>(null)
  const [isBulkSnoozeBusy, setIsBulkSnoozeBusy] = useState(false)
  const [bulkSnoozeConfirmation, setBulkSnoozeConfirmation] =
    useState<BulkSnoozeConfirmation | null>(null)
  const [isBulkSnoozeUndoing, setIsBulkSnoozeUndoing] = useState(false)
  const [bulkSnoozeUndoError, setBulkSnoozeUndoError] = useState<string | null>(null)
  const [bulkSelectionResetKey, setBulkSelectionResetKey] = useState(0)
  const [historyReloadKey, setHistoryReloadKey] = useState(0)
  const attentionNotifications = useAttentionNotifications(data.attentionItems)

  async function handleFollowUpSubmit(input: CreateJobActivityInput) {
    if (!followUpItem) return

    setIsFollowUpSaving(true)
    setFollowUpError(null)
    try {
      await createJobActivity(followUpItem.jobId, input)
      setFollowUpSuccess(`Follow-up saved for ${followUpItem.company}.`)
      setFollowUpItem(null)
      setReloadKey((key) => key + 1)
      setHistoryReloadKey((key) => key + 1)
    } catch (saveError) {
      setFollowUpError(getErrorMessage(saveError))
    } finally {
      setIsFollowUpSaving(false)
    }
  }

  async function handleAttentionSettingsSubmit(settings: JobAttentionSettings) {
    setIsAttentionSettingsSaving(true)
    setAttentionSettingsError(null)

    try {
      const updatedSettings = await updateJobAttentionSettings(settings)
      setData((current) => ({ ...current, attentionSettings: updatedSettings }))
      setIsAttentionSettingsOpen(false)
      setFollowUpSuccess('Follow-up reminder rules updated.')
      setReloadKey((key) => key + 1)
    } catch (saveError) {
      setAttentionSettingsError(getErrorMessage(saveError))
    } finally {
      setIsAttentionSettingsSaving(false)
    }
  }

  async function handleSnoozeSubmit(snoozedUntil: string) {
    if (!snoozeItem) return

    setIsSnoozeSaving(true)
    setSnoozeError(null)
    try {
      const updatedJob = await snoozeJobAttention(snoozeItem.jobId, snoozedUntil)
      setData((current) => ({
        ...current,
        jobs: current.jobs.map((job) => job.id === updatedJob.id ? updatedJob : job),
        attentionItems: current.attentionItems.filter(
          (item) => item.jobId !== snoozeItem.jobId,
        ),
      }))
      setBulkSnoozeConfirmation(null)
      setBulkSnoozeUndoError(null)
      setSnoozeConfirmation({
        jobId: snoozeItem.jobId,
        company: snoozeItem.company,
        snoozedUntil,
        previousSnoozedUntil: snoozeItem.currentSnoozedUntil ?? null,
      })
      setSnoozeUndoError(null)
      setSnoozedRemindersError(null)
      setFollowUpSuccess(null)
      setSnoozeItem(null)
      setHistoryReloadKey((key) => key + 1)
    } catch (saveError) {
      setSnoozeError(getErrorMessage(saveError))
    } finally {
      setIsSnoozeSaving(false)
    }
  }

  async function handleSnoozeUndo() {
    if (!snoozeConfirmation) return

    setIsSnoozeUndoing(true)
    setSnoozeUndoError(null)
    try {
      await restoreJobAttentionSnooze(
        snoozeConfirmation.jobId,
        snoozeConfirmation.previousSnoozedUntil,
      )
      setSnoozeConfirmation(null)
      setReloadKey((key) => key + 1)
      setHistoryReloadKey((key) => key + 1)
    } catch (undoError) {
      setSnoozeUndoError(getErrorMessage(undoError))
    } finally {
      setIsSnoozeUndoing(false)
    }
  }

  async function handleResumeReminder(job: Job) {
    setResumingSnoozeJobId(job.id)
    setSnoozedRemindersError(null)

    try {
      const updatedJob = await clearJobAttentionSnooze(job.id)
      setData((current) => ({
        ...current,
        jobs: current.jobs.map((item) => item.id === updatedJob.id ? updatedJob : item),
      }))
      setSnoozeConfirmation(null)
      setBulkSnoozeConfirmation(null)
      setBulkSnoozeUndoError(null)
      setFollowUpSuccess(job.company + ' reminder is active again.')
      setReloadKey((key) => key + 1)
      setHistoryReloadKey((key) => key + 1)
    } catch (resumeError) {
      setSnoozedRemindersError(getErrorMessage(resumeError))
    } finally {
      setResumingSnoozeJobId(null)
    }
  }

  async function handleBulkSnoozeSubmit(snoozedUntil: string) {
    if (!bulkSnoozeJobs) return

    const previousReminders = toSnoozeSnapshots(bulkSnoozeJobs)
    setIsBulkSnoozeBusy(true)
    setSnoozeError(null)

    try {
      const updatedJobs = await snoozeJobAttentionBulk(
        bulkSnoozeJobs.map((job) => job.id),
        snoozedUntil,
      )
      const updatesById = new Map(updatedJobs.map((job) => [job.id, job]))
      setData((current) => ({
        ...current,
        jobs: current.jobs.map((job) => updatesById.get(job.id) ?? job),
      }))
      setBulkSnoozeJobs(null)
      setSnoozedRemindersError(null)
      setSnoozeConfirmation(null)
      setFollowUpSuccess(null)
      setBulkSnoozeUndoError(null)
      setBulkSnoozeConfirmation({
        message: updatedJobs.length + ' reminders now return on '
          + formatDate(snoozedUntil + 'T12:00:00') + '.',
        reminders: previousReminders,
      })
      setBulkSelectionResetKey((key) => key + 1)
      setHistoryReloadKey((key) => key + 1)
    } catch (saveError) {
      setSnoozeError(getErrorMessage(saveError))
    } finally {
      setIsBulkSnoozeBusy(false)
    }
  }

  async function handleBulkResume(jobs: Job[]) {
    const previousReminders = toSnoozeSnapshots(jobs)
    setIsBulkSnoozeBusy(true)
    setSnoozedRemindersError(null)

    try {
      const updatedJobs = await clearJobAttentionSnoozeBulk(jobs.map((job) => job.id))
      const updatesById = new Map(updatedJobs.map((job) => [job.id, job]))
      setData((current) => ({
        ...current,
        jobs: current.jobs.map((job) => updatesById.get(job.id) ?? job),
      }))
      setSnoozeConfirmation(null)
      setFollowUpSuccess(null)
      setBulkSnoozeUndoError(null)
      setBulkSnoozeConfirmation({
        message: updatedJobs.length
          + (updatedJobs.length === 1 ? ' reminder is' : ' reminders are')
          + ' active again.',
        reminders: previousReminders,
      })
      setBulkSelectionResetKey((key) => key + 1)
      setReloadKey((key) => key + 1)
      setHistoryReloadKey((key) => key + 1)
    } catch (resumeError) {
      setSnoozedRemindersError(getErrorMessage(resumeError))
    } finally {
      setIsBulkSnoozeBusy(false)
    }
  }

  async function handleBulkSnoozeUndo() {
    if (!bulkSnoozeConfirmation) return

    setIsBulkSnoozeUndoing(true)
    setBulkSnoozeUndoError(null)

    try {
      const updatedJobs = await restoreJobAttentionSnoozes(
        bulkSnoozeConfirmation.reminders,
      )
      const updatesById = new Map(updatedJobs.map((job) => [job.id, job]))
      const restoredIds = new Set(updatedJobs.map((job) => job.id))
      setData((current) => ({
        ...current,
        jobs: current.jobs.map((job) => updatesById.get(job.id) ?? job),
        attentionItems: current.attentionItems.filter(
          (item) => !restoredIds.has(item.jobId),
        ),
      }))
      setBulkSnoozeConfirmation(null)
      setSnoozedRemindersError(null)
      setBulkSelectionResetKey((key) => key + 1)
      setReloadKey((key) => key + 1)
      setHistoryReloadKey((key) => key + 1)
    } catch (undoError) {
      setBulkSnoozeUndoError(getErrorMessage(undoError))
    } finally {
      setIsBulkSnoozeUndoing(false)
    }
  }

  useEffect(() => {
    const controller = new AbortController()

    async function loadDashboard() {
      setIsLoading(true)
      setError(null)

      try {
        const [
          jobs,
          resumes,
          analyses,
          attentionItems,
          attentionSettings,
          upcomingActivities,
        ] = await Promise.all([
          getJobs(controller.signal),
          getResumes(controller.signal),
          getMatchAnalyses(controller.signal),
          getJobAttentionItems(controller.signal),
          getJobAttentionSettings(controller.signal),
          getUpcomingJobActivities(controller.signal),
        ])
        setData({
          jobs,
          resumes,
          analyses,
          attentionItems,
          attentionSettings,
          upcomingActivities,
        })
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

  useEffect(() => {
    const controller = new AbortController()

    async function loadStats() {
      setIsStatsLoading(true)
      setStatsError(null)

      try {
        setStats(await getDashboardStats(statsRange, controller.signal))
      } catch (loadError) {
        if (!isAbortError(loadError)) {
          setStatsError(getErrorMessage(loadError))
        }
      } finally {
        if (!controller.signal.aborted) {
          setIsStatsLoading(false)
        }
      }
    }

    void loadStats()
    return () => controller.abort()
  }, [statsRange, statsReloadKey])

  const today = toDateInputValue(new Date())
  const snoozedJobs = data.jobs
    .filter((job) => (
      ['APPLIED', 'OA', 'INTERVIEW'].includes(job.status)
      && job.attentionSnoozedUntil !== null
      && job.attentionSnoozedUntil > today
    ))
    .sort((first, second) => (
      (first.attentionSnoozedUntil ?? '').localeCompare(second.attentionSnoozedUntil ?? '')
    ))
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

      {followUpSuccess && (
        <div className="dashboard-feedback" role="status">
          <span aria-hidden="true">✓</span>
          <strong>{followUpSuccess}</strong>
          <button
            className="dashboard-feedback__close"
            type="button"
            aria-label="Dismiss confirmation"
            onClick={() => setFollowUpSuccess(null)}
          >
            ×
          </button>
        </div>
      )}

      {snoozeConfirmation && (
        <div className="dashboard-feedback dashboard-feedback--snoozed" role="status">
          <span aria-hidden="true">◷</span>
          <strong>
            {snoozeConfirmation.company} snoozed until{' '}
            {formatDate(snoozeConfirmation.snoozedUntil + 'T12:00:00')}.
          </strong>
          {snoozeUndoError && <small role="alert">{snoozeUndoError}</small>}
          <button
            className="dashboard-feedback__undo"
            type="button"
            disabled={isSnoozeUndoing}
            onClick={() => void handleSnoozeUndo()}
          >
            {isSnoozeUndoing ? 'Undoing…' : 'Undo'}
          </button>
          <button
            className="dashboard-feedback__close"
            type="button"
            aria-label="Dismiss snooze confirmation"
            disabled={isSnoozeUndoing}
            onClick={() => setSnoozeConfirmation(null)}
          >
            ×
          </button>
        </div>
      )}

      {bulkSnoozeConfirmation && (
        <div className="dashboard-feedback dashboard-feedback--snoozed" role="status">
          <span aria-hidden="true">◷</span>
          <strong>{bulkSnoozeConfirmation.message}</strong>
          {bulkSnoozeUndoError && <small role="alert">{bulkSnoozeUndoError}</small>}
          <button
            className="dashboard-feedback__undo"
            type="button"
            disabled={isBulkSnoozeUndoing}
            onClick={() => void handleBulkSnoozeUndo()}
          >
            {isBulkSnoozeUndoing ? 'Undoing…' : 'Undo'}
          </button>
          <button
            className="dashboard-feedback__close"
            type="button"
            aria-label="Dismiss bulk snooze confirmation"
            disabled={isBulkSnoozeUndoing}
            onClick={() => {
              setBulkSnoozeConfirmation(null)
              setBulkSnoozeUndoError(null)
            }}
          >
            ×
          </button>
        </div>
      )}

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

          <DashboardFunnel
            stats={stats}
            range={statsRange}
            isLoading={isStatsLoading}
            error={statsError}
            onRangeChange={setStatsRange}
            onRetry={() => setStatsReloadKey((key) => key + 1)}
          />

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

          <NeedsAttentionPanel
            items={data.attentionItems}
            settings={data.attentionSettings}
            onConfigure={() => {
              setAttentionSettingsError(null)
              setIsAttentionSettingsOpen(true)
            }}
            onFollowUp={(item) => {
              setFollowUpError(null)
              setFollowUpItem(item)
            }}
            notificationStatus={attentionNotifications.status}
            isNotificationRequesting={attentionNotifications.isRequesting}
            notificationError={attentionNotifications.error}
            onToggleNotifications={() => void attentionNotifications.toggle()}
            onSnooze={(item) => {
              setSnoozeError(null)
              setSnoozeItem(item)
            }}
          />

          {snoozedJobs.length > 0 && (
            <SnoozedRemindersPanel
              key={bulkSelectionResetKey}
              jobs={snoozedJobs}
              busyJobId={resumingSnoozeJobId}
              isBulkBusy={isBulkSnoozeBusy || isBulkSnoozeUndoing}
              error={snoozedRemindersError}
              onChangeDate={(job) => {
                setSnoozeError(null)
                setSnoozedRemindersError(null)
                setBulkSnoozeJobs(null)
                setSnoozeItem({
                  jobId: job.id,
                  company: job.company,
                  jobTitle: job.title,
                  currentSnoozedUntil: job.attentionSnoozedUntil,
                })
              }}
              onResume={(job) => void handleResumeReminder(job)}
              onBulkChangeDate={(jobs) => {
                setSnoozeError(null)
                setSnoozedRemindersError(null)
                setSnoozeItem(null)
                setBulkSnoozeJobs(jobs)
              }}
              onBulkResume={(jobs) => void handleBulkResume(jobs)}
              onViewJob={(jobId) => navigate('/jobs/' + jobId)}
            />
          )}

          <ReminderHistory
            reloadKey={historyReloadKey}
            onViewJob={(jobId) => navigate(`/jobs/${jobId}`)}
          />

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

      {isAttentionSettingsOpen && (
        <AttentionSettingsDialog
          settings={data.attentionSettings}
          isSaving={isAttentionSettingsSaving}
          error={attentionSettingsError}
          onClose={() => {
            setAttentionSettingsError(null)
            setIsAttentionSettingsOpen(false)
          }}
          onSubmit={(settings) => void handleAttentionSettingsSubmit(settings)}
        />
      )}

      {(snoozeItem || bulkSnoozeJobs) && (
        <SnoozeReminderDialog
          item={snoozeItem ?? undefined}
          selectionCount={bulkSnoozeJobs?.length}
          isSaving={bulkSnoozeJobs ? isBulkSnoozeBusy : isSnoozeSaving}
          error={snoozeError}
          onClose={() => {
            setSnoozeError(null)
            setSnoozeItem(null)
            setBulkSnoozeJobs(null)
          }}
          onSubmit={(snoozedUntil) => {
            if (bulkSnoozeJobs) {
              void handleBulkSnoozeSubmit(snoozedUntil)
            } else {
              void handleSnoozeSubmit(snoozedUntil)
            }
          }}
        />
      )}

      {followUpItem && (
        <QuickFollowUpDialog
          item={followUpItem}
          isSaving={isFollowUpSaving}
          error={followUpError}
          onClose={() => {
            setFollowUpError(null)
            setFollowUpItem(null)
          }}
          onSubmit={(input) => void handleFollowUpSubmit(input)}
          onViewJob={(jobId) => navigate(`/jobs/${jobId}`)}
        />
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

function toSnoozeSnapshots(jobs: Job[]): JobAttentionSnoozeSnapshot[] {
  return jobs.flatMap((job) => (
    job.attentionSnoozedUntil === null
      ? []
      : [{ jobId: job.id, snoozedUntil: job.attentionSnoozedUntil }]
  ))
}

function toDateInputValue(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return [year, month, day].join('-')
}
