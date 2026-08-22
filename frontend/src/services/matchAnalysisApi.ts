import type {
  CreateMatchAnalysisInput,
  MatchAnalysis,
} from '../types/matchAnalysis'
import { apiRequest } from './apiClient'

export function createMatchAnalysis(
  input: CreateMatchAnalysisInput,
): Promise<MatchAnalysis> {
  return apiRequest<MatchAnalysis>('/api/match-analyses', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(input),
  })
}

export function getMatchAnalysis(
  id: number,
  signal?: AbortSignal,
): Promise<MatchAnalysis> {
  return apiRequest<MatchAnalysis>(`/api/match-analyses/${id}`, { signal })
}
