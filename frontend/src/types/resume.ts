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
