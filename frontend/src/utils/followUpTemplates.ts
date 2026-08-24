export type FollowUpTemplateId =
  | 'APPLICATION_CHECK_IN'
  | 'POST_INTERVIEW_THANK_YOU'
  | 'TIMELINE_UPDATE'

export interface FollowUpTemplate {
  id: FollowUpTemplateId
  label: string
  description: string
}

interface FollowUpTemplateContext {
  company: string
  jobTitle: string
  contact: string
}

export const followUpTemplates: ReadonlyArray<FollowUpTemplate> = [
  {
    id: 'APPLICATION_CHECK_IN',
    label: 'Application check-in',
    description: 'Ask for an update after applying.',
  },
  {
    id: 'POST_INTERVIEW_THANK_YOU',
    label: 'Post-interview thanks',
    description: 'Reinforce interest after a conversation.',
  },
  {
    id: 'TIMELINE_UPDATE',
    label: 'Timeline update',
    description: 'Confirm next steps and timing.',
  },
]

export function buildFollowUpMessage(
  templateId: FollowUpTemplateId,
  context: FollowUpTemplateContext,
): string {
  const greeting = context.contact.trim() || 'there'

  if (templateId === 'POST_INTERVIEW_THANK_YOU') {
    return [
      `Hi ${greeting},`,
      '',
      `Thank you again for taking the time to speak with me about the ${context.jobTitle} role at ${context.company}. I enjoyed learning more about the team and remain excited about the opportunity.`,
      '',
      'Please let me know if I can provide any additional information.',
      '',
      'Best,',
    ].join('\n')
  }

  if (templateId === 'TIMELINE_UPDATE') {
    return [
      `Hi ${greeting},`,
      '',
      `I wanted to check in on the hiring timeline for the ${context.jobTitle} role at ${context.company}. I remain very interested and would appreciate any update you can share about next steps.`,
      '',
      'Thank you for your time.',
    ].join('\n')
  }

  return [
    `Hi ${greeting},`,
    '',
    `I wanted to follow up on my application for the ${context.jobTitle} role at ${context.company}. I remain very interested in the opportunity and would appreciate any update you can share.`,
    '',
    'Thank you for your time.',
  ].join('\n')
}
