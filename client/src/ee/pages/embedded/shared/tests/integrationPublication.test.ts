import {
    canPublishIntegration,
    isIntegrationConfigurable,
    isIntegrationConnectionOnly,
    isIntegrationPublished,
} from '@/ee/pages/embedded/shared/integrationPublication';
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

describe('isIntegrationConnectionOnly', () => {
    it('is true for a never-published integration without workflows', () => {
        expect(isIntegrationConnectionOnly(draftIntegration)).toBe(true);
    });

    it('is true for a published integration whose draft and published version are both empty', () => {
        expect(
            isIntegrationConnectionOnly({
                ...publishedIntegration,
                integrationWorkflowIds: [],
                unpublishedChanges: false,
            })
        ).toBe(true);
    });

    it('is false while the draft has workflows', () => {
        expect(isIntegrationConnectionOnly({...draftIntegration, integrationWorkflowIds: [7]})).toBe(false);
    });

    it('is false while the removal of all workflows is not yet published', () => {
        expect(
            isIntegrationConnectionOnly({...publishedIntegration, integrationWorkflowIds: [], unpublishedChanges: true})
        ).toBe(false);
    });

    it('is false for a code integration', () => {
        expect(isIntegrationConnectionOnly({...draftIntegration, codeWorkflow: true})).toBe(false);
    });
});

describe('canPublishIntegration', () => {
    it('allows an integration whose draft has workflows', () => {
        expect(canPublishIntegration({...draftIntegration, integrationWorkflowIds: [7]})).toBe(true);
    });

    it('allows publishing the removal of all workflows', () => {
        expect(
            canPublishIntegration({...publishedIntegration, integrationWorkflowIds: [], unpublishedChanges: true})
        ).toBe(true);
    });

    it('refuses an empty draft with nothing to publish', () => {
        expect(canPublishIntegration(draftIntegration)).toBe(false);
        expect(
            canPublishIntegration({...publishedIntegration, integrationWorkflowIds: [], unpublishedChanges: false})
        ).toBe(false);
    });
});
