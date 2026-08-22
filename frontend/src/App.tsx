import { Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom'
import { AppLayout } from './components/AppLayout'
import { AddJobPage } from './pages/AddJobPage'
import { AddResumePage } from './pages/AddResumePage'
import { JobsPage } from './pages/JobsPage'
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

function App() {
  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route index element={<Navigate to="/jobs" replace />} />
        <Route path="jobs" element={<JobsRoute />} />
        <Route path="jobs/new" element={<AddJobRoute />} />
        <Route path="resumes" element={<ResumesRoute />} />
        <Route path="resumes/new" element={<AddResumeRoute />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}

export default App
