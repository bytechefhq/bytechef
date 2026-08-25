import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {
    saveWorkflowDefinitionMock,
    setRootClusterElementNodeDataMock,
    useWorkflowDataStoreMock,
    useWorkflowEditorStoreMock,
} = vi.hoisted(() => ({
    saveWorkflowDefinitionMock: vi.fn(),
    setRootClusterElementNodeDataMock: vi.fn(),
    useWorkflowDataStoreMock: vi.fn(),
    useWorkflowEditorStoreMock: vi.fn(),
}));

vi.mock('@/pages/platform/workflow-editor/providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => ({updateWorkflowMutation: {mutate: vi.fn()}}),
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowDataStore', () => ({
    default: useWorkflowDataStoreMock,
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowEditorStore', () => ({
    default: useWorkflowEditorStoreMock,
}));

vi.mock('@/pages/platform/workflow-editor/utils/saveWorkflowDefinition', () => ({
    default: saveWorkflowDefinitionMock,
}));

import useAiAgentStreamResponse from './useAiAgentStreamResponse';

const rootClusterElementNodeData = {
    clusterElements: {model: [{name: 'anthropic_1', type: 'anthropic/v1/model'}], tools: []},
    componentName: 'aiAgent',
    name: 'aiAgent_1',
    operationName: 'chat',
    type: 'aiAgent/v1/chat',
    workflowNodeName: 'aiAgent_1',
};

// The live cluster elements, deliberately different from the seeded snapshot above: the definition is the
// only source that tracks tools added after the AI Agent editor was opened.
const definitionClusterElements = {
    model: {label: 'Claude', name: 'anthropic_1', parameters: {model: 'claude-opus-5'}, type: 'anthropic/v1/model'},
    tools: [
        {
            label: 'Sheets',
            name: 'googleSheets_1',
            parameters: {spreadsheetId: 'abc'},
            type: 'googleSheets/v1/insertRow',
        },
    ],
};

const buildDefinition = (operationName: string) =>
    JSON.stringify({
        tasks: [
            {
                clusterElements: definitionClusterElements,
                name: 'aiAgent_1',
                parameters: {
                    response: {responseFormat: 'JSON'},
                    systemPrompt: 'Be helpful',
                    userPrompt: 'Hello',
                },
                type: `aiAgent/v1/${operationName}`,
            },
        ],
    });

const mockStores = (operationName: string) => {
    useWorkflowEditorStoreMock.mockImplementation((selector: (state: unknown) => unknown) =>
        selector({
            rootClusterElementNodeData: {
                ...rootClusterElementNodeData,
                operationName,
                type: `aiAgent/v1/${operationName}`,
            },
            setRootClusterElementNodeData: setRootClusterElementNodeDataMock,
        })
    );

    useWorkflowDataStoreMock.mockImplementation((selector: (state: unknown) => unknown) =>
        selector({workflow: {definition: buildDefinition(operationName), id: 'workflow-1'}})
    );
};

describe('useAiAgentStreamResponse', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        mockStores('chat');
    });

    it('reports the chat action as not streaming', () => {
        const {result} = renderHook(() => useAiAgentStreamResponse());

        expect(result.current.isStreaming).toBe(false);
        expect(result.current.isStreamingSupported).toBe(true);
    });

    it('reports the streamChat action as streaming', () => {
        mockStores('streamChat');

        const {result} = renderHook(() => useAiAgentStreamResponse());

        expect(result.current.isStreaming).toBe(true);
    });

    it('hides the toggle for an action that is neither chat nor streamChat', () => {
        mockStores('realtimeChat');

        const {result} = renderHook(() => useAiAgentStreamResponse());

        expect(result.current.isStreamingSupported).toBe(false);
    });

    it('switches the root task to streamChat, keeping every parameter', () => {
        const {result} = renderHook(() => useAiAgentStreamResponse());

        result.current.updateStreaming(true);

        const [{nodeData}] = saveWorkflowDefinitionMock.mock.calls[0];

        expect(nodeData.type).toBe('aiAgent/v1/streamChat');
        expect(nodeData.operationName).toBe('streamChat');
        expect(nodeData.parameters).toEqual({
            response: {responseFormat: 'JSON'},
            systemPrompt: 'Be helpful',
            userPrompt: 'Hello',
        });
        expect(nodeData.clusterElements).toEqual(definitionClusterElements);
        expect(setRootClusterElementNodeDataMock).toHaveBeenCalledWith(nodeData);
    });

    it('keeps the cluster elements the definition holds, not the stale seeded snapshot', () => {
        const {result} = renderHook(() => useAiAgentStreamResponse());

        result.current.updateStreaming(true);

        const [{nodeData}] = saveWorkflowDefinitionMock.mock.calls[0];

        expect(nodeData.clusterElements).toEqual(definitionClusterElements);
        expect(nodeData.clusterElements).not.toEqual(rootClusterElementNodeData.clusterElements);
    });

    it('keeps the cluster elements when the seeded root node data carries none at all', () => {
        useWorkflowEditorStoreMock.mockImplementation((selector: (state: unknown) => unknown) =>
            selector({
                rootClusterElementNodeData: {
                    componentName: 'aiAgent',
                    name: 'aiAgent_1',
                    operationName: 'chat',
                    type: 'aiAgent/v1/chat',
                    workflowNodeName: 'aiAgent_1',
                },
                setRootClusterElementNodeData: setRootClusterElementNodeDataMock,
            })
        );

        const {result} = renderHook(() => useAiAgentStreamResponse());

        result.current.updateStreaming(true);

        const [{nodeData}] = saveWorkflowDefinitionMock.mock.calls[0];

        expect(nodeData.clusterElements).toEqual(definitionClusterElements);
    });

    it('falls back to the node data operation when the workflow has no definition', () => {
        useWorkflowDataStoreMock.mockImplementation((selector: (state: unknown) => unknown) =>
            selector({workflow: {id: 'workflow-1'}})
        );

        const {result} = renderHook(() => useAiAgentStreamResponse());

        expect(result.current.isStreamingSupported).toBe(true);
        expect(result.current.isStreaming).toBe(false);

        result.current.updateStreaming(true);

        expect(saveWorkflowDefinitionMock).not.toHaveBeenCalled();
    });

    it('does not save when the definition cannot be parsed, so the prompts survive', () => {
        useWorkflowDataStoreMock.mockImplementation((selector: (state: unknown) => unknown) =>
            selector({workflow: {definition: '{not json', id: 'workflow-1'}})
        );

        const {result} = renderHook(() => useAiAgentStreamResponse());

        expect(result.current.isStreamingSupported).toBe(true);

        result.current.updateStreaming(true);

        expect(saveWorkflowDefinitionMock).not.toHaveBeenCalled();
        expect(setRootClusterElementNodeDataMock).not.toHaveBeenCalled();
    });

    it('does not save when the root node data carries no component name', () => {
        useWorkflowEditorStoreMock.mockImplementation((selector: (state: unknown) => unknown) =>
            selector({
                rootClusterElementNodeData: {
                    ...rootClusterElementNodeData,
                    componentName: undefined,
                },
                setRootClusterElementNodeData: setRootClusterElementNodeDataMock,
            })
        );

        const {result} = renderHook(() => useAiAgentStreamResponse());

        result.current.updateStreaming(true);

        expect(saveWorkflowDefinitionMock).not.toHaveBeenCalled();
        expect(setRootClusterElementNodeDataMock).not.toHaveBeenCalled();
    });

    it('switches the root task back to chat', () => {
        mockStores('streamChat');

        const {result} = renderHook(() => useAiAgentStreamResponse());

        result.current.updateStreaming(false);

        const [{nodeData}] = saveWorkflowDefinitionMock.mock.calls[0];

        expect(nodeData.type).toBe('aiAgent/v1/chat');
        expect(nodeData.operationName).toBe('chat');
        expect(nodeData.parameters.response).toEqual({responseFormat: 'JSON'});
    });
});
