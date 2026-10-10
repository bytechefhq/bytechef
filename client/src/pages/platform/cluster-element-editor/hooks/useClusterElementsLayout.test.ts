import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {act, renderHook, waitFor} from '@testing-library/react';
import {ReactNode, createElement} from 'react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import useClusterElementsDataStore from '../stores/useClusterElementsDataStore';

const {componentDefinitionsByName} = vi.hoisted(() => {
    const aiAgentDefinition = {
        actionClusterElementTypes: {},
        clusterElementClusterElementTypes: {},
        clusterElementTypes: [
            {key: 'model', label: 'Model', multipleElements: false, name: 'MODEL'},
            {key: 'chatMemory', label: 'Memory', multipleElements: false, name: 'CHAT_MEMORY'},
        ],
        name: 'aiAgent',
        version: 1,
    };

    return {
        componentDefinitionsByName: {
            aiAgent: aiAgentDefinition,
            awsChatMemory: {
                clusterElementTypes: [{key: 'model', label: 'Model', multipleElements: false, name: 'MODEL'}],
                name: 'awsChatMemory',
                version: 2,
            },
            jdbcChatMemory: {
                clusterElementTypes: [
                    {key: 'dataSource', label: 'Data Source', multipleElements: false, name: 'DATA_SOURCE'},
                    {key: 'model', label: 'Model', multipleElements: false, name: 'MODEL'},
                ],
                name: 'jdbcChatMemory',
                version: 2,
            },
        } as Record<string, object>,
    };
});

vi.mock('@/shared/queries/platform/componentDefinitions.queries', () => ({
    ComponentDefinitionKeys: {
        componentDefinition: (request: {componentName: string; componentVersion: number}) => [
            'componentDefinition',
            request.componentName,
            request.componentVersion,
        ],
    },
    useGetComponentDefinitionQuery: () => ({data: componentDefinitionsByName.aiAgent}),
}));

vi.mock('@/shared/middleware/platform/configuration', () => ({
    ComponentDefinitionApi: class {
        getComponentDefinition({componentName}: {componentName: string}) {
            return Promise.resolve(componentDefinitionsByName[componentName]);
        }
    },
}));

import useClusterElementsLayout from './useClusterElementsLayout';

const getWorkflowDefinition = (chatMemoryType: string, chatMemoryName: string) =>
    JSON.stringify({
        tasks: [
            {
                clusterElements: {
                    chatMemory: {clusterElements: {}, label: 'Memory', name: chatMemoryName, type: chatMemoryType},
                },
                label: 'AI Agent',
                name: 'aiAgent_1',
                type: 'aiAgent/v1/chat',
            },
        ],
    });

const getNodeIds = () => useClusterElementsDataStore.getState().nodes.map((node) => node.id);

describe('useClusterElementsLayout', () => {
    let pendingAnimationFrameCallbacks: Array<FrameRequestCallback> = [];

    const flushAnimationFrames = (time: number) => {
        const callbacks = pendingAnimationFrameCallbacks;

        pendingAnimationFrameCallbacks = [];

        callbacks.forEach((callback) => callback(time));
    };

    beforeEach(() => {
        pendingAnimationFrameCallbacks = [];

        vi.spyOn(window, 'requestAnimationFrame').mockImplementation((callback) => {
            pendingAnimationFrameCallbacks.push(callback);

            return pendingAnimationFrameCallbacks.length;
        });
        vi.spyOn(window, 'cancelAnimationFrame').mockImplementation(() => {
            pendingAnimationFrameCallbacks = [];
        });

        useClusterElementsDataStore.setState({
            edges: [],
            isNodeDragging: false,
            isPositionSaving: false,
            nodes: [],
        });

        useWorkflowDataStore.setState({
            workflow: {definition: getWorkflowDefinition('awsChatMemory/v2/chatMemory', 'awsChatMemory_1'), id: 'w1'},
        } as never);

        useWorkflowEditorStore.setState({
            mainClusterRootComponentDefinition: undefined,
            nestedClusterRootsComponentDefinitions: {},
            rootClusterElementNodeData: {
                clusterRoot: true,
                componentName: 'aiAgent',
                label: 'AI Agent',
                name: 'aiAgent_1',
                operationName: 'chat',
                type: 'aiAgent/v1/chat',
                workflowNodeName: 'aiAgent_1',
            },
        } as never);

        useWorkflowNodeDetailsPanelStore.setState({workflowNodeDetailsPanelOpen: true} as never);
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('keeps the nested root placeholders laid out while the panel-close animation is running', async () => {
        const queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});

        const wrapper = ({children}: {children: ReactNode}) =>
            createElement(QueryClientProvider, {client: queryClient}, children);

        renderHook(() => useClusterElementsLayout(), {wrapper});

        await waitFor(() => expect(getNodeIds()).toContain('awsChatMemory_1-model-placeholder-0'));

        // Replacing the selected memory closes the details panel, which starts the canvas-width animation...
        act(() => {
            useWorkflowNodeDetailsPanelStore.setState({workflowNodeDetailsPanelOpen: false} as never);
        });

        expect(pendingAnimationFrameCallbacks.length).toBeGreaterThan(0);

        // ...and the saved workflow then swaps the memory component while the animation is still in flight.
        act(() => {
            useWorkflowDataStore.setState({
                workflow: {
                    definition: getWorkflowDefinition('jdbcChatMemory/v2/chatMemory', 'jdbcChatMemory_1'),
                    id: 'w1',
                },
            } as never);
        });

        await waitFor(() =>
            expect(useWorkflowEditorStore.getState().nestedClusterRootsComponentDefinitions).toHaveProperty(
                'jdbcChatMemory'
            )
        );

        await waitFor(() => expect(getNodeIds()).toContain('jdbcChatMemory_1-dataSource-placeholder-0'));

        act(() => {
            flushAnimationFrames(0);
            flushAnimationFrames(10_000);
        });

        expect(getNodeIds()).toEqual(
            expect.arrayContaining([
                'jdbcChatMemory_1',
                'jdbcChatMemory_1-dataSource-placeholder-0',
                'jdbcChatMemory_1-model-placeholder-0',
            ])
        );
        expect(getNodeIds()).not.toContain('awsChatMemory_1');
    });
});
