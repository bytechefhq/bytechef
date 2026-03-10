import {SPACE} from '@/shared/constants';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';
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
                invalidateWorkflowQueries: vi.fn(),
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
                invalidateWorkflowQueries: vi.fn(),
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
                invalidateWorkflowQueries: vi.fn(),
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
                invalidateWorkflowQueries: vi.fn(),
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
                invalidateWorkflowQueries: vi.fn(),
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

        it('should not invalidate queries when there is a pending definition', () => {
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
            const invalidateWorkflowQueries = vi.fn();

            // First save: fires the mutation
            saveWorkflowNodesPosition({
                draggedNodeId: 'task_1',
                invalidateWorkflowQueries,
                nodePositions: {task_1: {x: 100, y: 200}},
                updateWorkflowMutation: mockMutation as never,
            });

            expect(mockMutation.mutate).toHaveBeenCalledTimes(1);

            // Second save: mutation is in flight so it gets queued
            saveWorkflowNodesPosition({
                draggedNodeId: 'task_2',
                invalidateWorkflowQueries,
                nodePositions: {task_2: {x: 300, y: 400}},
                updateWorkflowMutation: mockMutation as never,
            });

            expect(mockMutation.mutate).toHaveBeenCalledTimes(1);

            // Simulate first mutation's onSuccess
            const firstMutateCall = mockMutation.mutate.mock.calls[0];
            const firstCallbacks = firstMutateCall[1];

            firstCallbacks.onSuccess({version: 2});

            // invalidateWorkflowQueries should NOT be called because there's a pending definition
            expect(invalidateWorkflowQueries).not.toHaveBeenCalled();

            // Simulate first mutation's onSettled — this should fire the queued mutation
            firstCallbacks.onSettled();

            expect(mockMutation.mutate).toHaveBeenCalledTimes(2);

            // The second mutation should use the queued definition with task_2's position
            const secondMutateCall = mockMutation.mutate.mock.calls[1];
            const secondDefinition = JSON.parse(secondMutateCall[0].workflow.definition);

            expect(secondDefinition.tasks[1].metadata.ui.nodePosition).toEqual({x: 300, y: 400});
        });
    });
});
