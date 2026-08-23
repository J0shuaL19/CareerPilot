export interface Resume {
  id: number
  name: string
  content: string
  createdAt: string
}

export interface CreateResumeInput {
  name: string
  content: string
}

export interface ResumeExtraction {
  suggestedName: string
  content: string
}

export type UpdateResumeInput = CreateResumeInput
