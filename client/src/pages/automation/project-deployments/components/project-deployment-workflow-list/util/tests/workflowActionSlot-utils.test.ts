import {ProjectDeploymentWorkflow, Workflow} from '@/shared/middleware/automation/configuration';
import {describe, expect, it} from 'vitest';

import {canRunWorkflowManually, hasWorkflowTriggerUrl} from '../workflowActionSlot-utils';

describe('canRunWorkflowManually', () => {
    it('allows a workflow without triggers', () => {
        expect(canRunWorkflowManually({triggers: []} as unknown as Workflow)).toBe(true);
    });

    it('allows a workflow with a manual trigger', () => {
        expect(canRunWorkflowManually({triggers: [{type: 'manual/v1/manual'}]} as unknown as Workflow)).toBe(true);
    });

    it('rejects a workflow with only non-manual triggers', () => {
        expect(canRunWorkflowManually({triggers: [{type: 'form/v1/newFormRequest'}]} as unknown as Workflow)).toBe(
            false
        );
    });
});

describe('hasWorkflowTriggerUrl', () => {
    it('is true when the deployment workflow has a static webhook url', () => {
        expect(
            hasWorkflowTriggerUrl({staticWebhookUrl: 'https://example.com/webhooks/abc'} as ProjectDeploymentWorkflow)
        ).toBe(true);
    });

    it('is false without a static webhook url or deployment workflow', () => {
        expect(hasWorkflowTriggerUrl({} as ProjectDeploymentWorkflow)).toBe(false);
        expect(hasWorkflowTriggerUrl(undefined)).toBe(false);
    });
});
