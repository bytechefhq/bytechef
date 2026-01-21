import {WorkflowNodeOutput} from '@/shared/middleware/platform/configuration';
import {PropertyAllType} from '@/shared/types';
import {describe, expect, test} from 'vitest';

import getOutputSchemaFromWorkflowNodeOutput from './getOutputSchemaFromWorkflowNodeOutput';

describe('getOutputSchemaFromWorkflowNodeOutput', () => {
    const mockOutputSchema: PropertyAllType = {
        name: 'testOutput',
        type: 'OBJECT',
    };

    const mockVariableOutputSchema: PropertyAllType = {
        name: 'variableOutput',
        properties: [
            {name: 'item', type: 'OBJECT'},
            {name: 'index', type: 'INTEGER'},
        ],
        type: 'OBJECT',
    };

    test('returns undefined when workflowNodeOutput is undefined', () => {
        const result = getOutputSchemaFromWorkflowNodeOutput(undefined);

        expect(result).toBeUndefined();
    });

    test('returns outputResponse.outputSchema when available', () => {
        const workflowNodeOutput: WorkflowNodeOutput = {
            outputResponse: {
                outputSchema: mockOutputSchema,
            },
            workflowNodeName: 'action_1',
        };

        const result = getOutputSchemaFromWorkflowNodeOutput(workflowNodeOutput);

        expect(result).toBe(mockOutputSchema);
    });

    test('returns variableOutputResponse.outputSchema when outputResponse is not available', () => {
        const workflowNodeOutput: WorkflowNodeOutput = {
            variableOutputResponse: {
                outputSchema: mockVariableOutputSchema,
            },
            workflowNodeName: 'loop_1',
        };

        const result = getOutputSchemaFromWorkflowNodeOutput(workflowNodeOutput);

        expect(result).toBe(mockVariableOutputSchema);
    });

    test('prefers outputResponse over variableOutputResponse when both are available', () => {
        const workflowNodeOutput: WorkflowNodeOutput = {
            outputResponse: {
                outputSchema: mockOutputSchema,
            },
            variableOutputResponse: {
                outputSchema: mockVariableOutputSchema,
            },
            workflowNodeName: 'action_1',
        };

        const result = getOutputSchemaFromWorkflowNodeOutput(workflowNodeOutput);

        expect(result).toBe(mockOutputSchema);
    });

    test('returns undefined when neither outputResponse nor variableOutputResponse has outputSchema', () => {
        const workflowNodeOutput: WorkflowNodeOutput = {
            outputResponse: {},
            variableOutputResponse: {},
            workflowNodeName: 'action_1',
        };

        const result = getOutputSchemaFromWorkflowNodeOutput(workflowNodeOutput);

        expect(result).toBeUndefined();
    });

    test('returns variableOutputResponse.outputSchema for loop task dispatcher with item and index properties', () => {
        const loopVariableSchema: PropertyAllType = {
            name: 'loopOutput',
            properties: [
                {
                    name: 'item',
                    properties: [{name: 'id', type: 'STRING'}],
                    type: 'OBJECT',
                },
                {name: 'index', type: 'INTEGER'},
            ],
            type: 'OBJECT',
        };

        const workflowNodeOutput: WorkflowNodeOutput = {
            taskDispatcherDefinition: {
                name: 'loop',
                outputDefined: false,
                variablePropertiesDefined: true,
                version: 1,
            },
            variableOutputResponse: {
                outputSchema: loopVariableSchema,
            },
            workflowNodeName: 'loop_1',
        };

        const result = getOutputSchemaFromWorkflowNodeOutput(workflowNodeOutput);

        expect(result).toBe(loopVariableSchema);
        expect(result?.properties).toHaveLength(2);
        expect(result?.properties?.[0].name).toBe('item');
        expect(result?.properties?.[1].name).toBe('index');
    });
});
