import type {
  DataTransferCapabilities,
  DataTransferImportResult,
  DataTransferPreview,
} from '../types/dataTransfer'
import { apiDownload, apiRequest } from './apiClient'

export function getDataTransferCapabilities(
  signal?: AbortSignal,
): Promise<DataTransferCapabilities> {
  return apiRequest<DataTransferCapabilities>('/api/data-transfer/capabilities', { signal })
}

export function exportCareerPilotData(): Promise<Blob> {
  return apiDownload('/api/data-transfer/export', {
    headers: { Accept: 'application/json' },
  })
}

export function previewCareerPilotImport(file: File): Promise<DataTransferPreview> {
  const formData = new FormData()
  formData.append('file', file)
  return apiRequest<DataTransferPreview>('/api/data-transfer/import/preview', {
    method: 'POST',
    body: formData,
  })
}

export function importCareerPilotData(file: File): Promise<DataTransferImportResult> {
  const formData = new FormData()
  formData.append('file', file)
  return apiRequest<DataTransferImportResult>('/api/data-transfer/import', {
    method: 'POST',
    body: formData,
  })
}
