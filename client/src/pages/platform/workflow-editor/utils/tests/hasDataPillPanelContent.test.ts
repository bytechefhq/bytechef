import {WorkflowInput, WorkflowNodeOutput} from '@/shared/middleware/platform/configuration';
import {describe, expect, it} from 'vitest';

import hasDataPillPanelContent from '../hasDataPillPanelContent';

const manualTrigger = {
    triggerDefinition: {componentName: 'manual', outputDefined: false},
    workflowNodeName: 'trigger_1',
} as unknown as WorkflowNodeOutput;

const actionWithOutput = {
    actionDefinition: {componentName: 'activeCampaign', outputDefined: true},
    workflowNodeName: 'activeCampaign_1',
} as unknown as WorkflowNodeOutput;

const workflowInput = {name: 'email', type: 'STRING'} as unknown as WorkflowInput;

describe('hasDataPillPanelContent', () => {
    it('reports no content for a workflow that only has a manual trigger', () => {
        expect(hasDataPillPanelContent([manualTrigger], 'activeCampaign_1', [])).toBe(false);
    });

    it('reports content when an earlier node defines an output', () => {
        expect(hasDataPillPanelContent([manualTrigger, actionWithOutput], 'mailchimp_1', [])).toBe(true);
    });

    it('reports no content when the only node with an output is the current one', () => {
        expect(hasDataPillPanelContent([actionWithOutput], 'activeCampaign_1', [])).toBe(false);
    });

    it('reports content for workflow inputs alone', () => {
        expect(hasDataPillPanelContent([manualTrigger], 'activeCampaign_1', [workflowInput])).toBe(true);
    });

    it('treats missing outputs and inputs as no content', () => {
        expect(hasDataPillPanelContent(undefined, undefined, undefined)).toBe(false);
    });
});
