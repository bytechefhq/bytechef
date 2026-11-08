import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {NodeDataType} from '@/shared/types';
import {beforeEach, describe, expect, it} from 'vitest';

import getSelectedTestRunTriggerName from './getSelectedTestRunTriggerName';

const workflow = {
    triggers: [
        {name: 'trigger_1', type: 'webhook/v1/webhook'},
        {name: 'trigger_2', type: 'webhook/v1/webhook'},
    ],
};

describe('getSelectedTestRunTriggerName', () => {
    beforeEach(() => {
        useWorkflowNodeDetailsPanelStore.setState({currentNode: undefined, workflowNodeDetailsPanelOpen: false});
    });

    it('returns the trigger whose details panel is open', () => {
        useWorkflowNodeDetailsPanelStore.setState({
            currentNode: {name: 'trigger_2', trigger: true, workflowNodeName: 'trigger_2'} as NodeDataType,
            workflowNodeDetailsPanelOpen: true,
        });

        expect(getSelectedTestRunTriggerName(workflow)).toBe('trigger_2');
    });

    it('returns the first trigger when the details panel is closed', () => {
        useWorkflowNodeDetailsPanelStore.setState({
            currentNode: {name: 'trigger_2', trigger: true, workflowNodeName: 'trigger_2'} as NodeDataType,
            workflowNodeDetailsPanelOpen: false,
        });

        expect(getSelectedTestRunTriggerName(workflow)).toBe('trigger_1');
    });

    it('returns the first trigger when the open node is a task', () => {
        useWorkflowNodeDetailsPanelStore.setState({
            currentNode: {name: 'task_1', workflowNodeName: 'task_1'} as NodeDataType,
            workflowNodeDetailsPanelOpen: true,
        });

        expect(getSelectedTestRunTriggerName(workflow)).toBe('trigger_1');
    });

    it('returns undefined when the workflow has no triggers', () => {
        expect(getSelectedTestRunTriggerName({})).toBeUndefined();
    });

    it('skips a disabled first trigger when the details panel is closed', () => {
        expect(
            getSelectedTestRunTriggerName({
                triggers: [
                    {disabled: true, name: 'trigger_1', type: 'webhook/v1/webhook'},
                    {disabled: false, name: 'trigger_2', type: 'webhook/v1/webhook'},
                ],
            })
        ).toBe('trigger_2');
    });
});
