import {EmbeddedGettingStartedQuery} from '@/shared/middleware/graphql';

import type {JourneyStepStatusType} from '@/components/JourneySteps/JourneySteps';

type EmbeddedGettingStartedStatusType = EmbeddedGettingStartedQuery['embeddedGettingStarted'];

export type EmbeddedGettingStartedStepIdType = 'apiKey' | 'integration' | 'testUser' | 'firstCall';

export type IntegrationSubStageType = 'create' | 'publish' | 'enable' | 'done';

const STEP_ORDER: EmbeddedGettingStartedStepIdType[] = ['apiKey', 'integration', 'testUser', 'firstCall'];

export function getIntegrationSubStage(status: EmbeddedGettingStartedStatusType): IntegrationSubStageType {
    if (!status.integrationCreated) {
        return 'create';
    }

    if (status.integrationHasWorkflows && !status.integrationPublished) {
        return 'publish';
    }

    if (!status.integrationConfigurationEnabled) {
        return 'enable';
    }

    return 'done';
}

export function getEmbeddedGettingStartedStepStatuses(
    status: EmbeddedGettingStartedStatusType
): Record<EmbeddedGettingStartedStepIdType, JourneyStepStatusType> {
    const completion: Record<EmbeddedGettingStartedStepIdType, boolean> = {
        apiKey: status.apiKeyCreated,
        firstCall: status.firstCallMade,
        integration: getIntegrationSubStage(status) === 'done',
        testUser: status.testUserConnected,
    };

    const statuses = {} as Record<EmbeddedGettingStartedStepIdType, JourneyStepStatusType>;

    let currentAssigned = false;

    for (const stepId of STEP_ORDER) {
        if (completion[stepId]) {
            statuses[stepId] = 'complete';
        } else if (!currentAssigned) {
            statuses[stepId] = 'current';
            currentAssigned = true;
        } else {
            statuses[stepId] = 'upcoming';
        }
    }

    return statuses;
}

export function isListeningForFirstCall(status: EmbeddedGettingStartedStatusType): boolean {
    return !status.firstCallMade;
}
