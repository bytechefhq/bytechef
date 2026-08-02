import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {describe, expect, it} from 'vitest';

import insertTaskDispatcherSubtask from './insertTaskDispatcherSubtask';

function task(overrides: Partial<WorkflowTask> & {name: string}): WorkflowTask {
    return {
        parameters: {},
        type: 'test/v1/action',
        ...overrides,
    };
}

function graphTask(nodes: Array<{name: string; next?: string; tasks: WorkflowTask[]}> = []): WorkflowTask {
    return task({
        name: 'graph_1',
        parameters: {maxTransitions: 100, nodes},
        type: 'graph/v1',
    });
}

describe('insertTaskDispatcherSubtask — graph', () => {
    it('should create a named node on the trailing add-node placeholder of an empty graph', () => {
        const newTask = task({name: 'httpClient_1', type: 'httpClient/v1/get'});

        const updatedTasks = insertTaskDispatcherSubtask({
            newTask,
            taskDispatcherContext: {nodeIndex: 0, taskDispatcherId: 'graph_1'},
            tasks: [graphTask([])],
        });

        expect(updatedTasks[0].parameters?.nodes).toEqual([{name: 'node_0', tasks: [newTask]}]);
    });

    it('should create a second, distinctly-named node on the trailing placeholder', () => {
        const existingNode = {name: 'node_0', tasks: [task({name: 'existing_action'})]};
        const newTask = task({name: 'httpClient_2', type: 'httpClient/v1/get'});

        const updatedTasks = insertTaskDispatcherSubtask({
            newTask,
            taskDispatcherContext: {nodeIndex: 1, taskDispatcherId: 'graph_1'},
            tasks: [graphTask([existingNode])],
        });

        expect(updatedTasks[0].parameters?.nodes).toEqual([existingNode, {name: 'node_1', tasks: [newTask]}]);
    });

    it('should append into an existing node addressed by index, not by name', () => {
        const existingNode = {name: 'router', next: "'node_0'", tasks: [task({name: 'first_action'})]};
        const newTask = task({name: 'second_action'});

        const updatedTasks = insertTaskDispatcherSubtask({
            newTask,
            taskDispatcherContext: {index: 1, nodeIndex: 0, taskDispatcherId: 'graph_1'},
            tasks: [graphTask([existingNode])],
        });

        expect(updatedTasks[0].parameters?.nodes).toEqual([
            {
                name: 'router',
                next: "'node_0'",
                tasks: [task({name: 'first_action'}), newTask],
            },
        ]);
    });

    it('should resolve the target graph task via getTask when nested inside another dispatcher', () => {
        const newTask = task({name: 'inner_action'});
        const nestedGraph = graphTask([]);
        const loopWrapper = task({
            name: 'loop_1',
            parameters: {iteratee: [nestedGraph]},
            type: 'loop/v1',
        });

        const updatedTasks = insertTaskDispatcherSubtask({
            newTask,
            taskDispatcherContext: {nodeIndex: 0, taskDispatcherId: 'graph_1'},
            tasks: [loopWrapper],
        });

        const updatedNestedGraph = (updatedTasks[0].parameters?.iteratee as WorkflowTask[])[0];

        expect(updatedNestedGraph.parameters?.nodes).toEqual([{name: 'node_0', tasks: [newTask]}]);
    });

    it('should initialize parameters (maxTransitions: 100, nodes: []) when the graph task has none yet', () => {
        const bareGraphTask = task({name: 'graph_1', type: 'graph/v1'});

        delete bareGraphTask.parameters;

        const newTask = task({name: 'first_action'});

        const updatedTasks = insertTaskDispatcherSubtask({
            newTask,
            taskDispatcherContext: {nodeIndex: 0, taskDispatcherId: 'graph_1'},
            tasks: [bareGraphTask],
        });

        expect(updatedTasks[0].parameters?.maxTransitions).toBe(100);
        expect(updatedTasks[0].parameters?.nodes).toEqual([{name: 'node_0', tasks: [newTask]}]);
    });
});
