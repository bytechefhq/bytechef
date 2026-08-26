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

// `graph/v1`'s `nodes` is now a plain task list (a node IS one task, and `transitions` is a
// sibling edge list) — shaped like `parallel`'s `tasks`, not the retired `{name, tasks, next}`
// per-lane shape.
describe('TASK_DISPATCHER_CONFIG.graph', () => {
    describe('initializeParameters', () => {
        it('should return maxTransitions: 100 and empty nodes/transitions arrays', () => {
            expect(TASK_DISPATCHER_CONFIG.graph.initializeParameters()).toEqual({
                maxTransitions: 100,
                nodes: [],
                transitions: [],
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
        it('should parse the graph id from the trailing add-node placeholder', () => {
            const context = TASK_DISPATCHER_CONFIG.graph.extractContextFromPlaceholder('graph_1-graph-placeholder');

            expect(context).toEqual({taskDispatcherId: 'graph_1'});
        });
    });

    describe('getSubtasks', () => {
        it('should return an empty array when there are no nodes', () => {
            const graphTask = task({name: 'graph_1', parameters: {maxTransitions: 100, nodes: [], transitions: []}});

            expect(TASK_DISPATCHER_CONFIG.graph.getSubtasks({task: graphTask})).toEqual([]);
        });

        it('should return the declared nodes, in declaration order', () => {
            const nodes = [task({name: 'action_1'}), task({name: 'action_2'})];
            const graphTask = task({
                name: 'graph_1',
                parameters: {maxTransitions: 100, nodes, transitions: []},
            });

            expect(TASK_DISPATCHER_CONFIG.graph.getSubtasks({task: graphTask})).toEqual(nodes);
        });
    });

    describe('updateTaskParameters', () => {
        it('should replace the nodes array with updatedSubtasks', () => {
            const graphTask = task({
                name: 'graph_1',
                parameters: {
                    maxTransitions: 100,
                    nodes: [task({name: 'old_action'})],
                    transitions: [],
                },
            });

            const updatedTask = TASK_DISPATCHER_CONFIG.graph.updateTaskParameters({
                task: graphTask,
                updatedSubtasks: [task({name: 'new_action_1'}), task({name: 'new_action_2'})],
            });

            expect(updatedTask.parameters?.nodes).toEqual([task({name: 'new_action_1'}), task({name: 'new_action_2'})]);
            // Sibling parameters (like `transitions`) survive an update untouched.
            expect(updatedTask.parameters?.transitions).toEqual([]);
        });
    });
});
