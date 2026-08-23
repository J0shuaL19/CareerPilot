export type MatchScoreFilterValue = 'ALL' | 'HIGH' | 'MEDIUM' | 'LOW'
export type ResumeFilterValue = 'ALL' | number

export function matchesScoreFilter(score: number, filter: MatchScoreFilterValue): boolean {
  if (filter === 'HIGH') return score >= 75
  if (filter === 'MEDIUM') return score >= 50 && score < 75
  if (filter === 'LOW') return score < 50
  return true
}
