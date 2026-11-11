import {NextStepI} from '@/components/GettingStarted/GettingStartedNextSteps';

import {
    BUILD_FIRST_AGENT_DOCUMENTATION_URL,
    CONFIGURE_WORKFLOW_TRIGGER_DOCUMENTATION_URL,
    STAR_REPOSITORY_ON_GITHUB_DOCUMENTATION_URL,
} from '../journey/automationJourneySteps';

export function getAutomationNextSteps(guideProjectId?: number): NextStepI[] {
    return [
        {
            description: 'Start workflows from a schedule, a webhook or an app event.',
            external: true,
            href: CONFIGURE_WORKFLOW_TRIGGER_DOCUMENTATION_URL,
            title: 'Configure Workflow Trigger',
        },
        {
            description: 'Create an AI agent that decides which tools to use.',
            external: true,
            href: BUILD_FIRST_AGENT_DOCUMENTATION_URL,
            title: 'Build First Agent',
        },
        {
            description: 'Build your first workflow that uses a connection by starring a repository on GitHub.',
            external: true,
            href: STAR_REPOSITORY_ON_GITHUB_DOCUMENTATION_URL,
            title: 'Star Repository on GitHub',
        },
        ...(guideProjectId !== undefined
            ? [
                  {
                      description: 'Start from a ready-made workflow instead of a blank canvas.',
                      external: false,
                      href: `/automation/projects/${guideProjectId}/templates`,
                      title: 'Workflow Templates',
                  },
              ]
            : []),
        {
            description: 'See every run, including the one you just made.',
            external: false,
            href: '/automation/workflow-executions',
            title: 'Workflow Executions',
        },
    ];
}
