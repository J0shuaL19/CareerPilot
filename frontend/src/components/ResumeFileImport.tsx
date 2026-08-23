import { useState, type ChangeEvent, type DragEvent } from 'react'

interface ResumeFileImportProps {
  isImporting: boolean
  error: string | null
  notice: string | null
  onFileSelect: (file: File) => Promise<void>
}

const acceptedFileTypes =
  '.pdf,.docx,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document'

export function ResumeFileImport({
  isImporting,
  error,
  notice,
  onFileSelect,
}: ResumeFileImportProps) {
  const [isDragging, setIsDragging] = useState(false)

  async function importFile(file: File) {
    setIsDragging(false)
    await onFileSelect(file)
  }

  async function handleChange(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    event.target.value = ''

    if (file) {
      await importFile(file)
    }
  }

  function handleDragOver(event: DragEvent<HTMLLabelElement>) {
    event.preventDefault()
    if (!isImporting) {
      setIsDragging(true)
    }
  }

  function handleDragLeave(event: DragEvent<HTMLLabelElement>) {
    if (!event.currentTarget.contains(event.relatedTarget as Node | null)) {
      setIsDragging(false)
    }
  }

  async function handleDrop(event: DragEvent<HTMLLabelElement>) {
    event.preventDefault()
    if (isImporting) {
      return
    }

    const file = event.dataTransfer.files[0]
    if (file) {
      await importFile(file)
    }
  }

  return (
    <section className="resume-import" aria-labelledby="resume-import-title">
      <div className="resume-import__heading">
        <div>
          <p className="resume-import__eyebrow">Quick import</p>
          <h2 id="resume-import-title">Start from an existing resume</h2>
        </div>
        <span className="resume-import__format">PDF or DOCX · max 5 MB</span>
      </div>

      <label
        className={`resume-import__dropzone${isDragging ? ' resume-import__dropzone--active' : ''}${isImporting ? ' resume-import__dropzone--busy' : ''}`}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
      >
        <input
          className="resume-import__input"
          type="file"
          accept={acceptedFileTypes}
          disabled={isImporting}
          onChange={handleChange}
        />
        <span className="resume-import__icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" focusable="false">
            <path d="M12 16V4m0 0L7.5 8.5M12 4l4.5 4.5M5 14v4.25A1.75 1.75 0 0 0 6.75 20h10.5A1.75 1.75 0 0 0 19 18.25V14" />
          </svg>
        </span>
        <span className="resume-import__prompt">
          {isImporting ? 'Extracting resume text…' : 'Choose a file or drag it here'}
        </span>
        <span className="resume-import__detail">
          We extract the text and let you review it before anything is saved.
        </span>
      </label>

      {error && (
        <div className="alert alert--error resume-import__message" role="alert">
          {error}
        </div>
      )}
      {notice && (
        <div className="alert alert--success resume-import__message" role="status">
          <span aria-hidden="true">✓</span>
          {notice}
        </div>
      )}
    </section>
  )
}
