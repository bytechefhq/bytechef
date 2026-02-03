import {act, renderHook} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => {
    return {
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
        mockMutate: vi.fn(),
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

vi.mock('@/shared/queries/platform/componentDefinitions.queries', () => ({
    useGetComponentDefinitionQuery: () => ({
        data: hoisted.mockComponentDefinition,
    }),
}));

vi.mock('@/shared/mutations/platform/workflowTestConfigurations.mutations', () => ({
    useSaveWorkflowTestConfigurationConnectionMutation: () => ({
        mutate: hoisted.mockMutate,
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
        it('should call mutation with correct parameters', () => {
            const {result} = renderHook(() => usePropertyCodeEditorDialogRightPanelConnectionsSelect(defaultProps));

            act(() => {
                result.current.handleValueChange(456, 'connection-key');
            });

            expect(hoisted.mockMutate).toHaveBeenCalledWith({
                environmentId: 1,
                saveWorkflowTestConfigurationConnectionRequest: {
                    connectionId: 456,
                },
                workflowConnectionKey: 'connection-key',
                workflowId: 'workflow-1',
                workflowNodeName: 'testNode',
            });
        });
    });

    describe('ConnectionKeys', () => {
        it('should return ConnectionKeys from provider', () => {
            const {result} = renderHook(() => usePropertyCodeEditorDialogRightPanelConnectionsSelect(defaultProps));

            expect(result.current.ConnectionKeys).toEqual({connections: ['connections']});
        });
    });
});
