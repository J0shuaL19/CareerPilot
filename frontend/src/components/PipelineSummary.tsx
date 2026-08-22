import type { Job } from '../types/job'

interface PipelineSummaryProps {
  jobs: Job[]
}

export function PipelineSummary({ jobs }: PipelineSummaryProps) {
  const metrics = [
    {
      label: 'Total opportunities',
      value: jobs.length,
      tone: 'total',
    },
    {
      label: 'In progress',
      value: jobs.filter(({ status }) => ['APPLIED', 'OA', 'INTERVIEW'].includes(status)).length,
      tone: 'progress',
    },
    {
      label: 'Offers',
      value: jobs.filter(({ status }) => status === 'OFFER').length,
      tone: 'offer',
    },
    {
      label: 'Closed',
      value: jobs.filter(({ status }) => ['REJECTED', 'WITHDRAWN'].includes(status)).length,
      tone: 'closed',
    },
  ] as const

  return (
    <section className="pipeline-summary" aria-label="Pipeline overview">
      {metrics.map((metric) => (
        <article
          className={`pipeline-metric pipeline-metric--${metric.tone}`}
          key={metric.label}
        >
          <strong>{metric.value}</strong>
          <span>{metric.label}</span>
        </article>
      ))}
    </section>
  )
}
