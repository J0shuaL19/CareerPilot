import { useState } from 'react'
import { ResumeFileImport } from '../components/ResumeFileImport'
import { ResumeForm } from '../components/ResumeForm'
import { ApiError } from '../services/apiClient'
import { createResume, extractResumeFile } from '../services/resumeApi'
import type { CreateResumeInput, Resume } from '../types/resume'
import { getErrorMessage } from '../utils/errors'

interface AddResumePageProps {
  onCancel: () => void
  onCreated: (resume: Resume) => void
}

const maxFileSizeBytes = 5 * 1024 * 1024
const emptyResume: CreateResumeInput = { name: '', content: '' }

export function AddResumePage({ onCancel, onCreated }: AddResumePageProps) {
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [isImporting, setIsImporting] = useState(false)
  const [initialValues, setInitialValues] = useState<CreateResumeInput>(emptyResume)
  const [formVersion, setFormVersion] = useState(0)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [importError, setImportError] = useState<string | null>(null)
  const [importNotice, setImportNotice] = useState<string | null>(null)

  async function handleFileSelect(file: File) {
    setImportError(null)
    setImportNotice(null)

    const extension = file.name.split('.').pop()?.toLowerCase()
    if (extension !== 'pdf' && extension !== 'docx') {
      setImportError('Choose a PDF or DOCX resume file.')
      return
    }
    if (file.size > maxFileSizeBytes) {
      setImportError('Resume files must be 5 MB or smaller.')
      return
    }

    setIsImporting(true)
    try {
      const extraction = await extractResumeFile(file)
      setInitialValues({ name: extraction.suggestedName, content: extraction.content })
      setFormVersion((current) => current + 1)
      setFieldErrors({})
      setFormError(null)
      setImportNotice(`${file.name} imported. Review the extracted text before saving.`)
    } catch (error) {
      setImportError(getErrorMessage(error))
    } finally {
      setIsImporting(false)
    }
  }

  async function handleSubmit(input: CreateResumeInput) {
    setIsSubmitting(true)
    setFieldErrors({})
    setFormError(null)

    try {
      const resume = await createResume(input)
      onCreated(resume)
    } catch (error) {
      if (error instanceof ApiError) {
        setFieldErrors(error.fieldErrors)
      }
      setFormError(getErrorMessage(error))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="page page--narrow">
      <header className="page-header">
        <p className="page-header__eyebrow">Resume library</p>
        <h1>Add a resume</h1>
        <p>Import a PDF or DOCX, or paste the text manually, then review it before saving.</p>
      </header>

      <ResumeFileImport
        isImporting={isImporting}
        error={importError}
        notice={importNotice}
        onFileSelect={handleFileSelect}
      />

      <div className="resume-import-divider" aria-hidden="true">
        <span>or enter manually</span>
      </div>

      <section className="panel" aria-label="Add resume form">
        <ResumeForm
          key={formVersion}
          initialValues={initialValues}
          fieldErrors={fieldErrors}
          formError={formError}
          isSubmitting={isSubmitting}
          onSubmit={handleSubmit}
          onCancel={onCancel}
        />
      </section>
    </div>
  )
}
