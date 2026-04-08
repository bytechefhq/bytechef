import {SPACE} from '@/shared/constants';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {NodeDataType} from '@/shared/types';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import clearAllNodePositions from './clearAllNodePositions';
import removeWorkflowNodePosition from './removeWorkflowNodePosition';
import saveWorkflowNodesPosition from './saveWorkflowNodesPosition';
import {clearAllWorkflowMutations, setWorkflowMutating} from './workflowMutationGuard';

function makeTask(name: string, nodePosition?: {x: number; y: number}): WorkflowTask {
    return {
        metadata: nodePosition ? {ui: {nodePosition}} : undefined,
        name,
        type: `test/${name}`,
    } as WorkflowTask;
}

function makeDefinition(
    tasks: WorkflowTask[],
    triggers?: Array<{metadata?: {ui?: {nodePosition?: {x: number; y: number}}}; name: string; type: string}>
): string {
    return JSON.stringify(
        {
            tasks,
            triggers: triggers || [{name: 'trigger_1', type: 'test/trigger'}],
        },
        null,
        SPACE
    );
}

function createMockMutation() {
    return {
        isPending: false,
        mutate: vi.fn(),
    };
}

describe('store definition sync', () => {
    beforeEach(() => {
        clearAllWorkflowMutations();

        useWorkflowDataStore.setState({
            edges: [],
            isWorkflowLoaded: true,
            nodes: [],
            workflow: {
                nodeNames: ['trigger_1'],
            },
        });
    });

    describe('saveWorkflowNodesPosition', () => {
        it('should update store definition immediately with saved positions', () => {
            const tasks = [makeTask('task_1')];
            const definition = makeDefinition(tasks);

            useWorkflowDataStore.setState({
                workflow: {
                    ...useWorkflowDataStore.getState().workflow,
                    definition,
                    id: '1',
                    version: 1,
                },
            });

            const mockMutation = createMockMutation();

            saveWorkflowNodesPosition({
                draggedNodeId: 'task_1',
                nodePositions: {task_1: {x: 200, y: 300}},
                updateWorkflowMutation: mockMutation as never,
            });

            // Verify store definition was updated before mutation
            const storeDefinition = useWorkflowDataStore.getState().workflow.definition!;
            const parsed = JSON.parse(storeDefinition);

            expect(parsed.tasks[0].metadata.ui.nodePosition).toEqual({x: 200, y: 300});
        });

        it('should update store definition for nested task positions', () => {
            const conditionTask: WorkflowTask = {
                ...makeTask('condition_1'),
                parameters: {
                    caseTrue: [makeTask('child_1')],
                },
            };
            const definition = makeDefinition([conditionTask]);

            useWorkflowDataStore.setState({
                workflow: {
                    ...useWorkflowDataStore.getState().workflow,
                    definition,
                    id: '1',
                    version: 1,
                },
            });

            const mockMutation = createMockMutation();

            saveWorkflowNodesPosition({
                draggedNodeId: 'condition_1',
                nodePositions: {
                    child_1: {x: 350, y: 450},
                    condition_1: {x: 100, y: 200},
                },
                updateWorkflowMutation: mockMutation as never,
            });

            const storeDefinition = useWorkflowDataStore.getState().workflow.definition!;
            const parsed = JSON.parse(storeDefinition);

            expect(parsed.tasks[0].metadata.ui.nodePosition).toEqual({x: 100, y: 200});
            expect(parsed.tasks[0].parameters.caseTrue[0].metadata.ui.nodePosition).toEqual({x: 350, y: 450});
        });

        it('should send the same definition to the mutation as stored in state', () => {
            const tasks = [makeTask('task_1')];
            const definition = makeDefinition(tasks);

            useWorkflowDataStore.setState({
                workflow: {
                    ...useWorkflowDataStore.getState().workflow,
                    definition,
                    id: '1',
                    version: 1,
                },
            });

            const mockMutation = createMockMutation();

            saveWorkflowNodesPosition({
                draggedNodeId: 'task_1',
                nodePositions: {task_1: {x: 200, y: 300}},
                updateWorkflowMutation: mockMutation as never,
            });

            const storeDefinition = useWorkflowDataStore.getState().workflow.definition!;

            expect(mockMutation.mutate).toHaveBeenCalledWith(
                expect.objectContaining({
                    workflow: expect.objectContaining({
                        definition: storeDefinition,
                    }),
                }),
                expect.anything()
            );
        });
    });

    describe('removeWorkflowNodePosition', () => {
        it('should update store definition immediately with cleared position', () => {
            const tasks = [makeTask('task_1', {x: 100, y: 200}), makeTask('task_2', {x: 300, y: 400})];
            const definition = makeDefinition(tasks);

            useWorkflowDataStore.setState({
                workflow: {
                    ...useWorkflowDataStore.getState().workflow,
                    definition,
                    id: '1',
                    version: 1,
                },
            });

            const mockMutation = createMockMutation();

            removeWorkflowNodePosition({
                incrementLayoutResetCounter: vi.fn(),
                invalidateWorkflowQueries: vi.fn(),
                nodeName: 'task_1',
                updateWorkflowMutation: mockMutation as never,
            });

            const storeDefinition = useWorkflowDataStore.getState().workflow.definition!;
            const parsed = JSON.parse(storeDefinition);

            // task_1 position should be cleared
            expect(parsed.tasks[0].metadata?.ui?.nodePosition).toBeUndefined();
            // task_2 position should be preserved
            expect(parsed.tasks[1].metadata.ui.nodePosition).toEqual({x: 300, y: 400});
        });

        it('should preserve sibling positions when clearing nested task position', () => {
            const conditionTask: WorkflowTask = {
                ...makeTask('condition_1', {x: 50, y: 50}),
                parameters: {
                    caseTrue: [makeTask('each_1', {x: 100, y: 200})],
                },
            };
            const definition = makeDefinition([conditionTask]);

            useWorkflowDataStore.setState({
                workflow: {
                    ...useWorkflowDataStore.getState().workflow,
                    definition,
                    id: '1',
                    version: 1,
                },
            });

            const mockMutation = createMockMutation();

            removeWorkflowNodePosition({
                incrementLayoutResetCounter: vi.fn(),
                invalidateWorkflowQueries: vi.fn(),
                nodeName: 'each_1',
                updateWorkflowMutation: mockMutation as never,
            });

            const storeDefinition = useWorkflowDataStore.getState().workflow.definition!;
            const parsed = JSON.parse(storeDefinition);

            // condition_1 position should be preserved
            expect(parsed.tasks[0].metadata.ui.nodePosition).toEqual({x: 50, y: 50});
            // each_1 position should be cleared
            expect(parsed.tasks[0].parameters.caseTrue[0].metadata?.ui?.nodePosition).toBeUndefined();
        });
    });

    describe('clearAllNodePositions', () => {
        it('should update store definition immediately with all positions cleared', () => {
            const tasks = [makeTask('task_1', {x: 100, y: 200}), makeTask('task_2', {x: 300, y: 400})];
            const triggers = [
                {
                    metadata: {ui: {nodePosition: {x: 50, y: 50}}},
                    name: 'trigger_1',
                    type: 'test/trigger',
                },
            ];
            const definition = makeDefinition(tasks, triggers);

            useWorkflowDataStore.setState({
                workflow: {
                    ...useWorkflowDataStore.getState().workflow,
                    definition,
                    id: '1',
                    version: 1,
                },
            });

            const mockMutation = createMockMutation();

            clearAllNodePositions({
                incrementLayoutResetCounter: vi.fn(),
                invalidateWorkflowQueries: vi.fn(),
                updateWorkflowMutation: mockMutation as never,
            });

            const storeDefinition = useWorkflowDataStore.getState().workflow.definition!;
            const parsed = JSON.parse(storeDefinition);

            expect(parsed.triggers[0].metadata?.ui?.nodePosition).toBeUndefined();
            expect(parsed.tasks[0].metadata?.ui?.nodePosition).toBeUndefined();
            expect(parsed.tasks[1].metadata?.ui?.nodePosition).toBeUndefined();
        });

        it('should clear nested dispatcher positions in store definition', () => {
            const conditionTask: WorkflowTask = {
                ...makeTask('condition_1', {x: 50, y: 50}),
                parameters: {
                    caseTrue: [makeTask('child_1', {x: 100, y: 200})],
                },
            };
            const definition = makeDefinition([conditionTask]);

            useWorkflowDataStore.setState({
                workflow: {
                    ...useWorkflowDataStore.getState().workflow,
                    definition,
                    id: '1',
                    version: 1,
                },
            });

            const mockMutation = createMockMutation();

            clearAllNodePositions({
                incrementLayoutResetCounter: vi.fn(),
                invalidateWorkflowQueries: vi.fn(),
                updateWorkflowMutation: mockMutation as never,
            });

            const storeDefinition = useWorkflowDataStore.getState().workflow.definition!;
            const parsed = JSON.parse(storeDefinition);

            expect(parsed.tasks[0].metadata?.ui?.nodePosition).toBeUndefined();
            expect(parsed.tasks[0].parameters.caseTrue[0].metadata?.ui?.nodePosition).toBeUndefined();
        });
    });

    describe('clearPositionNodeIds during save', () => {
        it('should clear positions for specified nodes while saving dragged node position', () => {
            const tasks = [
                makeTask('task_1', {x: 100, y: 200}),
                makeTask('task_2', {x: 300, y: 400}),
                makeTask('task_3', {x: 500, y: 600}),
            ];
            const definition = makeDefinition(tasks);

            useWorkflowDataStore.setState({
                workflow: {
                    ...useWorkflowDataStore.getState().workflow,
                    definition,
                    id: '1',
                    version: 1,
                },
            });

            const mockMutation = createMockMutation();

            saveWorkflowNodesPosition({
                clearPositionNodeIds: new Set(['task_2']),
                draggedNodeId: 'task_1',
                nodePositions: {task_1: {x: 150, y: 250}},
                updateWorkflowMutation: mockMutation as never,
            });

            const storeDefinition = useWorkflowDataStore.getState().workflow.definition!;
            const parsed = JSON.parse(storeDefinition);

            // task_1 should have new position
            expect(parsed.tasks[0].metadata.ui.nodePosition).toEqual({x: 150, y: 250});
            // task_2 should have position cleared
            expect(parsed.tasks[1].metadata?.ui?.nodePosition).toBeUndefined();
            // task_3 should retain original position
            expect(parsed.tasks[2].metadata.ui.nodePosition).toEqual({x: 500, y: 600});
        });
    });

    describe('each single-object iteratee handling', () => {
        it('should update positions for each dispatcher single-object iteratee child', () => {
            const eachTask: WorkflowTask = {
                ...makeTask('each_1'),
                parameters: {
                    iteratee: makeTask('child_1'),
                },
            };
            const definition = makeDefinition([eachTask]);

            useWorkflowDataStore.setState({
                workflow: {
                    ...useWorkflowDataStore.getState().workflow,
                    definition,
                    id: '1',
                    version: 1,
                },
            });

            const mockMutation = createMockMutation();

            saveWorkflowNodesPosition({
                draggedNodeId: 'each_1',
                nodePositions: {
                    child_1: {x: 300, y: 400},
                    each_1: {x: 100, y: 200},
                },
                updateWorkflowMutation: mockMutation as never,
            });

            const storeDefinition = useWorkflowDataStore.getState().workflow.definition!;
            const parsed = JSON.parse(storeDefinition);

            expect(parsed.tasks[0].metadata.ui.nodePosition).toEqual({x: 100, y: 200});
            expect(parsed.tasks[0].parameters.iteratee.metadata.ui.nodePosition).toEqual({x: 300, y: 400});
        });

        it('should clear positions for each dispatcher single-object iteratee child', () => {
            const eachTask: WorkflowTask = {
                ...makeTask('each_1', {x: 100, y: 200}),
                parameters: {
                    iteratee: makeTask('child_1', {x: 300, y: 400}),
                },
            };
            const definition = makeDefinition([eachTask]);

            useWorkflowDataStore.setState({
                workflow: {
                    ...useWorkflowDataStore.getState().workflow,
                    definition,
                    id: '1',
                    version: 1,
                },
            });

            const mockMutation = createMockMutation();

            clearAllNodePositions({
                incrementLayoutResetCounter: vi.fn(),
                invalidateWorkflowQueries: vi.fn(),
                updateWorkflowMutation: mockMutation as never,
            });

            const storeDefinition = useWorkflowDataStore.getState().workflow.definition!;
            const parsed = JSON.parse(storeDefinition);

            expect(parsed.tasks[0].metadata?.ui?.nodePosition).toBeUndefined();
            expect(parsed.tasks[0].parameters.iteratee.metadata?.ui?.nodePosition).toBeUndefined();
        });
    });

    describe('mutation queuing when guard is active', () => {
        it('should queue definition when mutation is in flight and still sync store', () => {
            const tasks = [makeTask('task_1')];
            const definition = makeDefinition(tasks);

            useWorkflowDataStore.setState({
                workflow: {
                    ...useWorkflowDataStore.getState().workflow,
                    definition,
                    id: '1',
                    version: 1,
                },
            });

            // Simulate an in-flight mutation
            setWorkflowMutating('1', true);

            const mockMutation = createMockMutation();

            saveWorkflowNodesPosition({
                draggedNodeId: 'task_1',
                nodePositions: {task_1: {x: 200, y: 300}},
                updateWorkflowMutation: mockMutation as never,
            });

            // Store definition should still be synced
            const storeDefinition = useWorkflowDataStore.getState().workflow.definition!;
            const parsed = JSON.parse(storeDefinition);

            expect(parsed.tasks[0].metadata.ui.nodePosition).toEqual({x: 200, y: 300});
            // But mutation should NOT be called (guard active)
            expect(mockMutation.mutate).not.toHaveBeenCalled();
        });

        it('should drain pending definitions when first mutation settles', () => {
            const tasks = [makeTask('task_1'), makeTask('task_2')];
            const definition = makeDefinition(tasks);

            useWorkflowDataStore.setState({
                workflow: {
                    ...useWorkflowDataStore.getState().workflow,
                    definition,
                    id: '1',
                    version: 1,
                },
            });

            const mockMutation = createMockMutation();

            // First save: fires the mutation
            saveWorkflowNodesPosition({
                draggedNodeId: 'task_1',
                nodePositions: {task_1: {x: 100, y: 200}},
                updateWorkflowMutation: mockMutation as never,
            });

            expect(mockMutation.mutate).toHaveBeenCalledTimes(1);

            // Second save: mutation is in flight so it gets queued
            saveWorkflowNodesPosition({
                draggedNodeId: 'task_2',
                nodePositions: {task_2: {x: 300, y: 400}},
                updateWorkflowMutation: mockMutation as never,
            });

            expect(mockMutation.mutate).toHaveBeenCalledTimes(1);

            // Simulate first mutation's onSuccess — only updates version, no query invalidation
            const firstMutateCall = mockMutation.mutate.mock.calls[0];
            const firstCallbacks = firstMutateCall[1];

            firstCallbacks.onSuccess({version: 2});

            // Simulate first mutation's onSettled — this should fire the queued mutation
            firstCallbacks.onSettled();

            expect(mockMutation.mutate).toHaveBeenCalledTimes(2);

            // The second mutation should use the queued definition with task_2's position
            const secondMutateCall = mockMutation.mutate.mock.calls[1];
            const secondDefinition = JSON.parse(secondMutateCall[0].workflow.definition);

            expect(secondDefinition.tasks[1].metadata.ui.nodePosition).toEqual({x: 300, y: 400});
        });
    });

    describe('position mutation does not invalidate queries', () => {
        it('should fire mutation exactly once when no pending definitions exist', () => {
            const tasks = [makeTask('task_1')];
            const definition = makeDefinition(tasks);

            useWorkflowDataStore.setState({
                workflow: {
                    ...useWorkflowDataStore.getState().workflow,
                    definition,
                    id: '1',
                    version: 1,
                },
            });

            const mockMutation = createMockMutation();

            saveWorkflowNodesPosition({
                draggedNodeId: 'task_1',
                nodePositions: {task_1: {x: 200, y: 300}},
                updateWorkflowMutation: mockMutation as never,
            });

            // Simulate mutation success
            const mutateCall = mockMutation.mutate.mock.calls[0];
            const callbacks = mutateCall[1];

            callbacks.onSuccess({version: 2});
            callbacks.onSettled();

            // The mutation should have been called exactly once (no refetch-triggered re-saves)
            expect(mockMutation.mutate).toHaveBeenCalledTimes(1);
        });

        it('should update workflow version on success while preserving definition positions', () => {
            const tasks = [makeTask('task_1')];
            const definition = makeDefinition(tasks);

            useWorkflowDataStore.setState({
                workflow: {
                    ...useWorkflowDataStore.getState().workflow,
                    definition,
                    id: '1',
                    version: 1,
                },
            });

            const mockMutation = createMockMutation();

            saveWorkflowNodesPosition({
                draggedNodeId: 'task_1',
                nodePositions: {task_1: {x: 200, y: 300}},
                updateWorkflowMutation: mockMutation as never,
            });

            // Simulate mutation success with new version
            const mutateCall = mockMutation.mutate.mock.calls[0];
            const callbacks = mutateCall[1];

            callbacks.onSuccess({version: 5});

            // Version should be updated
            const workflow = useWorkflowDataStore.getState().workflow;

            expect(workflow.version).toBe(5);

            // Definition should still contain the saved position
            const parsed = JSON.parse(workflow.definition!);

            expect(parsed.tasks[0].metadata.ui.nodePosition).toEqual({x: 200, y: 300});
        });

        it('should preserve definition through full mutation lifecycle with pending drain', () => {
            const tasks = [makeTask('task_1'), makeTask('task_2')];
            const definition = makeDefinition(tasks);

            useWorkflowDataStore.setState({
                workflow: {
                    ...useWorkflowDataStore.getState().workflow,
                    definition,
                    id: '1',
                    version: 1,
                },
            });

            const mockMutation = createMockMutation();

            // First save
            saveWorkflowNodesPosition({
                draggedNodeId: 'task_1',
                nodePositions: {task_1: {x: 100, y: 200}},
                updateWorkflowMutation: mockMutation as never,
            });

            // Second save (queued)
            saveWorkflowNodesPosition({
                draggedNodeId: 'task_2',
                nodePositions: {task_2: {x: 300, y: 400}},
                updateWorkflowMutation: mockMutation as never,
            });

            // Complete first mutation
            const firstCallbacks = mockMutation.mutate.mock.calls[0][1];

            firstCallbacks.onSuccess({version: 2});
            firstCallbacks.onSettled();

            // Complete second (drained) mutation
            const secondCallbacks = mockMutation.mutate.mock.calls[1][1];

            secondCallbacks.onSuccess({version: 3});
            secondCallbacks.onSettled();

            // Both positions should survive through the entire lifecycle
            const finalWorkflow = useWorkflowDataStore.getState().workflow;
            const parsed = JSON.parse(finalWorkflow.definition!);

            expect(finalWorkflow.version).toBe(3);
            expect(parsed.tasks[0].metadata.ui.nodePosition).toEqual({x: 100, y: 200});
            expect(parsed.tasks[1].metadata.ui.nodePosition).toEqual({x: 300, y: 400});
        });
    });

    describe('onError rollback', () => {
        it('should restore both nodes and definition on mutation error', () => {
            const tasks = [makeTask('task_1')];
            const definition = makeDefinition(tasks);

            useWorkflowDataStore.setState({
                nodes: [
                    {
                        data: {name: 'task_1', type: 'test/task_1'},
                        id: 'task_1',
                        position: {x: 0, y: 0},
                        type: 'workflow',
                    },
                ],
                workflow: {
                    ...useWorkflowDataStore.getState().workflow,
                    definition,
                    id: '1',
                    version: 1,
                },
            });

            const mockMutation = createMockMutation();

            saveWorkflowNodesPosition({
                draggedNodeId: 'task_1',
                nodePositions: {task_1: {x: 200, y: 300}},
                updateWorkflowMutation: mockMutation as never,
            });

            // Verify optimistic update applied
            const optimisticDefinition = useWorkflowDataStore.getState().workflow.definition!;
            const optimisticParsed = JSON.parse(optimisticDefinition);

            expect(optimisticParsed.tasks[0].metadata.ui.nodePosition).toEqual({x: 200, y: 300});

            // Simulate mutation error
            const mutateCall = mockMutation.mutate.mock.calls[0];
            const callbacks = mutateCall[1];

            callbacks.onError();

            // Nodes should be restored to pre-drag state
            const restoredNodes = useWorkflowDataStore.getState().nodes;

            expect((restoredNodes[0].data as NodeDataType).metadata?.ui?.nodePosition).toBeUndefined();

            // Definition should be restored to original (no positions)
            const restoredDefinition = useWorkflowDataStore.getState().workflow.definition!;
            const restoredParsed = JSON.parse(restoredDefinition);

            expect(restoredParsed.tasks[0].metadata?.ui?.nodePosition).toBeUndefined();
        });

        it('should restore definition on error for a queued mutation fired from onSettled', () => {
            const tasks = [makeTask('task_1'), makeTask('task_2')];
            const definition = makeDefinition(tasks);

            useWorkflowDataStore.setState({
                nodes: [
                    {
                        data: {name: 'task_1', type: 'test/task_1'},
                        id: 'task_1',
                        position: {x: 0, y: 0},
                        type: 'workflow',
                    },
                    {
                        data: {name: 'task_2', type: 'test/task_2'},
                        id: 'task_2',
                        position: {x: 0, y: 100},
                        type: 'workflow',
                    },
                ],
                workflow: {
                    ...useWorkflowDataStore.getState().workflow,
                    definition,
                    id: '1',
                    version: 1,
                },
            });

            const mockMutation = createMockMutation();

            // First save succeeds
            saveWorkflowNodesPosition({
                draggedNodeId: 'task_1',
                nodePositions: {task_1: {x: 100, y: 200}},
                updateWorkflowMutation: mockMutation as never,
            });

            // Second save queued
            saveWorkflowNodesPosition({
                draggedNodeId: 'task_2',
                nodePositions: {task_2: {x: 300, y: 400}},
                updateWorkflowMutation: mockMutation as never,
            });

            // Complete first mutation successfully
            const firstCallbacks = mockMutation.mutate.mock.calls[0][1];

            firstCallbacks.onSuccess({version: 2});
            firstCallbacks.onSettled();

            // Second mutation was drained from onSettled
            expect(mockMutation.mutate).toHaveBeenCalledTimes(2);

            // Capture definition state before second mutation error
            const definitionBeforeError = useWorkflowDataStore.getState().workflow.definition!;
            const parsedBeforeError = JSON.parse(definitionBeforeError);

            // task_1 position should be present (from first successful save)
            expect(parsedBeforeError.tasks[0].metadata.ui.nodePosition).toEqual({x: 100, y: 200});

            // Simulate second mutation error
            const secondCallbacks = mockMutation.mutate.mock.calls[1][1];

            secondCallbacks.onError();

            // Definition should be restored to state before second mutation
            const restoredDefinition = useWorkflowDataStore.getState().workflow.definition!;
            const restoredParsed = JSON.parse(restoredDefinition);

            // task_1 position should survive (it was saved by first mutation)
            expect(restoredParsed.tasks[0].metadata.ui.nodePosition).toEqual({x: 100, y: 200});
        });

        it('should clear mutation guard on error so subsequent saves can fire', () => {
            const tasks = [makeTask('task_1')];
            const definition = makeDefinition(tasks);

            useWorkflowDataStore.setState({
                workflow: {
                    ...useWorkflowDataStore.getState().workflow,
                    definition,
                    id: '1',
                    version: 1,
                },
            });

            const mockMutation = createMockMutation();

            // First save
            saveWorkflowNodesPosition({
                draggedNodeId: 'task_1',
                nodePositions: {task_1: {x: 100, y: 200}},
                updateWorkflowMutation: mockMutation as never,
            });

            // Simulate error followed by settled
            const firstCallbacks = mockMutation.mutate.mock.calls[0][1];

            firstCallbacks.onError();
            firstCallbacks.onSettled();

            // Second save should fire immediately (guard should be cleared)
            saveWorkflowNodesPosition({
                draggedNodeId: 'task_1',
                nodePositions: {task_1: {x: 500, y: 600}},
                updateWorkflowMutation: mockMutation as never,
            });

            expect(mockMutation.mutate).toHaveBeenCalledTimes(2);
        });
    });

    describe('ReactFlow node position resilience after stale refetch', () => {
        it('should preserve ReactFlow node positions when definition is overwritten by stale refetch', () => {
            const tasks = [makeTask('task_1'), makeTask('task_2')];
            const definition = makeDefinition(tasks);

            useWorkflowDataStore.setState({
                nodes: [
                    {
                        data: {name: 'task_1', type: 'test/task_1'},
                        id: 'task_1',
                        position: {x: 0, y: 0},
                        type: 'workflow',
                    },
                    {
                        data: {name: 'task_2', type: 'test/task_2'},
                        id: 'task_2',
                        position: {x: 0, y: 100},
                        type: 'workflow',
                    },
                ],
                workflow: {
                    ...useWorkflowDataStore.getState().workflow,
                    definition,
                    id: '1',
                    version: 1,
                },
            });

            const mockMutation = createMockMutation();

            // Save position for task_1 — this updates both the definition and ReactFlow node data
            saveWorkflowNodesPosition({
                draggedNodeId: 'task_1',
                nodePositions: {task_1: {x: 200, y: 300}},
                updateWorkflowMutation: mockMutation as never,
            });

            // Verify ReactFlow node data has the optimistic position
            const nodesAfterSave = useWorkflowDataStore.getState().nodes;
            const task1Node = nodesAfterSave.find((node) => node.id === 'task_1')!;

            expect((task1Node.data as NodeDataType).metadata!.ui!.nodePosition).toEqual({x: 200, y: 300});

            // Simulate a stale refetch overwriting the workflow definition
            // (this is what useProject.ts does: setWorkflow({...currentWorkflow}))
            // The stale server data does NOT have task_1's new position
            const staleServerDefinition = makeDefinition(tasks); // no positions

            useWorkflowDataStore.setState((state) => ({
                workflow: {
                    ...state.workflow,
                    definition: staleServerDefinition,
                },
            }));

            // The definition no longer has the position
            const staleDefinition = useWorkflowDataStore.getState().workflow.definition!;
            const staleParsed = JSON.parse(staleDefinition);

            expect(staleParsed.tasks[0].metadata?.ui?.nodePosition).toBeUndefined();

            // But ReactFlow nodes are NOT affected by setWorkflow — they retain
            // the optimistic position. This is the invariant the layout effect
            // relies on as a fallback when the definition is stale.
            const nodesAfterOverwrite = useWorkflowDataStore.getState().nodes;
            const task1NodeAfterOverwrite = nodesAfterOverwrite.find((node) => node.id === 'task_1')!;

            expect((task1NodeAfterOverwrite.data as NodeDataType).metadata!.ui!.nodePosition).toEqual({
                x: 200,
                y: 300,
            });
        });

        it('should preserve multiple node positions when definition is overwritten', () => {
            const tasks = [makeTask('task_1'), makeTask('task_2')];
            const definition = makeDefinition(tasks);

            useWorkflowDataStore.setState({
                nodes: [
                    {
                        data: {name: 'task_1', type: 'test/task_1'},
                        id: 'task_1',
                        position: {x: 0, y: 0},
                        type: 'workflow',
                    },
                    {
                        data: {name: 'task_2', type: 'test/task_2'},
                        id: 'task_2',
                        position: {x: 0, y: 100},
                        type: 'workflow',
                    },
                ],
                workflow: {
                    ...useWorkflowDataStore.getState().workflow,
                    definition,
                    id: '1',
                    version: 1,
                },
            });

            const mockMutation = createMockMutation();

            // Save position for task_1
            saveWorkflowNodesPosition({
                draggedNodeId: 'task_1',
                nodePositions: {task_1: {x: 200, y: 300}},
                updateWorkflowMutation: mockMutation as never,
            });

            // Save position for task_2 (queued because task_1 mutation is in flight)
            saveWorkflowNodesPosition({
                draggedNodeId: 'task_2',
                nodePositions: {task_2: {x: 400, y: 500}},
                updateWorkflowMutation: mockMutation as never,
            });

            // Simulate stale refetch that only has task_1's position (from first mutation)
            const partialServerDefinition = makeDefinition([makeTask('task_1', {x: 200, y: 300}), makeTask('task_2')]);

            useWorkflowDataStore.setState((state) => ({
                workflow: {
                    ...state.workflow,
                    definition: partialServerDefinition,
                },
            }));

            // ReactFlow nodes still have both positions
            const nodesAfterOverwrite = useWorkflowDataStore.getState().nodes;
            const task1Node = nodesAfterOverwrite.find((node) => node.id === 'task_1')!;
            const task2Node = nodesAfterOverwrite.find((node) => node.id === 'task_2')!;

            expect((task1Node.data as NodeDataType).metadata!.ui!.nodePosition).toEqual({x: 200, y: 300});
            expect((task2Node.data as NodeDataType).metadata!.ui!.nodePosition).toEqual({x: 400, y: 500});
        });
    });
});
