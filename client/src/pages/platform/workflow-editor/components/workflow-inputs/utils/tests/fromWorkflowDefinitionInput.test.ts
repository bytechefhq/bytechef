import {fromWorkflowDefinitionInput} from '@/pages/platform/workflow-editor/components/workflow-inputs/utils/fromWorkflowDefinitionInput';
import {WorkflowInput} from '@/shared/middleware/platform/configuration';
import {describe, expect, it} from 'vitest';

describe('fromWorkflowDefinitionInput', () => {
    it('rebuilds a nested componentReference from flat definition keys', () => {
        const input = {
            componentName: 'slack',
            componentVersion: 2,
            groupName: 'channel',
            label: 'Channel',
            name: 'channel',
            required: false,
        } as unknown as WorkflowInput;

        expect(fromWorkflowDefinitionInput(input)).toEqual({
            componentReference: {componentName: 'slack', componentVersion: 2, groupName: 'channel'},
            label: 'Channel',
            name: 'channel',
            required: false,
        });
    });

    it('passes a primitive input through unchanged', () => {
        const input = {label: 'Email', name: 'email', required: true, type: 'string'} as WorkflowInput;

        expect(fromWorkflowDefinitionInput(input)).toEqual(input);
    });
});
