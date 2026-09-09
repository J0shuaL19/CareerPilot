import { type ChangeEvent, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  exportCareerPilotData,
  getDataTransferCapabilities,
  importCareerPilotData,
  previewCareerPilotImport,
} from '../services/dataTransferApi'
import type {
  DataTransferCapabilities,
  DataTransferCounts,
  DataTransferImportResult,
  DataTransferPreview,
} from '../types/dataTransfer'
import { getErrorMessage, isAbortError } from '../utils/errors'

const countLabels: Array<[keyof DataTransferCounts, string]> = [
  ['jobs', 'Jobs'],
  ['resumes', 'Resumes'],
  ['jobActivities', 'Calendar activities'],
  ['matchAnalyses', 'Match analyses'],
  ['attentionEvents', 'Reminder history'],
  ['interviewPreparations', 'Interview preparations'],
  ['attentionSettings', 'Reminder settings'],
]

function downloadFilename() {
  const stamp = new Date().toISOString().replace(/[-:]/g, '').replace(/\.\d{3}Z$/, 'Z')
  return `careerpilot-data-${stamp}.json`
}

function formatBytes(bytes: number) {
  return `${Math.round(bytes / 1024 / 1024)} MB`
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}

function totalRecords(counts: DataTransferCounts) {
  return countLabels.reduce((total, [key]) => total + counts[key], 0)
}

function RecordCounts({ counts }: { counts: DataTransferCounts }) {
  return (
    <dl className="data-transfer-counts">
      {countLabels.map(([key, label]) => (
        <div key={key}>
          <dt>{label}</dt>
          <dd>{counts[key]}</dd>
        </div>
      ))}
    </dl>
  )
}

export function DataTransferPage() {
  const [capabilities, setCapabilities] = useState<DataTransferCapabilities | null>(null)
  const [capabilitiesError, setCapabilitiesError] = useState<string | null>(null)
  const [isExporting, setIsExporting] = useState(false)
  const [exportNotice, setExportNotice] = useState<string | null>(null)
  const [exportError, setExportError] = useState<string | null>(null)
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [preview, setPreview] = useState<DataTransferPreview | null>(null)
  const [isPreviewing, setIsPreviewing] = useState(false)
  const [previewError, setPreviewError] = useState<string | null>(null)
  const [confirmed, setConfirmed] = useState(false)
  const [isImporting, setIsImporting] = useState(false)
  const [importError, setImportError] = useState<string | null>(null)
  const [importResult, setImportResult] = useState<DataTransferImportResult | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    getDataTransferCapabilities(controller.signal)
      .then(setCapabilities)
      .catch((error: unknown) => {
        if (!isAbortError(error)) setCapabilitiesError(getErrorMessage(error))
      })
    return () => controller.abort()
  }, [])

  async function handleExport() {
    setIsExporting(true)
    setExportError(null)
    setExportNotice(null)
    try {
      const archive = await exportCareerPilotData()
      const url = URL.createObjectURL(archive)
      const link = document.createElement('a')
      link.href = url
      link.download = downloadFilename()
      document.body.appendChild(link)
      link.click()
      link.remove()
      URL.revokeObjectURL(url)
      setExportNotice('Your complete CareerPilot data export was downloaded.')
    } catch (error) {
      setExportError(getErrorMessage(error))
    } finally {
      setIsExporting(false)
    }
  }

  async function handleFileChange(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0] ?? null
    setSelectedFile(file)
    setPreview(null)
    setConfirmed(false)
    setPreviewError(null)
    setImportError(null)
    setImportResult(null)
    if (!file || !capabilities) return
    if (file.size > capabilities.maxImportFileSizeBytes) {
      setPreviewError(
        `Choose a CareerPilot data export smaller than ${formatBytes(capabilities.maxImportFileSizeBytes)}.`,
      )
      return
    }

    setIsPreviewing(true)
    try {
      setPreview(await previewCareerPilotImport(file))
    } catch (error) {
      setPreviewError(getErrorMessage(error))
    } finally {
      setIsPreviewing(false)
    }
  }

  async function handleImport() {
    if (!selectedFile || !preview || !confirmed) return
    setIsImporting(true)
    setImportError(null)
    try {
      const result = await importCareerPilotData(selectedFile)
      setImportResult(result)
      setPreview(null)
      setSelectedFile(null)
      setConfirmed(false)
    } catch (error) {
      setImportError(getErrorMessage(error))
    } finally {
      setIsImporting(false)
    }
  }

  return (
    <div className="page page--narrow data-transfer-page">
      <header className="page-header">
        <p className="page-header__eyebrow">Data portability</p>
        <h1>Move your CareerPilot data</h1>
        <p>
          Download a complete, versioned copy from the web app and restore it in the
          Windows app without losing relationships or history.
        </p>
      </header>

      {capabilitiesError && <div className="alert alert--error">{capabilitiesError}</div>}

      <section className="panel data-transfer-card" aria-labelledby="export-heading">
        <div className="data-transfer-card__icon" aria-hidden="true">⇩</div>
        <div className="data-transfer-card__content">
          <div className="data-transfer-card__heading">
            <div>
              <span>Step 1 · Web app</span>
              <h2 id="export-heading">Export all data</h2>
            </div>
            <span className="data-transfer-badge">Available here</span>
          </div>
          <p>
            Includes jobs, resumes, calendar activities, reminder history, match analyses,
            reminder settings, and interview preparation notes. API keys and server
            passwords are never included.
          </p>
          {exportNotice && (
            <div className="alert alert--success"><span>✓</span>{exportNotice}</div>
          )}
          {exportError && <div className="alert alert--error">{exportError}</div>}
          <div className="data-transfer-actions">
            <button
              className="button button--primary"
              type="button"
              disabled={isExporting}
              onClick={() => void handleExport()}
            >
              <span aria-hidden="true">⇩</span>
              {isExporting ? 'Preparing export…' : 'Download data export'}
            </button>
            <span>JSON format · Version {capabilities?.formatVersion ?? 1}</span>
          </div>
        </div>
      </section>

      <div className="data-transfer-connector" aria-hidden="true">
        <span>2</span>
      </div>

      <section className="panel data-transfer-card" aria-labelledby="import-heading">
        <div className="data-transfer-card__icon data-transfer-card__icon--import" aria-hidden="true">⇧</div>
        <div className="data-transfer-card__content">
          <div className="data-transfer-card__heading">
            <div>
              <span>Step 2 · Windows app</span>
              <h2 id="import-heading">Import into the EXE</h2>
            </div>
            <span className={capabilities?.importEnabled
              ? 'data-transfer-badge data-transfer-badge--desktop'
              : 'data-transfer-badge data-transfer-badge--muted'}>
              {capabilities?.importEnabled ? 'Desktop mode' : 'Windows app only'}
            </span>
          </div>

          {!capabilities && !capabilitiesError && (
            <div className="data-transfer-state">Checking this app…</div>
          )}

          {capabilities && !capabilities.importEnabled && (
            <div className="data-transfer-web-notice">
              <strong>Import is intentionally disabled on the web app.</strong>
              <p>
                Open this page from CareerPilot.exe, then choose the JSON file you downloaded
                above. This protects the hosted database from accidental replacement.
              </p>
            </div>
          )}

          {capabilities?.importEnabled && (
            <>
              <p>
                Choose a CareerPilot JSON export. The file is checked first; nothing changes
                until you review the preview and confirm.
              </p>
              <label className="data-transfer-file">
                <span>{isPreviewing ? 'Checking export…' : 'Choose data export'}</span>
                <input
                  type="file"
                  accept="application/json,.json"
                  disabled={isPreviewing || isImporting}
                  onChange={(event) => void handleFileChange(event)}
                />
                <small>
                  {selectedFile
                    ? `${selectedFile.name} · ${(selectedFile.size / 1024).toFixed(1)} KB`
                    : `Maximum file size ${formatBytes(capabilities.maxImportFileSizeBytes)}`}
                </small>
              </label>
              {previewError && <div className="alert alert--error">{previewError}</div>}

              {preview && (
                <div className="data-transfer-preview">
                  <div className="data-transfer-preview__heading">
                    <div>
                      <span>Validated export</span>
                      <strong>{preview.filename}</strong>
                    </div>
                    <span>Created {formatDate(preview.exportedAt)}</span>
                  </div>
                  <RecordCounts counts={preview.incoming} />
                  <div className="data-transfer-warning">
                    <strong>This replaces the current data in this Windows app.</strong>
                    <p>
                      The import is transactional: if any record fails, the existing local
                      data remains unchanged.
                    </p>
                  </div>
                  <label className="data-transfer-confirmation">
                    <input
                      type="checkbox"
                      checked={confirmed}
                      disabled={isImporting}
                      onChange={(event) => setConfirmed(event.target.checked)}
                    />
                    <span>I understand that the current local data will be replaced.</span>
                  </label>
                  {importError && <div className="alert alert--error">{importError}</div>}
                  <button
                    className="button button--danger"
                    type="button"
                    disabled={!confirmed || isImporting}
                    onClick={() => void handleImport()}
                  >
                    {isImporting ? 'Importing data…' : 'Replace local data and import'}
                  </button>
                </div>
              )}

              {importResult && (
                <div className="data-transfer-complete">
                  <span aria-hidden="true">✓</span>
                  <div>
                    <strong>Import complete</strong>
                    <p>{totalRecords(importResult.imported)} records are now available locally.</p>
                  </div>
                  <Link className="button button--secondary button--compact" to="/dashboard">
                    Open dashboard
                  </Link>
                </div>
              )}
            </>
          )}
        </div>
      </section>
    </div>
  )
}
