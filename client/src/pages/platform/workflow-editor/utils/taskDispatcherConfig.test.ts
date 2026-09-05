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
            expect(updatedTask.parameters?.transitions).toEqual([]);
        });
    });
});

describe('TASK_DISPATCHER_CONFIG.callAiAgent', () => {
    it('should read callAiAgentId off the context', () => {
        expect(
            TASK_DISPATCHER_CONFIG.callAiAgent.getDispatcherId({callAiAgentId: 'agent_1', taskDispatcherId: 'agent_1'})
        ).toBe('agent_1');
    });

    it('should have no subtasks', () => {
        expect(TASK_DISPATCHER_CONFIG.callAiAgent.getSubtasks()).toEqual([]);
    });

    it('should parse the dispatcher id from a placeholder', () => {
        expect(TASK_DISPATCHER_CONFIG.callAiAgent.extractContextFromPlaceholder('agent_1-0')).toEqual({
            index: 0,
            taskDispatcherId: 'agent_1',
        });
    });
});
