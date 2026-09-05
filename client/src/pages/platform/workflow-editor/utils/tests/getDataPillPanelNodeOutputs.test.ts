import {WorkflowNodeOutput} from '@/shared/middleware/platform/configuration';
import {describe, expect, it} from 'vitest';

import getDataPillPanelNodeOutputs from '../getDataPillPanelNodeOutputs';

const manualTrigger = {
    triggerDefinition: {componentName: 'manual', outputDefined: false},
    workflowNodeName: 'trigger_1',
} as unknown as WorkflowNodeOutput;

const actionWithOutput = {
    actionDefinition: {componentName: 'activeCampaign', outputDefined: true},
    workflowNodeName: 'activeCampaign_1',
} as unknown as WorkflowNodeOutput;

describe('getDataPillPanelNodeOutputs', () => {
    it('drops a manual trigger, which defines no output', () => {
        expect(getDataPillPanelNodeOutputs([manualTrigger], 'activeCampaign_2')).toEqual([]);
    });

    it('drops the current node so it never lists its own output', () => {
        expect(getDataPillPanelNodeOutputs([actionWithOutput], 'activeCampaign_1')).toEqual([]);
    });

    it('keeps a previous node that defines an output', () => {
        expect(getDataPillPanelNodeOutputs([manualTrigger, actionWithOutput], 'activeCampaign_2')).toEqual([
            actionWithOutput,
        ]);
    });

    it('keeps a task dispatcher whose variable properties are defined', () => {
        const loop = {
            taskDispatcherDefinition: {outputDefined: false, variablePropertiesDefined: true},
            workflowNodeName: 'loop_1',
        } as unknown as WorkflowNodeOutput;

        expect(getDataPillPanelNodeOutputs([loop], 'activeCampaign_1')).toEqual([loop]);
    });
});
