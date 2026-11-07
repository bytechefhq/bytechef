import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {TaskDispatcherContextType} from '@/shared/types';
import {Node} from '@xyflow/react';
import {afterEach, beforeEach, describe, expect, it} from 'vitest';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import {getContextFromPlaceholderNode} from './getTaskDispatcherContext';
import {AutoPlacedGraphPositionsRefType, registerAutoPlacedGraphPositions} from './graph/autoPlacedGraphPositions';
import insertTaskDispatcherSubtask from './insertTaskDispatcherSubtask';

function task(overrides: Partial<WorkflowTask> & {name: string}): WorkflowTask {
    return {
        parameters: {},
        type: 'test/v1/action',
        ...overrides,
    };
}

function placedAt(newTask: WorkflowTask, nodePosition: {x: number; y: number}): WorkflowTask {
    return {...newTask, metadata: {...newTask.metadata, ui: {...newTask.metadata?.ui, nodePosition}}};
}

function leftForAutoPlacement(newTask: WorkflowTask): WorkflowTask {
    return newTask;
}

function graphAddPlaceholderNode(): Node {
    return {
        data: {graphId: 'graph_1', taskDispatcherId: 'graph_1'},
        id: 'graph_1-graph-placeholder',
        position: {x: 0, y: 0},
        type: 'placeholder',
    };
}

function graphAddPlaceholderContext(): TaskDispatcherContextType {
    const context = getContextFromPlaceholderNode(graphAddPlaceholderNode());

    if (!context) {
        throw new Error('The graph add-node placeholder must resolve a context');
    }

    return context;
}

function graphTask(nodes: WorkflowTask[] = []): WorkflowTask {
    return task({
        name: 'graph_1',
        parameters: {maxTransitions: 100, nodes, transitions: []},
        type: 'graph/v1',
    });
}

describe('insertTaskDispatcherSubtask', () => {
    it('should insert into a fork-join branch whose node name is camelCased', () => {
        const forkJoinTask = task({
            name: 'forkJoin_1',
            parameters: {branches: [[task({name: 'first_action'})]]},
            type: 'fork-join/v1',
        });

        const updatedTasks = insertTaskDispatcherSubtask({
            newTask: task({name: 'second_action'}),
            taskDispatcherContext: {branchIndex: 0, index: 1, taskDispatcherId: 'forkJoin_1'},
            tasks: [forkJoinTask],
        });

        const branches = updatedTasks[0].parameters?.branches as WorkflowTask[][];

        expect(branches[0].map((subtask) => subtask.name)).toEqual(['first_action', 'second_action']);
    });

    it('should still insert into a fork-join branch with a hyphenated node name', () => {
        const forkJoinTask = task({
            name: 'fork-join_1',
            parameters: {branches: [[task({name: 'first_action'})]]},
            type: 'fork-join/v1',
        });

        const updatedTasks = insertTaskDispatcherSubtask({
            newTask: task({name: 'second_action'}),
            taskDispatcherContext: {branchIndex: 0, index: 1, taskDispatcherId: 'fork-join_1'},
            tasks: [forkJoinTask],
        });

        const branches = updatedTasks[0].parameters?.branches as WorkflowTask[][];

        expect(branches[0].map((subtask) => subtask.name)).toEqual(['first_action', 'second_action']);
    });
});

describe('insertTaskDispatcherSubtask - graph', () => {
    let unregisterAutoPlacedGraphPositions: (() => void) | undefined;

    afterEach(() => {
        unregisterAutoPlacedGraphPositions?.();

        unregisterAutoPlacedGraphPositions = undefined;
    });

    beforeEach(() => {
        useWorkflowDataStore.setState({nodes: []});

        useWorkflowEditorStore.getState().setGraphPendingConnection(undefined);
    });

    it('should append a task onto an empty graph via the add-node placeholder', () => {
        const newTask = task({name: 'httpClient_1', type: 'httpClient/v1/get'});

        const updatedTasks = insertTaskDispatcherSubtask({
            newTask,
            placeholderId: 'graph_1-graph-placeholder',
            taskDispatcherContext: graphAddPlaceholderContext(),
            tasks: [graphTask([])],
        });

        expect(updatedTasks[0].parameters?.nodes).toEqual([leftForAutoPlacement(newTask)]);
    });

    it('should append a second task after an existing node, not prepend before it', () => {
        const existingTask = task({name: 'existing_action'});
        const newTask = task({name: 'httpClient_2', type: 'httpClient/v1/get'});

        const updatedTasks = insertTaskDispatcherSubtask({
            newTask,
            placeholderId: 'graph_1-graph-placeholder',
            taskDispatcherContext: graphAddPlaceholderContext(),
            tasks: [graphTask([existingTask])],
        });

        expect(updatedTasks[0].parameters?.nodes).toEqual([existingTask, leftForAutoPlacement(newTask)]);
    });

    it('should append a task dispatcher added through the add-node placeholder, which forwards no placeholderId', () => {
        const existingTask = task({name: 'existing_action'});
        const newDispatcher = task({name: 'condition_1', type: 'condition/v1'});

        const updatedTasks = insertTaskDispatcherSubtask({
            newTask: newDispatcher,
            taskDispatcherContext: graphAddPlaceholderContext(),
            tasks: [graphTask([existingTask])],
        });

        const nodes = updatedTasks[0].parameters?.nodes as WorkflowTask[];

        expect(nodes.map((node) => node.name)).toEqual(['existing_action', 'condition_1']);
    });

    it('should land a task added through the add-node placeholder at nodes.length', () => {
        const existingTasks = [
            task({name: 'first_action'}),
            task({name: 'second_action'}),
            task({name: 'third_action'}),
        ];
        const newTask = task({name: 'httpClient_3', type: 'httpClient/v1/get'});

        const updatedTasks = insertTaskDispatcherSubtask({
            newTask,
            placeholderId: 'graph_1-graph-placeholder',
            taskDispatcherContext: graphAddPlaceholderContext(),
            tasks: [graphTask(existingTasks)],
        });

        const nodes = updatedTasks[0].parameters?.nodes as WorkflowTask[];

        expect(nodes).toHaveLength(existingTasks.length + 1);
        expect(nodes[existingTasks.length]).toEqual(leftForAutoPlacement(newTask));
    });

    it('should insert a task at a specific declared index rather than always appending', () => {
        const firstTask = task({name: 'first_action'});
        const thirdTask = placedAt(task({name: 'third_action'}), {x: 300, y: 120});
        const newTask = task({name: 'second_action'});

        const updatedTasks = insertTaskDispatcherSubtask({
            newTask,
            taskDispatcherContext: {index: 1, taskDispatcherId: 'graph_1'},
            tasks: [graphTask([firstTask, thirdTask])],
        });

        const nodes = updatedTasks[0].parameters?.nodes as WorkflowTask[];

        expect(nodes.map((node) => node.name)).toEqual(['first_action', 'second_action', 'third_action']);
        expect(nodes[0]).toEqual(firstTask);
        expect(nodes[2]).toEqual(thirdTask);
    });

    it('should keep every following member position intact when inserting mid-list', () => {
        const firstTask = placedAt(task({name: 'first_action'}), {x: 0, y: 0});
        const thirdTask = placedAt(task({name: 'third_action'}), {x: 300, y: 120});
        const fourthTask = placedAt(task({name: 'fourth_action'}), {x: 620, y: 260});
        const newTask = task({name: 'second_action'});

        const updatedTasks = insertTaskDispatcherSubtask({
            newTask,
            taskDispatcherContext: {graphId: 'graph_1', index: 1, taskDispatcherId: 'graph_1'},
            tasks: [graphTask([firstTask, thirdTask, fourthTask])],
        });

        const nodes = updatedTasks[0].parameters?.nodes as WorkflowTask[];

        expect(nodes.map((node) => node.name)).toEqual([
            'first_action',
            'second_action',
            'third_action',
            'fourth_action',
        ]);
        expect(nodes[2].metadata?.ui?.nodePosition).toEqual({x: 300, y: 120});
        expect(nodes[3].metadata?.ui?.nodePosition).toEqual({x: 620, y: 260});
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
            taskDispatcherContext: {taskDispatcherId: 'graph_1'},
            tasks: [loopWrapper],
        });

        const updatedNestedGraph = (updatedTasks[0].parameters?.iteratee as WorkflowTask[])[0];

        expect(updatedNestedGraph.parameters?.nodes).toEqual([leftForAutoPlacement(newTask)]);
    });

    it('should land a task at the pending drop position and connect it to the source member', () => {
        const existingTask = task({name: 'existing_action'});
        const newTask = task({name: 'httpClient_4', type: 'httpClient/v1/get'});

        useWorkflowEditorStore
            .getState()
            .setGraphPendingConnection({dropPosition: {x: 410, y: 220}, from: 'existing_action', graphId: 'graph_1'});

        const updatedTasks = insertTaskDispatcherSubtask({
            newTask,
            placeholderId: 'graph_1-graph-placeholder',
            taskDispatcherContext: graphAddPlaceholderContext(),
            tasks: [graphTask([existingTask])],
        });

        expect((updatedTasks[0].parameters?.nodes as WorkflowTask[])[1].metadata?.ui?.nodePosition).toEqual({
            x: 410,
            y: 220,
        });
        expect(updatedTasks[0].parameters?.transitions).toEqual([{from: 'existing_action', to: 'httpClient_4'}]);
        expect(useWorkflowEditorStore.getState().graphPendingConnection).toBeUndefined();
    });

    it('should land a task at the pending drop position without a transition when there is no source', () => {
        const newTask = task({name: 'httpClient_5', type: 'httpClient/v1/get'});

        useWorkflowEditorStore
            .getState()
            .setGraphPendingConnection({dropPosition: {x: 60, y: 90}, from: '', graphId: 'graph_1'});

        const updatedTasks = insertTaskDispatcherSubtask({
            newTask,
            placeholderId: 'graph_1-graph-placeholder',
            taskDispatcherContext: graphAddPlaceholderContext(),
            tasks: [graphTask([])],
        });

        expect((updatedTasks[0].parameters?.nodes as WorkflowTask[])[0].metadata?.ui?.nodePosition).toEqual({
            x: 60,
            y: 90,
        });
        expect(updatedTasks[0].parameters?.transitions).toEqual([]);
    });

    it('should leave a pending connection raised for another graph untouched', () => {
        const pendingConnection = {dropPosition: {x: 410, y: 220}, from: 'other_1', graphId: 'graph_2'};

        useWorkflowEditorStore.getState().setGraphPendingConnection(pendingConnection);

        const newTask = task({name: 'httpClient_6', type: 'httpClient/v1/get'});

        const updatedTasks = insertTaskDispatcherSubtask({
            newTask,
            placeholderId: 'graph_1-graph-placeholder',
            taskDispatcherContext: graphAddPlaceholderContext(),
            tasks: [graphTask([])],
        });

        expect(updatedTasks[0].parameters?.nodes).toEqual([leftForAutoPlacement(newTask)]);
        expect(useWorkflowEditorStore.getState().graphPendingConnection).toEqual(pendingConnection);
    });

    it('should flush the pending auto-placed positions and empty the channel', () => {
        const autoPlacedGraphPositionsRef: AutoPlacedGraphPositionsRefType = {
            current: {graph_1: {existing_action: {x: 300, y: 120}}},
        };

        unregisterAutoPlacedGraphPositions = registerAutoPlacedGraphPositions(autoPlacedGraphPositionsRef);

        const updatedTasks = insertTaskDispatcherSubtask({
            newTask: task({name: 'httpClient_7', type: 'httpClient/v1/get'}),
            placeholderId: 'graph_1-graph-placeholder',
            taskDispatcherContext: graphAddPlaceholderContext(),
            tasks: [graphTask([task({name: 'existing_action'})])],
        });

        const nodes = updatedTasks[0].parameters?.nodes as WorkflowTask[];

        expect(nodes[0].metadata?.ui?.nodePosition).toEqual({x: 300, y: 120});
        expect(autoPlacedGraphPositionsRef.current.graph_1).toBeUndefined();
    });

    it('should initialize parameters (maxTransitions: 100, nodes: [], transitions: []) when the graph task has none yet', () => {
        const bareGraphTask = task({name: 'graph_1', type: 'graph/v1'});

        delete bareGraphTask.parameters;

        const newTask = task({name: 'first_action'});

        const updatedTasks = insertTaskDispatcherSubtask({
            newTask,
            taskDispatcherContext: {taskDispatcherId: 'graph_1'},
            tasks: [bareGraphTask],
        });

        expect(updatedTasks[0].parameters?.maxTransitions).toBe(100);
        expect(updatedTasks[0].parameters?.nodes).toEqual([leftForAutoPlacement(newTask)]);
        expect(updatedTasks[0].parameters?.transitions).toEqual([]);
    });
});
