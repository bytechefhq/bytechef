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

const CURRENT_STEP_INDEX: Record<AutomationJourneyStageType, number> = {
    createProject: 0,
    // eslint-disable-next-line sort-keys
    buildWorkflow: 1,
    publish: 2,
    // eslint-disable-next-line sort-keys
    deploy: 3,
    enable: 3,
    live: AUTOMATION_JOURNEY_STEP_IDS.length,
};

const hasWorkflows = (project: Project) => (project.projectWorkflowIds?.length ?? 0) > 0;

/*
 * Each signal implies the ones before it, so the stage follows the furthest signal that holds. A published project
 * whose current draft has no workflows has still been through the workflow step.
 */
const getStage = (projects: Project[], projectDeployments: ProjectDeployment[]): AutomationJourneyStageType => {
    if (projectDeployments.some((projectDeployment) => projectDeployment.enabled)) {
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

export const getAutomationJourney = (
    projects: Project[],
    projectDeployments: ProjectDeployment[]
): AutomationJourneyI => {
    const stage = getStage(projects, projectDeployments);

    const currentStepIndex = CURRENT_STEP_INDEX[stage];

    const stepStatuses = Object.fromEntries(
        AUTOMATION_JOURNEY_STEP_IDS.map((stepId, index) => [
            stepId,
            index < currentStepIndex ? 'complete' : index === currentStepIndex ? 'current' : 'upcoming',
        ])
    ) as Record<AutomationJourneyStepIdType, JourneyStepStatusType>;

    const publishableProjects = stage === 'publish' ? projects.filter(hasWorkflows) : [];

    return {
        nextProject: publishableProjects.length === 1 ? publishableProjects[0] : undefined,
        stage,
        stepStatuses,
    };
};
