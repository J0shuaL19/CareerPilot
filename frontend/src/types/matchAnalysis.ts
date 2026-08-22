export interface MatchAnalysis {
  id: number
  jobId: number
  company: string
  jobTitle: string
  resumeId: number
  resumeName: string
  matchScore: number
  summary: string
  strengths: string
  gaps: string
  recommendations: string
  modelName: string
  createdAt: string
}

export interface CreateMatchAnalysisInput {
  jobId: number
  resumeId: number
}
