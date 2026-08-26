import {ComponentDefinitionBasic, OutputResponse, WorkflowNodeOutput} from '@/shared/middleware/platform/configuration';
import {describe, expect, it} from 'vitest';

import filterWorkflowNodeOutputs from './filterWorkflowNodeOutputs';

// ── Helpers ──────────────────────────────────────────────────────────

function makeActionOutput(componentName: string, workflowNodeName: string): WorkflowNodeOutput {
    return {
        actionDefinition: {
            componentName,
            componentVersion: 1,
            name: 'action',
            outputDefined: true,
            outputFunctionDefined: false,
        },
        workflowNodeName,
    };
}

function makeTriggerOutput(componentName: string, workflowNodeName: string): WorkflowNodeOutput {
    return {
        triggerDefinition: {
            componentName,
            componentVersion: 1,
            name: 'trigger',
            outputDefined: true,
            outputFunctionDefined: false,
            type: 'POLLING',
        },
        workflowNodeName,
    };
}

function makeOutputResponse(): OutputResponse {
    return {outputSchema: {type: 'OBJECT'}} as OutputResponse;
}

function makeTaskDispatcherOutput(
    name: string,
    workflowNodeName: string,
    options: {
        outputDefined?: boolean;
        outputSchema?: boolean;
        variableOutputSchema?: boolean;
        variablePropertiesDefined?: boolean;
    } = {}
): WorkflowNodeOutput {
    return {
        outputResponse: options.outputSchema ? makeOutputResponse() : undefined,
        taskDispatcherDefinition: {
            name,
            outputDefined: options.outputDefined ?? false,
            variablePropertiesDefined: options.variablePropertiesDefined ?? false,
            version: 1,
        },
        variableOutputResponse: options.variableOutputSchema ? makeOutputResponse() : undefined,
        workflowNodeName,
    };
}

function makeComponentDef(name: string): ComponentDefinitionBasic {
    return {name} as ComponentDefinitionBasic;
}

// ── Tests ────────────────────────────────────────────────────────────

describe('filterWorkflowNodeOutputs', () => {
    const componentDefinitions = [makeComponentDef('httpClient'), makeComponentDef('webhook')];
    const taskDispatcherDefinitions = [makeComponentDef('loop'), makeComponentDef('each'), makeComponentDef('map')];

    it('should include action outputs with matching component definition', () => {
        const outputs = [makeActionOutput('httpClient', 'httpClient_1')];

        const result = filterWorkflowNodeOutputs(outputs, componentDefinitions, taskDispatcherDefinitions);

        expect(result.outputs).toHaveLength(1);
        expect(result.outputs[0].workflowNodeName).toBe('httpClient_1');
        expect(result.definitions).toHaveLength(1);
        expect(result.definitions[0].name).toBe('httpClient');
    });

    it('should include trigger outputs with matching component definition', () => {
        const outputs = [makeTriggerOutput('webhook', 'webhook_trigger')];

        const result = filterWorkflowNodeOutputs(outputs, componentDefinitions, taskDispatcherDefinitions);

        expect(result.outputs).toHaveLength(1);
        expect(result.outputs[0].workflowNodeName).toBe('webhook_trigger');
        expect(result.definitions[0].name).toBe('webhook');
    });

    it('should include loop task dispatcher whose variable properties resolved to a schema', () => {
        const outputs = [
            makeTaskDispatcherOutput('loop', 'loop_1', {variableOutputSchema: true, variablePropertiesDefined: true}),
        ];

        const result = filterWorkflowNodeOutputs(outputs, componentDefinitions, taskDispatcherDefinitions);

        expect(result.outputs).toHaveLength(1);
        expect(result.outputs[0].workflowNodeName).toBe('loop_1');
        expect(result.definitions[0].name).toBe('loop');
    });

    it('should include each task dispatcher whose variable properties resolved to a schema', () => {
        const outputs = [
            makeTaskDispatcherOutput('each', 'each_1', {variableOutputSchema: true, variablePropertiesDefined: true}),
        ];

        const result = filterWorkflowNodeOutputs(outputs, componentDefinitions, taskDispatcherDefinitions);

        expect(result.outputs).toHaveLength(1);
        expect(result.outputs[0].workflowNodeName).toBe('each_1');
        expect(result.definitions[0].name).toBe('each');
    });

    it('should include task dispatchers whose output resolved to a schema', () => {
        const outputs = [makeTaskDispatcherOutput('map', 'map_1', {outputDefined: true, outputSchema: true})];

        const result = filterWorkflowNodeOutputs(outputs, componentDefinitions, taskDispatcherDefinitions);

        expect(result.outputs).toHaveLength(1);
        expect(result.outputs[0].workflowNodeName).toBe('map_1');
        expect(result.definitions[0].name).toBe('map');
    });

    it('should exclude task dispatchers that resolved to neither an output nor a variable schema', () => {
        const outputs = [makeTaskDispatcherOutput('condition', 'condition_1')];

        const result = filterWorkflowNodeOutputs(outputs, componentDefinitions, [
            ...taskDispatcherDefinitions,
            makeComponentDef('condition'),
        ]);

        expect(result.outputs).toHaveLength(0);
        expect(result.definitions).toHaveLength(0);
    });

    it('should exclude an enclosing task dispatcher that declares an output but resolved to none', () => {
        const outputs = [
            makeTaskDispatcherOutput('map', 'map_1', {outputDefined: true, variablePropertiesDefined: true}),
        ];

        const result = filterWorkflowNodeOutputs(outputs, componentDefinitions, taskDispatcherDefinitions);

        expect(result.outputs).toHaveLength(0);
        expect(result.definitions).toHaveLength(0);
    });

    it('should exclude outputs with no matching component definition', () => {
        const outputs = [makeActionOutput('unknownComponent', 'unknown_1')];

        const result = filterWorkflowNodeOutputs(outputs, componentDefinitions, taskDispatcherDefinitions);

        expect(result.outputs).toHaveLength(0);
        expect(result.definitions).toHaveLength(0);
    });

    it('should handle mixed output types and filter correctly', () => {
        const outputs = [
            makeActionOutput('httpClient', 'httpClient_1'),
            makeTaskDispatcherOutput('each', 'each_1', {variableOutputSchema: true, variablePropertiesDefined: true}),
            makeTaskDispatcherOutput('loop', 'loop_1', {variableOutputSchema: true, variablePropertiesDefined: true}),
            makeTaskDispatcherOutput('condition', 'condition_1'),
            makeTriggerOutput('webhook', 'webhook_trigger'),
        ];

        const result = filterWorkflowNodeOutputs(outputs, componentDefinitions, [
            ...taskDispatcherDefinitions,
            makeComponentDef('condition'),
        ]);

        expect(result.outputs).toHaveLength(4);
        expect(result.outputs.map((output) => output.workflowNodeName)).toEqual([
            'httpClient_1',
            'each_1',
            'loop_1',
            'webhook_trigger',
        ]);
    });

    it('should return empty results for empty inputs', () => {
        const result = filterWorkflowNodeOutputs([], componentDefinitions, taskDispatcherDefinitions);

        expect(result.outputs).toHaveLength(0);
        expect(result.definitions).toHaveLength(0);
    });

    it('should exclude outputs with no actionDefinition, triggerDefinition, or taskDispatcherDefinition', () => {
        const outputs: WorkflowNodeOutput[] = [{workflowNodeName: 'orphan_1'}];

        const result = filterWorkflowNodeOutputs(outputs, componentDefinitions, taskDispatcherDefinitions);

        expect(result.outputs).toHaveLength(0);
    });
});
