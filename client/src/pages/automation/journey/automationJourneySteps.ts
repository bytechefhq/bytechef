import {type JourneyStepI, type JourneyStepStatusType} from '@/components/JourneySteps/JourneySteps';

import {AUTOMATION_JOURNEY_STEP_IDS, type AutomationJourneyStepIdType} from './getAutomationJourney';

export const A2A_SERVERS_DOCUMENTATION_URL = 'https://docs.bytechef.io/platform/automation/deploy/a2a-servers';

export const BUILD_FIRST_AGENT_DOCUMENTATION_URL =
    'https://docs.bytechef.io/platform/automation/get-started/quick-start/build-first-agent';

export const BUILD_FIRST_WORKFLOW_DOCUMENTATION_URL =
    'https://docs.bytechef.io/platform/automation/get-started/quick-start/build-first-workflow';

export const CHATS_DOCUMENTATION_URL = 'https://docs.bytechef.io/platform/automation/deploy/chats';

export const CONNECTIONS_DOCUMENTATION_URL = 'https://docs.bytechef.io/platform/automation/build/connections';

export const CONFIGURE_WORKFLOW_TRIGGER_DOCUMENTATION_URL =
    'https://docs.bytechef.io/platform/automation/get-started/quick-start/configure-workflow-trigger';

export const DATA_TABLES_DOCUMENTATION_URL = 'https://docs.bytechef.io/platform/automation/data/data-tables';

export const DEPLOYMENTS_DOCUMENTATION_URL = 'https://docs.bytechef.io/platform/automation/deploy/workflows';

export const KNOWLEDGE_BASES_DOCUMENTATION_URL = 'https://docs.bytechef.io/platform/automation/data/knowledge-base';

export const MCP_SERVERS_DOCUMENTATION_URL = 'https://docs.bytechef.io/platform/automation/deploy/mcp-servers';

export const PROJECTS_DOCUMENTATION_URL = 'https://docs.bytechef.io/platform/automation/build/workflows/projects';

export const EXTERNAL_INVOCATIONS_DOCUMENTATION_URL =
    'https://docs.bytechef.io/platform/automation/monitor/external-invocations';

export const STAR_REPOSITORY_ON_GITHUB_DOCUMENTATION_URL =
    'https://docs.bytechef.io/platform/automation/get-started/quick-start/star-repository-on-github';

export const WORKFLOW_EXECUTIONS_DOCUMENTATION_URL =
    'https://docs.bytechef.io/platform/automation/monitor/workflow-executions';

const AUTOMATION_JOURNEY_STEP_LABELS: Record<AutomationJourneyStepIdType, string> = {
    buildWorkflow: 'Build and test a workflow',
    createProject: 'Create a project',
    deploy: 'Deploy and enable',
    publish: 'Publish a version',
};

/* Attaches currentStepContent (an action, a description) to whichever step is current. */
export const getAutomationJourneySteps = (
    stepStatuses: Record<AutomationJourneyStepIdType, JourneyStepStatusType>,
    currentStepContent?: Pick<JourneyStepI, 'action' | 'description'>
): JourneyStepI[] =>
    AUTOMATION_JOURNEY_STEP_IDS.map((stepId) => ({
        ...(stepStatuses[stepId] === 'current' ? currentStepContent : undefined),
        id: stepId,
        label: AUTOMATION_JOURNEY_STEP_LABELS[stepId],
        status: stepStatuses[stepId],
    }));
