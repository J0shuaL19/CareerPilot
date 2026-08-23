import {
  Navigate,
  Route,
  Routes,
  useLocation,
  useNavigate,
  useParams,
} from 'react-router-dom'
import { AppLayout } from './components/AppLayout'
import { ScrollToTop } from './components/ScrollToTop'
import { AddJobPage } from './pages/AddJobPage'
import { AddResumePage } from './pages/AddResumePage'
import { EditJobPage } from './pages/EditJobPage'
import { EditResumePage } from './pages/EditResumePage'
import { JobDetailPage } from './pages/JobDetailPage'
import { JobsPage } from './pages/JobsPage'
import { MatchAnalysesPage } from './pages/MatchAnalysesPage'
import { MatchAnalysisResultPage } from './pages/MatchAnalysisResultPage'
import { NewMatchAnalysisPage } from './pages/NewMatchAnalysisPage'
import { NotFoundPage } from './pages/NotFoundPage'
import { ResumesPage } from './pages/ResumesPage'
import type { Job } from './types/job'
import type { Resume } from './types/resume'

interface NavigationState {
  notice?: string
}

function JobsRoute() {
  const navigate = useNavigate()
  const location = useLocation()
  const state = location.state as NavigationState | null

  return (
    <JobsPage
      notice={state?.notice}
      onAddJob={() => navigate('/jobs/new')}
      onViewJob={(jobId) => navigate(`/jobs/${jobId}`)}
      onEditJob={(jobId) => navigate(`/jobs/${jobId}/edit`)}
    />
  )
}

function EditJobRoute() {
  const navigate = useNavigate()
  const jobId = Number(useParams().id)

  if (!Number.isSafeInteger(jobId) || jobId <= 0) {
    return <NotFoundPage />
  }

  return (
    <EditJobPage
      jobId={jobId}
      onCancel={() => navigate('/jobs')}
      onUpdated={(job) => navigate('/jobs', {
        replace: true,
        state: { notice: `${job.title} at ${job.company} was updated.` },
      })}
    />
  )
}

function AddJobRoute() {
  const navigate = useNavigate()

  function handleCreated(job: Job) {
    navigate('/jobs', {
      replace: true,
      state: { notice: `${job.title} at ${job.company} was saved.` },
    })
  }

  return (
    <AddJobPage
      onCancel={() => navigate('/jobs')}
      onCreated={handleCreated}
    />
  )
}

function ResumesRoute() {
  const navigate = useNavigate()
  const location = useLocation()
  const state = location.state as NavigationState | null

  return (
    <ResumesPage
      notice={state?.notice}
      onAddResume={() => navigate('/resumes/new')}
      onEditResume={(resumeId) => navigate(`/resumes/${resumeId}/edit`)}
    />
  )
}

function EditResumeRoute() {
  const navigate = useNavigate()
  const resumeId = Number(useParams().id)

  if (!Number.isSafeInteger(resumeId) || resumeId <= 0) {
    return <NotFoundPage />
  }

  return (
    <EditResumePage
      resumeId={resumeId}
      onCancel={() => navigate('/resumes')}
      onUpdated={(resume) => navigate('/resumes', {
        replace: true,
        state: { notice: `${resume.name} was updated.` },
      })}
    />
  )
}

function AddResumeRoute() {
  const navigate = useNavigate()

  function handleCreated(resume: Resume) {
    navigate('/resumes', {
      replace: true,
      state: { notice: `${resume.name} was saved.` },
    })
  }

  return (
    <AddResumePage
      onCancel={() => navigate('/resumes')}
      onCreated={handleCreated}
    />
  )
}

function NewMatchAnalysisRoute() {
  const navigate = useNavigate()

  return (
    <NewMatchAnalysisPage
      onCreated={(analysis) => navigate(`/analyses/${analysis.id}`)}
    />
  )
}

function App() {
  return (
    <>
      <ScrollToTop />
      <Routes>
        <Route element={<AppLayout />}>
          <Route index element={<Navigate to="/jobs" replace />} />
          <Route path="jobs" element={<JobsRoute />} />
          <Route path="jobs/new" element={<AddJobRoute />} />
          <Route path="jobs/:id/edit" element={<EditJobRoute />} />
          <Route path="jobs/:id" element={<JobDetailPage />} />
          <Route path="resumes" element={<ResumesRoute />} />
          <Route path="resumes/new" element={<AddResumeRoute />} />
          <Route path="resumes/:id/edit" element={<EditResumeRoute />} />
          <Route path="analyses" element={<MatchAnalysesPage />} />
          <Route path="analyses/new" element={<NewMatchAnalysisRoute />} />
          <Route path="analyses/:id" element={<MatchAnalysisResultPage />} />
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Routes>
    </>
  )
}

export default App
