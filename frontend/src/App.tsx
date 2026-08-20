import { Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom'
import { AppLayout } from './components/AppLayout'
import { AddJobPage } from './pages/AddJobPage'
import { JobsPage } from './pages/JobsPage'
import { NotFoundPage } from './pages/NotFoundPage'
import type { Job } from './types/job'

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

function App() {
  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route index element={<Navigate to="/jobs" replace />} />
        <Route path="jobs" element={<JobsRoute />} />
        <Route path="jobs/new" element={<AddJobRoute />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}

export default App
