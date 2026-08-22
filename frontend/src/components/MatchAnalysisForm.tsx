import { useState, type FormEvent } from 'react'
import type { Job } from '../types/job'
import type { CreateMatchAnalysisInput } from '../types/matchAnalysis'
import type { Resume } from '../types/resume'

interface MatchAnalysisFormProps {
  jobs: Job[]
  resumes: Resume[]
  isSubmitting: boolean
  formError?: string | null
  onSubmit: (input: CreateMatchAnalysisInput) => Promise<void>
}

export function MatchAnalysisForm({
  jobs,
  resumes,
  isSubmitting,
  formError,
  onSubmit,
}: MatchAnalysisFormProps) {
  const [jobId, setJobId] = useState(String(jobs[0].id))
  const [resumeId, setResumeId] = useState(String(resumes[0].id))

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    await onSubmit({
      jobId: Number(jobId),
      resumeId: Number(resumeId),
    })
  }

  return (
    <form className="analysis-form" onSubmit={handleSubmit}>
      {formError && (
        <div className="alert alert--error" role="alert">
          {formError}
        </div>
      )}

      <div className="analysis-form__selectors">
        <div className="form-field">
          <label htmlFor="analysis-job">Job</label>
          <select
            id="analysis-job"
            value={jobId}
            onChange={(event) => setJobId(event.target.value)}
            disabled={isSubmitting}
          >
            {jobs.map((job) => (
              <option key={job.id} value={job.id}>
                {job.title} — {job.company}
              </option>
            ))}
          </select>
          <p className="form-field__hint">
            The complete saved job description will be used for matching.
          </p>
        </div>

        <div className="form-field">
          <label htmlFor="analysis-resume">Resume</label>
          <select
            id="analysis-resume"
            value={resumeId}
            onChange={(event) => setResumeId(event.target.value)}
            disabled={isSubmitting}
          >
            {resumes.map((resume) => (
              <option key={resume.id} value={resume.id}>
                {resume.name}
              </option>
            ))}
          </select>
          <p className="form-field__hint">
            Choose the resume version you want to tailor for this role.
          </p>
        </div>
      </div>

      <div className="analysis-form__privacy">
        <span aria-hidden="true">◇</span>
        <p>
          CareerPilot sends the selected content through the backend for one analysis.
          Your API key never enters the browser.
        </p>
      </div>

      <div className="form-actions">
        <button className="button button--primary" type="submit" disabled={isSubmitting}>
          {isSubmitting ? (
            <>
              <span className="button__spinner" aria-hidden="true" />
              Analyzing match…
            </>
          ) : (
            'Analyze match'
          )}
        </button>
      </div>
    </form>
  )
}
