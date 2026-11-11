import {isIntegrationConfigurable, isIntegrationPublished} from '@/ee/pages/embedded/shared/integrationPublication';
import {Integration} from '@/ee/shared/middleware/embedded/configuration';
import {describe, expect, it} from 'vitest';

const draftIntegration = {
    componentName: 'httpClient',
    integrationWorkflowIds: [],
    lastIntegrationVersion: 1,
    multipleInstances: false,
} as Integration;

const publishedIntegration = {
    ...draftIntegration,
    integrationWorkflowIds: [7],
    lastIntegrationVersion: 2,
    lastPublishedDate: new Date(),
} as Integration;

describe('isIntegrationPublished', () => {
    it('is true only once a version has been published', () => {
        expect(isIntegrationPublished(draftIntegration)).toBe(false);
        expect(isIntegrationPublished(publishedIntegration)).toBe(true);
    });
});

describe('isIntegrationConfigurable', () => {
    it('allows an unpublished integration without workflows', () => {
        expect(isIntegrationConfigurable(draftIntegration)).toBe(true);
    });

    it('refuses an unpublished integration with workflows', () => {
        expect(isIntegrationConfigurable({...draftIntegration, integrationWorkflowIds: [7]})).toBe(false);
    });

    it('allows a published integration whatever its draft holds', () => {
        expect(isIntegrationConfigurable(publishedIntegration)).toBe(true);
        expect(isIntegrationConfigurable({...publishedIntegration, integrationWorkflowIds: []})).toBe(true);
    });
});
