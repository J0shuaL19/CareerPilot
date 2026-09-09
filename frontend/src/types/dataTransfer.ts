export interface DataTransferCapabilities {
  importEnabled: boolean
  formatVersion: number
  maxImportFileSizeBytes: number
}

export interface DataTransferCounts {
  jobs: number
  resumes: number
  jobActivities: number
  matchAnalyses: number
  attentionSettings: number
  attentionEvents: number
  interviewPreparations: number
}

export interface DataTransferPreview {
  filename: string
  formatVersion: number
  exportedAt: string
  incoming: DataTransferCounts
  existing: DataTransferCounts
  willReplaceExistingData: boolean
}

export interface DataTransferImportResult {
  importedAt: string
  imported: DataTransferCounts
}
