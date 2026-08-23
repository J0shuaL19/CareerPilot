import { useState, type ChangeEvent } from 'react'
import { importJobCsv, previewJobCsv } from '../services/jobApi'
import type {
  JobCsvImportPreview,
  JobCsvImportResult,
  JobCsvImportRowState,
} from '../types/job'
import { getErrorMessage } from '../utils/errors'
import { getJobStatusConfig } from '../utils/jobStatus'

interface JobCsvImportPanelProps {
  onClose: () => void
  onImported: (result: JobCsvImportResult) => void
}

const maxFileSizeBytes = 2 * 1024 * 1024

const stateConfig: Record<JobCsvImportRowState, { label: string; symbol: string }> = {
  VALID: { label: 'Ready', symbol: '✓' },
  DUPLICATE: { label: 'Duplicate', symbol: '≈' },
  INVALID: { label: 'Needs attention', symbol: '!' },
}

export function JobCsvImportPanel({ onClose, onImported }: JobCsvImportPanelProps) {
  const [file, setFile] = useState<File | null>(null)
  const [preview, setPreview] = useState<JobCsvImportPreview | null>(null)
  const [isPreviewing, setIsPreviewing] = useState(false)
  const [isImporting, setIsImporting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleFileChange(event: ChangeEvent<HTMLInputElement>) {
    const selectedFile = event.target.files?.[0]
    event.target.value = ''
    if (!selectedFile) return

    setError(null)
    setPreview(null)
    setFile(null)

    if (!selectedFile.name.toLocaleLowerCase().endsWith('.csv')) {
      setError('Choose a CSV file to import.')
      return
    }
    if (selectedFile.size > maxFileSizeBytes) {
      setError('Job CSV files must be 2 MB or smaller.')
      return
    }

    setIsPreviewing(true)
    try {
      const nextPreview = await previewJobCsv(selectedFile)
      setFile(selectedFile)
      setPreview(nextPreview)
    } catch (previewError) {
      setError(getErrorMessage(previewError))
    } finally {
      setIsPreviewing(false)
    }
  }

  async function handleImport() {
    if (!file || !preview || preview.validRows === 0) return

    setIsImporting(true)
    setError(null)
    try {
      onImported(await importJobCsv(file))
    } catch (importError) {
      setError(getErrorMessage(importError))
    } finally {
      setIsImporting(false)
    }
  }

  return (
    <section className="job-csv-import" aria-labelledby="job-csv-import-title">
      <div className="job-csv-import__heading">
        <div>
          <p>Bulk import</p>
          <h2 id="job-csv-import-title">Import jobs from CSV</h2>
          <span>Preview every row before anything is saved.</span>
        </div>
        <button
          className="job-csv-import__close"
          type="button"
          aria-label="Close CSV import"
          disabled={isImporting}
          onClick={onClose}
        >
          ×
        </button>
      </div>

      <div className="job-csv-import__picker">
        <label className={isPreviewing || isImporting ? 'is-busy' : undefined}>
          <input
            type="file"
            accept=".csv,text/csv"
            disabled={isPreviewing || isImporting}
            onChange={(event) => void handleFileChange(event)}
          />
          <span aria-hidden="true">⇧</span>
          <strong>{isPreviewing ? 'Checking file…' : 'Choose CSV file'}</strong>
          <small>Company, Title, and Description required · max 2 MB</small>
        </label>
        <p>
          CareerPilot exports can be imported directly. Status and Job URL are optional.
        </p>
      </div>

      {error && (
        <div className="alert alert--error job-csv-import__error" role="alert">
          {error}
        </div>
      )}

      {preview && (
        <>
          <div className="job-csv-import__summary" aria-label="CSV preview summary">
            <div>
              <span>File</span>
              <strong title={preview.filename}>{preview.filename}</strong>
            </div>
            <div className="job-csv-import__summary-item job-csv-import__summary-item--valid">
              <span>Ready</span>
              <strong>{preview.validRows}</strong>
            </div>
            <div className="job-csv-import__summary-item job-csv-import__summary-item--duplicate">
              <span>Duplicates</span>
              <strong>{preview.duplicateRows}</strong>
            </div>
            <div className="job-csv-import__summary-item job-csv-import__summary-item--invalid">
              <span>Invalid</span>
              <strong>{preview.invalidRows}</strong>
            </div>
          </div>

          <div className="job-csv-import__table-wrap">
            <table className="job-csv-import__table">
              <thead>
                <tr>
                  <th>Row</th>
                  <th>Company and role</th>
                  <th>Status</th>
                  <th>Import result</th>
                </tr>
              </thead>
              <tbody>
                {preview.rows.map((row) => {
                  const config = stateConfig[row.state]
                  return (
                    <tr key={row.rowNumber}>
                      <td>{row.rowNumber}</td>
                      <td>
                        <strong>{row.title || 'Missing title'}</strong>
                        <span>{row.company || 'Missing company'}</span>
                      </td>
                      <td>{getJobStatusConfig(row.status).label}</td>
                      <td>
                        <span className={`job-csv-import__state job-csv-import__state--${row.state.toLocaleLowerCase()}`}>
                          <b aria-hidden="true">{config.symbol}</b> {config.label}
                        </span>
                        {row.errors.length > 0 && <small>{row.errors.join(' · ')}</small>}
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>

          <div className="job-csv-import__actions">
            <p>
              {preview.validRows === 0
                ? 'There are no new valid jobs to import.'
                : `${preview.validRows} ${preview.validRows === 1 ? 'job is' : 'jobs are'} ready to import.`}
            </p>
            <div>
              <button
                className="button button--secondary"
                type="button"
                disabled={isImporting}
                onClick={onClose}
              >
                Cancel
              </button>
              <button
                className="button button--primary"
                type="button"
                disabled={preview.validRows === 0 || isImporting}
                onClick={() => void handleImport()}
              >
                {isImporting ? 'Importing…' : `Import ${preview.validRows} ${preview.validRows === 1 ? 'job' : 'jobs'}`}
              </button>
            </div>
          </div>
        </>
      )}
    </section>
  )
}
