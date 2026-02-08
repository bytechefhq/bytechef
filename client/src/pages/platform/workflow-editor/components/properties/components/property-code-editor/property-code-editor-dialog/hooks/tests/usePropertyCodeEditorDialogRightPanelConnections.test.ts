import {act, renderHook} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => {
    return {
        mockInvalidateQueries: vi.fn(),
        mockMutate: vi.fn(),
        mockSetShowConnectionNote: vi.fn(),
        storeState: {
            componentDefinitions: [{name: 'slack', title: 'Slack', version: 1}],
            currentEnvironmentId: 1,
            currentNode: {clusterElementType: undefined as string | undefined, name: 'testNode'},
            rootClusterElementNodeData: undefined as {workflowNodeName: string} | undefined,
            showConnectionNote: true,
            workflowTestConfigurationConnections: [{connectionId: 1, workflowConnectionKey: 'slack_1'}],
        },
    };
});

vi.mock('@/pages/platform/workflow-editor/stores/useConnectionNoteStore', () => ({
    useConnectionNoteStore: (selector: (state: unknown) => unknown) =>
        selector({
            setShowConnectionNote: hoisted.mockSetShowConnectionNote,
            showConnectionNote: hoisted.storeState.showConnectionNote,
        }),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: unknown) => unknown) =>
        selector({
            currentEnvironmentId: hoisted.storeState.currentEnvironmentId,
        }),
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore', () => ({
    default: (selector: (state: unknown) => unknown) => selector({currentNode: hoisted.storeState.currentNode}),
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowEditorStore', () => ({
    default: () => hoisted.storeState.rootClusterElementNodeData,
}));

vi.mock('@tanstack/react-query', () => ({
    useQueryClient: () => ({
        invalidateQueries: hoisted.mockInvalidateQueries,
    }),
}));

vi.mock('@/pages/platform/workflow-editor/providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => ({
        ConnectionKeys: {
            connectionTags: ['connectionTags'],
            connections: ['connections'],
        },
        updateWorkflowMutation: {
            mutate: hoisted.mockMutate,
        },
        useCreateConnectionMutation: vi.fn(),
        useGetComponentDefinitionsQuery: () => ({
            data: hoisted.storeState.componentDefinitions,
        }),
        useGetConnectionTagsQuery: vi.fn(),
    }),
}));

vi.mock('@/shared/queries/platform/workflowTestConfigurations.queries', () => ({
    useGetWorkflowTestConfigurationConnectionsQuery: () => ({
        data: hoisted.storeState.workflowTestConfigurationConnections,
    }),
}));

describe('usePropertyCodeEditorDialogRightPanelConnections', () => {
    const defaultProps = {
        componentConnections: [
            {componentName: 'slack', componentVersion: 1, key: 'slack_1', required: true, workflowNodeName: 'testNode'},
        ],
        workflow: {
            definition: JSON.stringify({
                tasks: [
                    {
                        connections: {slack_1: {componentName: 'slack', componentVersion: 1}},
                        name: 'testNode',
                    },
                ],
            }),
            id: 'workflow-1',
            version: 1,
        },
        workflowNodeName: 'testNode',
    };

    beforeEach(async () => {
        hoisted.storeState.showConnectionNote = true;
        hoisted.storeState.currentEnvironmentId = 1;
        hoisted.storeState.componentDefinitions = [{name: 'slack', title: 'Slack', version: 1}];
        hoisted.storeState.workflowTestConfigurationConnections = [{connectionId: 1, workflowConnectionKey: 'slack_1'}];
        hoisted.storeState.currentNode = {clusterElementType: undefined, name: 'testNode'};
        hoisted.storeState.rootClusterElementNodeData = undefined;
        vi.clearAllMocks();
        vi.resetModules();
    });

    afterEach(() => {
        vi.clearAllMocks();
    });

    describe('initial state', () => {
        it('should return showConnectionNote from store', async () => {
            const {usePropertyCodeEditorDialogRightPanelConnections} =
                await import('../usePropertyCodeEditorDialogRightPanelConnections');
            const {result} = renderHook(() => usePropertyCodeEditorDialogRightPanelConnections(defaultProps));

            expect(result.current.showConnectionNote).toBe(true);
        });

        it('should return componentDefinitions', async () => {
            const {usePropertyCodeEditorDialogRightPanelConnections} =
                await import('../usePropertyCodeEditorDialogRightPanelConnections');
            const {result} = renderHook(() => usePropertyCodeEditorDialogRightPanelConnections(defaultProps));

            expect(result.current.componentDefinitions).toEqual([{name: 'slack', title: 'Slack', version: 1}]);
        });

        it('should return workflowTestConfigurationConnections', async () => {
            const {usePropertyCodeEditorDialogRightPanelConnections} =
                await import('../usePropertyCodeEditorDialogRightPanelConnections');
            const {result} = renderHook(() => usePropertyCodeEditorDialogRightPanelConnections(defaultProps));

            expect(result.current.workflowTestConfigurationConnections).toEqual([
                {connectionId: 1, workflowConnectionKey: 'slack_1'},
            ]);
        });

        it('should return showNewConnectionDialog as false initially', async () => {
            const {usePropertyCodeEditorDialogRightPanelConnections} =
                await import('../usePropertyCodeEditorDialogRightPanelConnections');
            const {result} = renderHook(() => usePropertyCodeEditorDialogRightPanelConnections(defaultProps));

            expect(result.current.showNewConnectionDialog).toBe(false);
        });
    });

    describe('handleCloseConnectionNote', () => {
        it('should call setShowConnectionNote with false', async () => {
            const {usePropertyCodeEditorDialogRightPanelConnections} =
                await import('../usePropertyCodeEditorDialogRightPanelConnections');
            const {result} = renderHook(() => usePropertyCodeEditorDialogRightPanelConnections(defaultProps));

            act(() => {
                result.current.handleCloseConnectionNote();
            });

            expect(hoisted.mockSetShowConnectionNote).toHaveBeenCalledWith(false);
        });
    });

    describe('setShowNewConnectionDialog', () => {
        it('should update showNewConnectionDialog state', async () => {
            const {usePropertyCodeEditorDialogRightPanelConnections} =
                await import('../usePropertyCodeEditorDialogRightPanelConnections');
            const {result} = renderHook(() => usePropertyCodeEditorDialogRightPanelConnections(defaultProps));

            act(() => {
                result.current.setShowNewConnectionDialog(true);
            });

            expect(result.current.showNewConnectionDialog).toBe(true);
        });
    });

    describe('handleOnSubmit', () => {
        it('should call updateWorkflowMutation.mutate with updated definition', async () => {
            const {usePropertyCodeEditorDialogRightPanelConnections} =
                await import('../usePropertyCodeEditorDialogRightPanelConnections');
            const {result} = renderHook(() => usePropertyCodeEditorDialogRightPanelConnections(defaultProps));

            act(() => {
                result.current.handleOnSubmit({
                    componentName: 'github',
                    componentVersion: 2,
                    name: 'github_connection',
                });
            });

            expect(hoisted.mockMutate).toHaveBeenCalled();

            const callArg = hoisted.mockMutate.mock.calls[0][0];

            expect(callArg.id).toBe('workflow-1');

            const definition = JSON.parse(callArg.workflow.definition);

            expect(definition.tasks[0].connections.github_connection).toEqual({
                componentName: 'github',
                componentVersion: 2,
            });
        });

        it('should not call mutate when workflow definition is missing', async () => {
            const {usePropertyCodeEditorDialogRightPanelConnections} =
                await import('../usePropertyCodeEditorDialogRightPanelConnections');
            const {result} = renderHook(() =>
                usePropertyCodeEditorDialogRightPanelConnections({
                    ...defaultProps,
                    workflow: {...defaultProps.workflow, definition: undefined},
                })
            );

            act(() => {
                result.current.handleOnSubmit({
                    componentName: 'github',
                    componentVersion: 2,
                    name: 'github_connection',
                });
            });

            expect(hoisted.mockMutate).not.toHaveBeenCalled();
        });
    });

    describe('handleOnRemoveClick', () => {
        it('should call updateWorkflowMutation.mutate with connection removed', async () => {
            const {usePropertyCodeEditorDialogRightPanelConnections} =
                await import('../usePropertyCodeEditorDialogRightPanelConnections');
            const {result} = renderHook(() => usePropertyCodeEditorDialogRightPanelConnections(defaultProps));

            act(() => {
                result.current.handleOnRemoveClick('slack_1');
            });

            expect(hoisted.mockMutate).toHaveBeenCalled();

            const callArg = hoisted.mockMutate.mock.calls[0][0];

            expect(callArg.id).toBe('workflow-1');

            const definition = JSON.parse(callArg.workflow.definition);

            expect(definition.tasks[0].connections.slack_1).toBeUndefined();
        });

        it('should not call mutate when workflow definition is missing', async () => {
            const {usePropertyCodeEditorDialogRightPanelConnections} =
                await import('../usePropertyCodeEditorDialogRightPanelConnections');
            const {result} = renderHook(() =>
                usePropertyCodeEditorDialogRightPanelConnections({
                    ...defaultProps,
                    workflow: {...defaultProps.workflow, definition: undefined},
                })
            );

            act(() => {
                result.current.handleOnRemoveClick('slack_1');
            });

            expect(hoisted.mockMutate).not.toHaveBeenCalled();
        });
    });

    describe('cluster element connections', () => {
        const clusterElementProps = {
            componentConnections: [
                {
                    componentName: 'amazonS3',
                    componentVersion: 1,
                    key: 's3_connection',
                    required: true,
                    workflowNodeName: 'source_1',
                },
            ],
            workflow: {
                definition: JSON.stringify({
                    tasks: [
                        {
                            clusterElements: {
                                source: {
                                    connections: {s3_connection: {componentName: 'amazonS3', componentVersion: 1}},
                                    name: 'source_1',
                                    type: 'amazonS3/v1/amazonS3Read',
                                },
                            },
                            name: 'dataStream_1',
                            type: 'dataStream/v1/sync',
                        },
                    ],
                }),
                id: 'workflow-1',
                version: 1,
            },
            workflowNodeName: 'source_1',
        };

        beforeEach(async () => {
            hoisted.storeState.currentNode = {clusterElementType: 'source', name: 'source_1'};
            hoisted.storeState.rootClusterElementNodeData = {workflowNodeName: 'dataStream_1'};
            vi.clearAllMocks();
            vi.resetModules();
        });

        describe('handleOnSubmit for cluster elements', () => {
            it('should add connection to cluster element', async () => {
                const {usePropertyCodeEditorDialogRightPanelConnections} =
                    await import('../usePropertyCodeEditorDialogRightPanelConnections');
                const {result} = renderHook(() =>
                    usePropertyCodeEditorDialogRightPanelConnections(clusterElementProps)
                );

                act(() => {
                    result.current.handleOnSubmit({
                        componentName: 'googleDrive',
                        componentVersion: 1,
                        name: 'gdrive_connection',
                    });
                });

                expect(hoisted.mockMutate).toHaveBeenCalled();

                const callArg = hoisted.mockMutate.mock.calls[0][0];
                const definition = JSON.parse(callArg.workflow.definition);

                expect(definition.tasks[0].clusterElements.source.connections.gdrive_connection).toEqual({
                    componentName: 'googleDrive',
                    componentVersion: 1,
                });
            });

            it('should not overwrite existing cluster element connections', async () => {
                const {usePropertyCodeEditorDialogRightPanelConnections} =
                    await import('../usePropertyCodeEditorDialogRightPanelConnections');
                const {result} = renderHook(() =>
                    usePropertyCodeEditorDialogRightPanelConnections(clusterElementProps)
                );

                act(() => {
                    result.current.handleOnSubmit({
                        componentName: 'googleDrive',
                        componentVersion: 1,
                        name: 'gdrive_connection',
                    });
                });

                const callArg = hoisted.mockMutate.mock.calls[0][0];
                const definition = JSON.parse(callArg.workflow.definition);

                expect(definition.tasks[0].clusterElements.source.connections.s3_connection).toBeDefined();
            });
        });

        describe('handleOnRemoveClick for cluster elements', () => {
            it('should remove connection from cluster element', async () => {
                const {usePropertyCodeEditorDialogRightPanelConnections} =
                    await import('../usePropertyCodeEditorDialogRightPanelConnections');
                const {result} = renderHook(() =>
                    usePropertyCodeEditorDialogRightPanelConnections(clusterElementProps)
                );

                act(() => {
                    result.current.handleOnRemoveClick('s3_connection');
                });

                expect(hoisted.mockMutate).toHaveBeenCalled();

                const callArg = hoisted.mockMutate.mock.calls[0][0];
                const definition = JSON.parse(callArg.workflow.definition);

                expect(definition.tasks[0].clusterElements.source.connections.s3_connection).toBeUndefined();
            });
        });
    });

    describe('query invalidation', () => {
        it('should invalidate workflowNodeComponentConnections on submit success for regular nodes', async () => {
            const {usePropertyCodeEditorDialogRightPanelConnections} =
                await import('../usePropertyCodeEditorDialogRightPanelConnections');
            const {result} = renderHook(() => usePropertyCodeEditorDialogRightPanelConnections(defaultProps));

            act(() => {
                result.current.handleOnSubmit({
                    componentName: 'github',
                    componentVersion: 2,
                    name: 'github_connection',
                });
            });

            const mutateOptions = hoisted.mockMutate.mock.calls[0][1];

            act(() => {
                mutateOptions.onSuccess();
            });

            expect(hoisted.mockInvalidateQueries).toHaveBeenCalledWith({
                queryKey: ['workflowNodeComponentConnections'],
            });
        });

        it('should invalidate clusterElementComponentConnections on submit success for cluster elements', async () => {
            hoisted.storeState.currentNode = {clusterElementType: 'source', name: 'source_1'};
            hoisted.storeState.rootClusterElementNodeData = {workflowNodeName: 'dataStream_1'};
            vi.resetModules();

            const clusterElementProps = {
                componentConnections: [],
                workflow: {
                    definition: JSON.stringify({
                        tasks: [
                            {
                                clusterElements: {
                                    source: {
                                        connections: {},
                                        name: 'source_1',
                                        type: 'amazonS3/v1/amazonS3Read',
                                    },
                                },
                                name: 'dataStream_1',
                                type: 'dataStream/v1/sync',
                            },
                        ],
                    }),
                    id: 'workflow-1',
                    version: 1,
                },
                workflowNodeName: 'source_1',
            };

            const {usePropertyCodeEditorDialogRightPanelConnections} =
                await import('../usePropertyCodeEditorDialogRightPanelConnections');
            const {result} = renderHook(() => usePropertyCodeEditorDialogRightPanelConnections(clusterElementProps));

            act(() => {
                result.current.handleOnSubmit({
                    componentName: 'amazonS3',
                    componentVersion: 1,
                    name: 's3_connection',
                });
            });

            const mutateOptions = hoisted.mockMutate.mock.calls[0][1];

            act(() => {
                mutateOptions.onSuccess();
            });

            expect(hoisted.mockInvalidateQueries).toHaveBeenCalledWith({
                queryKey: ['clusterElementComponentConnections'],
            });
        });

        it('should invalidate workflowNodeComponentConnections on remove success for regular nodes', async () => {
            const {usePropertyCodeEditorDialogRightPanelConnections} =
                await import('../usePropertyCodeEditorDialogRightPanelConnections');
            const {result} = renderHook(() => usePropertyCodeEditorDialogRightPanelConnections(defaultProps));

            act(() => {
                result.current.handleOnRemoveClick('slack_1');
            });

            const mutateOptions = hoisted.mockMutate.mock.calls[0][1];

            act(() => {
                mutateOptions.onSuccess();
            });

            expect(hoisted.mockInvalidateQueries).toHaveBeenCalledWith({
                queryKey: ['workflowNodeComponentConnections'],
            });
        });
    });
});
