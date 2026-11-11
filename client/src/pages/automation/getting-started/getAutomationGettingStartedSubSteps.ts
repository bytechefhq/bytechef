import {GettingStartedSubStepI} from '@/components/GettingStarted/gettingStartedSubStep';
import {AutomationJourneyI} from '@/pages/automation/journey/getAutomationJourney';

import {AutomationGettingStartedStatusType} from './automationGettingStartedStatus';
import {hasFirstRun, hasReachedStage} from './getAutomationGettingStartedSteps';

const PROJECTS_HREF = '/automation/projects';

const WORKFLOW_CREATE_HINTS = {
    dialog: 'Opens the Create Workflow dialog',
    none: 'Create a project first',
};

export type WorkflowCreateModeType = 'dialog' | 'none';

export function getProjectSubSteps(
    journey: AutomationJourneyI,
    status?: AutomationGettingStartedStatusType
): GettingStartedSubStepI[] {
    return [
        {
            done: hasFirstRun(status) || hasReachedStage(journey.stage, 'buildWorkflow'),
            hint: 'Opens the Create Project dialog',
            href: PROJECTS_HREF,
            id: 'create',
            label: 'Create a project',
        },
    ];
}

export function getWorkflowSubSteps(
    journey: AutomationJourneyI,
    status: AutomationGettingStartedStatusType | undefined,
    guideWorkflowHref: string,
    createMode: WorkflowCreateModeType
): GettingStartedSubStepI[] {
    const finished = hasFirstRun(status);

    return [
        {
            disabled: createMode === 'none',
            done: finished || hasReachedStage(journey.stage, 'publish'),
            hint: WORKFLOW_CREATE_HINTS[createMode],
            href: PROJECTS_HREF,
            id: 'create',
            label: 'Create a workflow',
        },
        {
            done: finished || !!status?.httpClientAdded,
            hint: 'Click + below the trigger → HTTP Client → GET, URI https://dummyjson.com/quotes/random',
            href: guideWorkflowHref,
            id: 'fetch',
            label: 'Fetch a quote',
        },
        {
            done: finished || hasReachedStage(journey.stage, 'deploy'),
            hint: "Open the HTTP Client step's Output tab → Test Action",
            href: guideWorkflowHref,
            id: 'test',
            label: 'Test it',
        },
        {
            done: finished || !!status?.loggerAdded,
            hint: 'Click + below HTTP Client → Helpers tab → Logger → Info, click into Text, pick quote under HTTP Client in the Data Pill Panel, then click Test',
            href: guideWorkflowHref,
            id: 'log',
            label: 'Log it',
        },
    ];
}

export function getPublishSubSteps(
    journey: AutomationJourneyI,
    guideWorkflowHref: string,
    status?: AutomationGettingStartedStatusType
): GettingStartedSubStepI[] {
    return [
        {
            done: hasFirstRun(status) || hasReachedStage(journey.stage, 'deploy'),
            hint: 'In the editor, Getting started card → Publish',
            href: guideWorkflowHref,
            id: 'publish',
            label: 'Publish the project',
        },
    ];
}

export function getDeploySubSteps(
    journey: AutomationJourneyI,
    status: AutomationGettingStartedStatusType | undefined,
    guideWorkflowHref: string
): GettingStartedSubStepI[] {
    const finished = hasFirstRun(status);

    return [
        {
            done: finished || hasReachedStage(journey.stage, 'enable'),
            hint: 'In the editor, Getting started card → Deploy',
            href: guideWorkflowHref,
            id: 'deploy',
            label: 'Deploy it in Development',
        },
        {
            done: finished || journey.stage === 'live',
            hint: 'In the editor, Getting started card → Enable',
            href: guideWorkflowHref,
            id: 'enable',
            label: 'Enable the workflow and the deployment',
        },
        {
            done: finished,
            hint: 'In the editor, Getting started card → Run now',
            href: guideWorkflowHref,
            id: 'run',
            label: 'See it run',
        },
    ];
}
