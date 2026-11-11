import {EmbeddedGettingStartedQuery} from '@/shared/middleware/graphql';
import {describe, expect, it} from 'vitest';

import {
    getEmbeddedGettingStartedStepStatuses,
    getIntegrationSubStage,
    isListeningForFirstCall,
} from './getEmbeddedGettingStartedSteps';

type StatusType = EmbeddedGettingStartedQuery['embeddedGettingStarted'];

const baseStatus: StatusType = {
    apiBaseUrl: null,
    apiKeyCreated: false,
    completed: false,
    completionType: null,
    componentName: null,
    componentVersion: null,
    firstCallMade: false,
    integrationConfigurationCreated: false,
    integrationConfigurationEnabled: false,
    integrationCreated: false,
    integrationHasWorkflows: true,
    integrationId: null,
    integrationInstanceConfigurationId: null,
    integrationPublished: false,
    lastFailedCall: null,
    testUserAdded: false,
    testUserConnected: false,
    testUserExternalId: null,
    testUserId: null,
};

describe('getEmbeddedGettingStartedStepStatuses', () => {
    it('marks the first step current when nothing is done', () => {
        expect(getEmbeddedGettingStartedStepStatuses(baseStatus)).toEqual({
            apiKey: 'current',
            firstCall: 'upcoming',
            integration: 'upcoming',
            testUser: 'upcoming',
        });
    });

    it('marks only the first incomplete step current', () => {
        expect(getEmbeddedGettingStartedStepStatuses({...baseStatus, apiKeyCreated: true})).toEqual({
            apiKey: 'complete',
            firstCall: 'upcoming',
            integration: 'current',
            testUser: 'upcoming',
        });
    });

    it('marks every step complete when all checks hold', () => {
        expect(
            getEmbeddedGettingStartedStepStatuses({
                ...baseStatus,
                apiKeyCreated: true,
                firstCallMade: true,
                integrationConfigurationEnabled: true,
                integrationCreated: true,
                integrationPublished: true,
                testUserConnected: true,
            })
        ).toEqual({apiKey: 'complete', firstCall: 'complete', integration: 'complete', testUser: 'complete'});
    });
});

describe('getIntegrationSubStage', () => {
    it('returns create when nothing exists', () => {
        expect(getIntegrationSubStage(baseStatus)).toBe('create');
    });

    it('returns publish when created but unpublished', () => {
        expect(getIntegrationSubStage({...baseStatus, integrationCreated: true})).toBe('publish');
    });

    it('skips publish for an integration without workflows', () => {
        expect(getIntegrationSubStage({...baseStatus, integrationCreated: true, integrationHasWorkflows: false})).toBe(
            'enable'
        );
    });

    it('returns enable when published without enabled configuration', () => {
        expect(getIntegrationSubStage({...baseStatus, integrationCreated: true, integrationPublished: true})).toBe(
            'enable'
        );
    });

    it('returns done when the configuration is enabled', () => {
        expect(
            getIntegrationSubStage({
                ...baseStatus,
                integrationConfigurationEnabled: true,
                integrationCreated: true,
                integrationPublished: true,
            })
        ).toBe('done');
    });
});

describe('isListeningForFirstCall', () => {
    const readyStatus: StatusType = {
        ...baseStatus,
        apiKeyCreated: true,
        integrationConfigurationEnabled: true,
        integrationCreated: true,
        integrationPublished: true,
        testUserConnected: true,
    };

    it('listens before the first three steps are done', () => {
        expect(isListeningForFirstCall(baseStatus)).toBe(true);
        expect(isListeningForFirstCall(readyStatus)).toBe(true);
    });

    it('stops listening once the first call is made', () => {
        expect(isListeningForFirstCall({...readyStatus, firstCallMade: true})).toBe(false);
    });
});
