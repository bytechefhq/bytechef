import {WorkflowInputType} from '@/shared/types';
import {describe, expect, it} from 'vitest';

import {toWorkflowDefinitionInput} from './toWorkflowDefinitionInput';

describe('toWorkflowDefinitionInput', () => {
    it('flattens a component reference into flat definition keys and drops testValue', () => {
        const input = {
            componentReference: {componentName: 'slack', componentVersion: 2, groupName: 'channel'},
            label: 'Channel',
            name: 'channel',
            required: false,
            testValue: 'ignored',
        } as WorkflowInputType;

        expect(toWorkflowDefinitionInput(input)).toEqual({
            componentName: 'slack',
            componentVersion: 2,
            groupName: 'channel',
            label: 'Channel',
            name: 'channel',
            required: false,
        });
    });

    it('returns a primitive input unchanged except for testValue', () => {
        const input = {
            label: 'Email',
            name: 'email',
            required: true,
            testValue: 'a@b.com',
            type: 'string',
        } as WorkflowInputType;

        expect(toWorkflowDefinitionInput(input)).toEqual({
            label: 'Email',
            name: 'email',
            required: true,
            type: 'string',
        });
    });

    it('preserves internalOnly and drops testValue', () => {
        const result = toWorkflowDefinitionInput({
            internalOnly: true,
            label: 'API Key',
            name: 'apiKey',
            testValue: 'x',
            type: 'string',
        } as WorkflowInputType) as WorkflowInputType;

        expect(result.internalOnly).toBe(true);
        expect('testValue' in result).toBe(false);
    });
});
