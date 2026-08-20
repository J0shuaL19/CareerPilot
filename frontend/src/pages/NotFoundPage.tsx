import { Link } from 'react-router-dom'

export function NotFoundPage() {
  return (
    <div className="page page--narrow">
      <div className="state-card not-found">
        <p className="not-found__code">404</p>
        <h1>Page not found</h1>
        <p>The page you’re looking for doesn’t exist or has moved.</p>
        <Link className="button button--primary" to="/jobs">
          Back to jobs
        </Link>
      </div>
    </div>
  )
}
