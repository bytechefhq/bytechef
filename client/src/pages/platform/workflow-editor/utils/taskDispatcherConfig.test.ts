import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {describe, expect, it} from 'vitest';

import {TASK_DISPATCHER_CONFIG} from './taskDispatcherConfig';

function task(overrides: Partial<WorkflowTask> & {name: string}): WorkflowTask {
    return {
        parameters: {},
        type: 'test/v1/action',
        ...overrides,
    };
}

describe('TASK_DISPATCHER_CONFIG.graph', () => {
    describe('initializeParameters', () => {
        it('should return maxTransitions: 100 and an empty nodes array', () => {
            expect(TASK_DISPATCHER_CONFIG.graph.initializeParameters()).toEqual({
                maxTransitions: 100,
                nodes: [],
            });
        });
    });

    describe('getDispatcherId', () => {
        it('should read graphId off the context', () => {
            expect(
                TASK_DISPATCHER_CONFIG.graph.getDispatcherId({graphId: 'graph_1', taskDispatcherId: 'graph_1'})
            ).toBe('graph_1');
        });
    });

    describe('extractContextFromPlaceholder', () => {
        it('should parse the node index and graph id from the trailing add-node placeholder', () => {
            const context = TASK_DISPATCHER_CONFIG.graph.extractContextFromPlaceholder(
                'graph_1-graph-node-0-placeholder-0'
            );

            expect(context).toEqual({nodeIndex: 0, taskDispatcherId: 'graph_1'});
        });

        it('should parse a non-zero node index', () => {
            const context = TASK_DISPATCHER_CONFIG.graph.extractContextFromPlaceholder(
                'graph_1-graph-node-2-placeholder-0'
            );

            expect(context).toEqual({nodeIndex: 2, taskDispatcherId: 'graph_1'});
        });
    });

    describe('getSubtasks', () => {
        it('should return an empty array when there are no nodes', () => {
            const graphTask = task({name: 'graph_1', parameters: {maxTransitions: 100, nodes: []}});

            expect(
                TASK_DISPATCHER_CONFIG.graph.getSubtasks({
                    context: {nodeIndex: 0, taskDispatcherId: 'graph_1'},
                    task: graphTask,
                })
            ).toEqual([]);
        });

        it('should return an empty array for a nodeIndex past the end (trailing add-node placeholder)', () => {
            const graphTask = task({
                name: 'graph_1',
                parameters: {maxTransitions: 100, nodes: [{name: 'node_0', tasks: [task({name: 'action_1'})]}]},
            });

            expect(
                TASK_DISPATCHER_CONFIG.graph.getSubtasks({
                    context: {nodeIndex: 1, taskDispatcherId: 'graph_1'},
                    task: graphTask,
                })
            ).toEqual([]);
        });

        it("should return the targeted node's tasks", () => {
            const nodeTasks = [task({name: 'action_1'}), task({name: 'action_2'})];
            const graphTask = task({
                name: 'graph_1',
                parameters: {
                    maxTransitions: 100,
                    nodes: [
                        {name: 'node_0', tasks: [task({name: 'other_action'})]},
                        {name: 'node_1', tasks: nodeTasks},
                    ],
                },
            });

            expect(
                TASK_DISPATCHER_CONFIG.graph.getSubtasks({
                    context: {nodeIndex: 1, taskDispatcherId: 'graph_1'},
                    task: graphTask,
                })
            ).toEqual(nodeTasks);
        });

        it('should flatten every node when getAllSubtasks is true', () => {
            const graphTask = task({
                name: 'graph_1',
                parameters: {
                    maxTransitions: 100,
                    nodes: [
                        {name: 'node_0', tasks: [task({name: 'action_1'})]},
                        {name: 'node_1', tasks: [task({name: 'action_2'}), task({name: 'action_3'})]},
                    ],
                },
            });

            const allSubtasks = TASK_DISPATCHER_CONFIG.graph.getSubtasks({getAllSubtasks: true, task: graphTask});

            expect(allSubtasks.map((subtask) => subtask.name)).toEqual(['action_1', 'action_2', 'action_3']);
        });
    });

    describe('updateTaskParameters', () => {
        it('should update an existing node in place, preserving its name and next expression', () => {
            const graphTask = task({
                name: 'graph_1',
                parameters: {
                    maxTransitions: 100,
                    nodes: [{name: 'node_0', next: 'node_1', tasks: [task({name: 'old_action'})]}],
                },
            });

            const updatedTask = TASK_DISPATCHER_CONFIG.graph.updateTaskParameters({
                context: {nodeIndex: 0, taskDispatcherId: 'graph_1'},
                task: graphTask,
                updatedSubtasks: [task({name: 'new_action'})],
            });

            expect(updatedTask.parameters?.nodes).toEqual([
                {name: 'node_0', next: 'node_1', tasks: [task({name: 'new_action'})]},
            ]);
        });

        it('should create a new node with a generated unique name when inserting past the end', () => {
            const graphTask = task({name: 'graph_1', parameters: {maxTransitions: 100, nodes: []}});

            const updatedTask = TASK_DISPATCHER_CONFIG.graph.updateTaskParameters({
                context: {nodeIndex: 0, taskDispatcherId: 'graph_1'},
                task: graphTask,
                updatedSubtasks: [task({name: 'action_1'})],
            });

            expect(updatedTask.parameters?.nodes).toEqual([{name: 'node_0', tasks: [task({name: 'action_1'})]}]);
        });

        it('should skip an already-taken generated name when creating a second new node', () => {
            const graphTask = task({
                name: 'graph_1',
                parameters: {
                    maxTransitions: 100,
                    nodes: [{name: 'node_0', tasks: [task({name: 'action_1'})]}],
                },
            });

            const updatedTask = TASK_DISPATCHER_CONFIG.graph.updateTaskParameters({
                context: {nodeIndex: 1, taskDispatcherId: 'graph_1'},
                task: graphTask,
                updatedSubtasks: [task({name: 'action_2'})],
            });

            expect(updatedTask.parameters?.nodes).toEqual([
                {name: 'node_0', tasks: [task({name: 'action_1'})]},
                {name: 'node_1', tasks: [task({name: 'action_2'})]},
            ]);
        });

        it('should reuse the freed node_0 name when a differently-named node occupies slot 0', () => {
            const graphTask = task({
                name: 'graph_1',
                parameters: {
                    maxTransitions: 100,
                    nodes: [{name: 'router', tasks: [task({name: 'action_1'})]}],
                },
            });

            const updatedTask = TASK_DISPATCHER_CONFIG.graph.updateTaskParameters({
                context: {nodeIndex: 1, taskDispatcherId: 'graph_1'},
                task: graphTask,
                updatedSubtasks: [task({name: 'action_2'})],
            });

            expect(updatedTask.parameters?.nodes).toEqual([
                {name: 'router', tasks: [task({name: 'action_1'})]},
                {name: 'node_0', tasks: [task({name: 'action_2'})]},
            ]);
        });
    });
});
