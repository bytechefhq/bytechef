import {
    getIntegrationSubSteps,
    getTestUserSubSteps,
} from '@/ee/pages/embedded/getting-started/getEmbeddedGettingStartedSubSteps';
import {EmbeddedGettingStartedQuery} from '@/shared/middleware/graphql';
import {describe, expect, it} from 'vitest';

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

describe('getIntegrationSubSteps', () => {
    it('lists the four integration sub-steps in order, none done on an empty tenant', () => {
        const subSteps = getIntegrationSubSteps(baseStatus);

        expect(subSteps.map((subStep) => [subStep.id, subStep.label, subStep.done])).toEqual([
            ['create', 'Create an integration', false],
            ['publish', 'Publish a version', false],
            ['configure', 'Create a configuration in Development', false],
            ['enable', 'Enable the configuration', false],
        ]);
    });

    it('leaves out publishing and enabling for an integration without workflows', () => {
        const subSteps = getIntegrationSubSteps({
            ...baseStatus,
            integrationCreated: true,
            integrationHasWorkflows: false,
        });

        expect(subSteps.map((subStep) => subStep.id)).toEqual(['create', 'configure']);
    });

    it('keeps enabling for an integration without workflows whose configuration is still disabled', () => {
        const subSteps = getIntegrationSubSteps({
            ...baseStatus,
            integrationConfigurationCreated: true,
            integrationCreated: true,
            integrationHasWorkflows: false,
        });

        expect(subSteps.map((subStep) => subStep.id)).toEqual(['create', 'configure', 'enable']);
    });

    it('marks each sub-step done from its own status field', () => {
        const subSteps = getIntegrationSubSteps({
            ...baseStatus,
            integrationConfigurationCreated: true,
            integrationCreated: true,
            integrationPublished: true,
        });

        expect(subSteps.map((subStep) => subStep.done)).toEqual([true, true, true, false]);
    });

    it('points the configuration sub-steps at the integration list', () => {
        const subSteps = getIntegrationSubSteps(baseStatus);

        expect(subSteps.map((subStep) => subStep.href)).toEqual([
            '/embedded/integrations',
            '/embedded/integrations',
            '/embedded/integrations',
            '/embedded/integrations',
        ]);
        expect(subSteps[2].hint).toBe('Integration menu (⋮) → New Configuration');
        expect(subSteps[3].hint).toBe('Expand the integration → Configurations tab → switch it on');
    });
});

describe('getTestUserSubSteps', () => {
    it('lists adding the user and provisioning, none done on an empty tenant', () => {
        const subSteps = getTestUserSubSteps(baseStatus);

        expect(subSteps.map((subStep) => [subStep.id, subStep.label, subStep.done, subStep.href])).toEqual([
            ['add', 'Add a connected user', false, '/embedded/connected-users'],
            ['provision', 'Provision your integration and connect their account', false, '/embedded/connected-users'],
        ]);
    });

    it('marks adding done before provisioning', () => {
        const subSteps = getTestUserSubSteps({...baseStatus, testUserAdded: true});

        expect(subSteps.map((subStep) => subStep.done)).toEqual([true, false]);
    });

    it('marks both done once the test user is connected', () => {
        const subSteps = getTestUserSubSteps({...baseStatus, testUserAdded: true, testUserConnected: true});

        expect(subSteps.map((subStep) => subStep.done)).toEqual([true, true]);
    });
});
