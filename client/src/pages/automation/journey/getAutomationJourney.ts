import {type JourneyStepStatusType} from '@/components/JourneySteps/JourneySteps';
import {Project, ProjectDeployment} from '@/shared/middleware/automation/configuration';

export type AutomationJourneyStageType = 'createProject' | 'buildWorkflow' | 'publish' | 'deploy' | 'enable' | 'live';

export type AutomationJourneyStepIdType = 'createProject' | 'buildWorkflow' | 'publish' | 'deploy';

export interface AutomationJourneyI {
    /* The one project the user can publish next, set only in the publish stage when exactly one qualifies. */
    nextProject?: Project;
    stage: AutomationJourneyStageType;
    stepStatuses: Record<AutomationJourneyStepIdType, JourneyStepStatusType>;
}

export const AUTOMATION_JOURNEY_STEP_IDS: AutomationJourneyStepIdType[] = [
    'createProject',
    'buildWorkflow',
    'publish',
    'deploy',
];

// Enabling finishes the deploy step, so it keeps that step current; once live, every step is complete.
const getCurrentStepIndex = (stage: AutomationJourneyStageType) => {
    if (stage === 'live') {
        return AUTOMATION_JOURNEY_STEP_IDS.length;
    }

    return AUTOMATION_JOURNEY_STEP_IDS.indexOf(stage === 'enable' ? 'deploy' : stage);
};

const getStepStatus = (stepIndex: number, currentStepIndex: number): JourneyStepStatusType => {
    if (stepIndex < currentStepIndex) {
        return 'complete';
    }

    return stepIndex === currentStepIndex ? 'current' : 'upcoming';
};

const hasWorkflows = (project: Project) => (project.projectWorkflowIds?.length ?? 0) > 0;

// Triggers fire only for a workflow that is enabled inside an enabled deployment.
const canRun = (projectDeployment: ProjectDeployment) =>
    !!projectDeployment.enabled &&
    !!projectDeployment.projectDeploymentWorkflows?.some(
        (projectDeploymentWorkflow) => projectDeploymentWorkflow.enabled
    );

/*
 * Each signal implies the ones before it, so the stage follows the furthest signal that holds. A published project
 * whose current draft has no workflows has still been through the workflow step.
 */
const getStage = (projects: Project[], projectDeployments: ProjectDeployment[]): AutomationJourneyStageType => {
    if (projectDeployments.some(canRun)) {
        return 'live';
    }

    if (projectDeployments.length > 0) {
        return 'enable';
    }

    if (projects.some((project) => project.lastPublishedDate != null)) {
        return 'deploy';
    }

    if (projects.some(hasWorkflows)) {
        return 'publish';
    }

    if (projects.length > 0) {
        return 'buildWorkflow';
    }

    return 'createProject';
};

export interface AutomationJourneyStageSignalsI {
    deployed: boolean;
    live: boolean;
    projectCreated: boolean;
    published: boolean;
    workflowCreated: boolean;
}

const getStageFromSignals = (signals: AutomationJourneyStageSignalsI): AutomationJourneyStageType => {
    if (signals.live) {
        return 'live';
    }

    if (signals.deployed) {
        return 'enable';
    }

    if (signals.published) {
        return 'deploy';
    }

    if (signals.workflowCreated) {
        return 'publish';
    }

    return signals.projectCreated ? 'buildWorkflow' : 'createProject';
};

const toJourney = (stage: AutomationJourneyStageType, nextProject?: Project): AutomationJourneyI => {
    const currentStepIndex = getCurrentStepIndex(stage);

    const stepStatuses = Object.fromEntries(
        AUTOMATION_JOURNEY_STEP_IDS.map((stepId, index) => [stepId, getStepStatus(index, currentStepIndex)])
    ) as Record<AutomationJourneyStepIdType, JourneyStepStatusType>;

    return {nextProject, stage, stepStatuses};
};

export const getAutomationJourney = (
    projects: Project[],
    projectDeployments: ProjectDeployment[]
): AutomationJourneyI => {
    const stage = getStage(projects, projectDeployments);

    const publishableProjects = stage === 'publish' ? projects.filter(hasWorkflows) : [];

    return toJourney(stage, publishableProjects.length === 1 ? publishableProjects[0] : undefined);
};

export const getAutomationJourneyFromSignals = (signals: AutomationJourneyStageSignalsI): AutomationJourneyI =>
    toJourney(getStageFromSignals(signals));
