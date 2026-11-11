import {type JourneyStepStatusType} from '@/components/JourneySteps/JourneySteps';
import {
    AutomationJourneyI,
    AutomationJourneyStageType,
    AutomationJourneyStepIdType,
} from '@/pages/automation/journey/getAutomationJourney';

import {AutomationGettingStartedStatusType} from './automationGettingStartedStatus';

export type FirstRunStateType = 'waiting' | 'failed' | 'done';

const STAGE_ORDER: AutomationJourneyStageType[] = [
    'createProject',
    'buildWorkflow',
    'publish',
    'deploy',
    'enable',
    'live',
];

export const hasReachedStage = (stage: AutomationJourneyStageType, reachedStage: AutomationJourneyStageType) =>
    STAGE_ORDER.indexOf(stage) >= STAGE_ORDER.indexOf(reachedStage);

export const hasFirstRun = (status?: AutomationGettingStartedStatusType) =>
    !!status && (status.firstRun != null || status.completed);

export function getAutomationGettingStartedSteps(
    journey: AutomationJourneyI,
    status?: AutomationGettingStartedStatusType
): {firstRunState: FirstRunStateType; stepStatuses: Record<AutomationJourneyStepIdType, JourneyStepStatusType>} {
    if (hasFirstRun(status)) {
        return {
            firstRunState: 'done',
            stepStatuses: {
                buildWorkflow: 'complete',
                createProject: 'complete',
                deploy: 'complete',
                publish: 'complete',
            },
        };
    }

    const stepStatuses: Record<AutomationJourneyStepIdType, JourneyStepStatusType> = {
        ...journey.stepStatuses,
        deploy: hasReachedStage(journey.stage, 'deploy') ? 'current' : journey.stepStatuses.deploy,
    };

    if (journey.stage === 'publish') {
        stepStatuses.buildWorkflow = 'current';
        stepStatuses.publish = 'upcoming';
    }

    return {
        firstRunState: status?.lastFailedRun ? 'failed' : 'waiting',
        stepStatuses,
    };
}
