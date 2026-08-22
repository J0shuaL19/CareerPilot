import { NavLink, Outlet, useLocation } from 'react-router-dom'

export function AppLayout() {
  const { pathname } = useLocation()
  const isAnalysisRoute = pathname === '/analyses' || pathname.startsWith('/analyses/')

  return (
    <div className="app-shell">
      <header className="app-header">
        <div className="app-header__inner">
          <NavLink className="brand" to="/jobs" aria-label="CareerPilot home">
            <span className="brand__mark" aria-hidden="true">C</span>
            <span>CareerPilot</span>
          </NavLink>

          <nav className="app-nav" aria-label="Main navigation">
            <NavLink
              to="/jobs"
              end
              className={({ isActive }) => (isActive ? 'app-nav__link app-nav__link--active' : 'app-nav__link')}
            >
              Jobs
            </NavLink>
            <NavLink
              to="/resumes"
              end
              className={({ isActive }) => (isActive ? 'app-nav__link app-nav__link--active' : 'app-nav__link')}
            >
              Resumes
            </NavLink>
            <NavLink
              to="/analyses"
              className={isAnalysisRoute
                ? 'app-nav__link app-nav__link--analysis app-nav__link--active'
                : 'app-nav__link app-nav__link--analysis'}
            >
              Analyses
            </NavLink>
            <NavLink className="button button--primary button--compact" to="/jobs/new">
              <span aria-hidden="true">＋</span> Add job
            </NavLink>
          </nav>
        </div>
      </header>

      <main className="app-main">
        <Outlet />
      </main>
    </div>
  )
}
