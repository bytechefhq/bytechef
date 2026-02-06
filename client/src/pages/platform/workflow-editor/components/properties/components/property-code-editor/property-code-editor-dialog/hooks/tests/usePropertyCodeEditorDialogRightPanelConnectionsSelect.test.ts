import {act, renderHook} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => {
    return {
        currentNode: undefined as {clusterElementType?: string; name: string} | undefined,
        mockComponentDefinition: {
            connection: {version: 1},
            name: 'testComponent',
            title: 'Test Component',
            version: 1,
        },
        mockComponentDefinitions: [
            {name: 'component1', title: 'Component 1', version: 1},
            {name: 'component2', title: 'Component 2', version: 1},
        ],
        mockConnections: [
            {id: 1, name: 'Connection 1'},
            {id: 2, name: 'Connection 2'},
        ],
        mockMutateClusterElement: vi.fn(),
        mockMutateWorkflowNode: vi.fn(),
        rootClusterElementNodeData: undefined as {workflowNodeName: string} | undefined,
    };
});

vi.mock('@/pages/platform/workflow-editor/providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => ({
        ConnectionKeys: {connections: ['connections']},
        useCreateConnectionMutation: vi.fn(),
        useGetComponentDefinitionsQuery: () => ({
            data: hoisted.mockComponentDefinitions,
        }),
        useGetConnectionTagsQuery: vi.fn(),
        useGetConnectionsQuery: () => ({
            data: hoisted.mockConnections,
        }),
    }),
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowEditorStore', () => ({
    default: (selector: (state: {rootClusterElementNodeData: typeof hoisted.rootClusterElementNodeData}) => unknown) =>
        selector({rootClusterElementNodeData: hoisted.rootClusterElementNodeData}),
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore', () => ({
    default: (selector: (state: {currentNode: typeof hoisted.currentNode}) => unknown) =>
        selector({currentNode: hoisted.currentNode}),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useSaveClusterElementTestConfigurationConnectionMutation: () => ({
        mutate: hoisted.mockMutateClusterElement,
    }),
    useSaveWorkflowTestConfigurationConnectionMutation: () => ({
        mutate: hoisted.mockMutateWorkflowNode,
    }),
}));

vi.mock('@/shared/queries/platform/componentDefinitions.queries', () => ({
    useGetComponentDefinitionQuery: () => ({
        data: hoisted.mockComponentDefinition,
    }),
}));

vi.mock('@/shared/queries/platform/workflowTestConfigurations.queries', () => ({
    WorkflowTestConfigurationKeys: {
        workflowTestConfigurations: ['workflowTestConfigurations'],
    },
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: () => 1,
}));

vi.mock('@tanstack/react-query', () => ({
    useQueryClient: () => ({
        invalidateQueries: vi.fn(),
    }),
}));

vi.mock('zustand/react/shallow', () => ({
    useShallow: (selector: unknown) => selector,
}));

import usePropertyCodeEditorDialogRightPanelConnectionsSelect from '../usePropertyCodeEditorDialogRightPanelConnectionsSelect';

describe('usePropertyCodeEditorDialogRightPanelConnectionsSelect', () => {
    const defaultProps = {
        componentConnection: {
            componentName: 'testComponent',
            componentVersion: 1,
            key: 'test-connection',
            required: true,
            workflowNodeName: 'testNode',
        },
        workflowId: 'workflow-1',
        workflowNodeName: 'testNode',
        workflowTestConfigurationConnection: undefined,
    };

    beforeEach(() => {
        vi.clearAllMocks();

        hoisted.currentNode = undefined;
        hoisted.rootClusterElementNodeData = undefined;
    });

    afterEach(() => {
        vi.clearAllMocks();
    });

    describe('initial state', () => {
        it('should return showNewConnectionDialog as false initially', () => {
            const {result} = renderHook(() => usePropertyCodeEditorDialogRightPanelConnectionsSelect(defaultProps));

            expect(result.current.showNewConnectionDialog).toBe(false);
        });

        it('should return component definition from query', () => {
            const {result} = renderHook(() => usePropertyCodeEditorDialogRightPanelConnectionsSelect(defaultProps));

            expect(result.current.componentDefinition).toEqual(hoisted.mockComponentDefinition);
        });

        it('should return component definitions from query', () => {
            const {result} = renderHook(() => usePropertyCodeEditorDialogRightPanelConnectionsSelect(defaultProps));

            expect(result.current.componentDefinitions).toEqual(hoisted.mockComponentDefinitions);
        });

        it('should return connections from query', () => {
            const {result} = renderHook(() => usePropertyCodeEditorDialogRightPanelConnectionsSelect(defaultProps));

            expect(result.current.connections).toEqual(hoisted.mockConnections);
        });

        it('should return connectionId as undefined when no test configuration connection', () => {
            const {result} = renderHook(() => usePropertyCodeEditorDialogRightPanelConnectionsSelect(defaultProps));

            expect(result.current.connectionId).toBeUndefined();
        });

        it('should return connectionId from test configuration connection', () => {
            const propsWithConnection = {
                ...defaultProps,
                workflowTestConfigurationConnection: {
                    connectionId: 123,
                    workflowConnectionKey: 'test-key',
                    workflowNodeName: 'testNode',
                },
            };

            const {result} = renderHook(() =>
                usePropertyCodeEditorDialogRightPanelConnectionsSelect(propsWithConnection)
            );

            expect(result.current.connectionId).toBe(123);
        });
    });

    describe('setShowNewConnectionDialog', () => {
        it('should update showNewConnectionDialog state to true', () => {
            const {result} = renderHook(() => usePropertyCodeEditorDialogRightPanelConnectionsSelect(defaultProps));

            act(() => {
                result.current.setShowNewConnectionDialog(true);
            });

            expect(result.current.showNewConnectionDialog).toBe(true);
        });

        it('should update showNewConnectionDialog state to false', () => {
            const {result} = renderHook(() => usePropertyCodeEditorDialogRightPanelConnectionsSelect(defaultProps));

            act(() => {
                result.current.setShowNewConnectionDialog(true);
            });

            act(() => {
                result.current.setShowNewConnectionDialog(false);
            });

            expect(result.current.showNewConnectionDialog).toBe(false);
        });
    });

    describe('handleValueChange', () => {
        it('should call workflow node mutation with correct parameters', () => {
            const {result} = renderHook(() => usePropertyCodeEditorDialogRightPanelConnectionsSelect(defaultProps));

            act(() => {
                result.current.handleValueChange(456, 'connection-key');
            });

            expect(hoisted.mockMutateWorkflowNode).toHaveBeenCalledWith(
                {
                    connectionId: 456,
                    environmentId: 1,
                    workflowConnectionKey: 'connection-key',
                    workflowId: 'workflow-1',
                    workflowNodeName: 'testNode',
                },
                expect.objectContaining({onSuccess: expect.any(Function)})
            );
        });

        it('should call cluster element mutation when in cluster element context', () => {
            hoisted.currentNode = {clusterElementType: 'model', name: 'script_1'};
            hoisted.rootClusterElementNodeData = {workflowNodeName: 'ai_agent_1'};

            const {result} = renderHook(() => usePropertyCodeEditorDialogRightPanelConnectionsSelect(defaultProps));

            act(() => {
                result.current.handleValueChange(789, 'connection-key');
            });

            expect(hoisted.mockMutateClusterElement).toHaveBeenCalledWith(
                {
                    clusterElementType: 'model',
                    clusterElementWorkflowNodeName: 'script_1',
                    connectionId: 789,
                    environmentId: 1,
                    workflowConnectionKey: 'connection-key',
                    workflowId: 'workflow-1',
                    workflowNodeName: 'ai_agent_1',
                },
                expect.objectContaining({onSuccess: expect.any(Function)})
            );

            expect(hoisted.mockMutateWorkflowNode).not.toHaveBeenCalled();
        });
    });

    describe('ConnectionKeys', () => {
        it('should return ConnectionKeys from provider', () => {
            const {result} = renderHook(() => usePropertyCodeEditorDialogRightPanelConnectionsSelect(defaultProps));

            expect(result.current.ConnectionKeys).toEqual({connections: ['connections']});
        });
    });
});
