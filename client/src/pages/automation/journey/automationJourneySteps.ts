import {type JourneyStepI, type JourneyStepStatusType} from '@/components/JourneySteps/JourneySteps';

import {AUTOMATION_JOURNEY_STEP_IDS, type AutomationJourneyStepIdType} from './getAutomationJourney';

export const CONNECTIONS_DOCUMENTATION_URL = 'https://docs.bytechef.io/platform/automation/build/connections';

export const DEPLOYMENTS_DOCUMENTATION_URL = 'https://docs.bytechef.io/platform/automation/deploy/workflows';

export const PROJECTS_DOCUMENTATION_URL = 'https://docs.bytechef.io/platform/automation/build/workflows/projects';

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
