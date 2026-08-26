import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {afterEach, beforeEach, describe, expect, it} from 'vitest';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
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

/**
 * The task as it comes back out of a graph insertion: a new member is always given a concrete
 * position, so it never has to be auto-placed on the next layout. With no member on the canvas to
 * measure, the free spot is the one right of the Start pill.
 */
function placedAtFreeSpot(newTask: WorkflowTask): WorkflowTask {
    return placedAt(newTask, {x: 112, y: 0});
}

function graphTask(nodes: WorkflowTask[] = []): WorkflowTask {
    return task({
        name: 'graph_1',
        parameters: {maxTransitions: 100, nodes, transitions: []},
        type: 'graph/v1',
    });
}

// `graph/v1`'s `nodes` is now a plain task list (a node IS one task, see Task 1 of the
// freeform-canvas rework), addressed by declaration `index` like `parallel`'s `tasks` — not the
// retired `{name, tasks, next}` per-lane shape addressed by `nodeIndex`.
describe('insertTaskDispatcherSubtask — graph', () => {
    let unregisterAutoPlacedGraphPositions: (() => void) | undefined;

    afterEach(() => {
        unregisterAutoPlacedGraphPositions?.();

        unregisterAutoPlacedGraphPositions = undefined;
    });

    beforeEach(() => {
        useWorkflowDataStore.setState({nodes: []});

        useWorkflowEditorStore.getState().setGraphPendingConnection(undefined);
    });

    // The real caller (saveWorkflowDefinition.ts) always supplies BOTH `placeholderId` and a
    // `taskDispatcherContext` whose `index` the graph branch of getContextFromPlaceholderNode.ts
    // pins to 0 (the bare `<graphId>-graph-placeholder` id carries no index to parse — see
    // getTaskDispatcherContext.test.ts). Omitting `placeholderId` here would skip the
    // `context.index === 0` correction branch entirely and fall straight into the unconditional-
    // append path below, which would pass even if the add-node insertion path itself prepended —
    // so these reproduce the real shape.
    it('should append a task onto an empty graph via the add-node placeholder', () => {
        const newTask = task({name: 'httpClient_1', type: 'httpClient/v1/get'});

        const updatedTasks = insertTaskDispatcherSubtask({
            newTask,
            placeholderId: 'graph_1-graph-placeholder',
            taskDispatcherContext: {index: 0, taskDispatcherId: 'graph_1'},
            tasks: [graphTask([])],
        });

        expect(updatedTasks[0].parameters?.nodes).toEqual([placedAtFreeSpot(newTask)]);
    });

    it('should append a second task after an existing node, not prepend before it', () => {
        const existingTask = task({name: 'existing_action'});
        const newTask = task({name: 'httpClient_2', type: 'httpClient/v1/get'});

        const updatedTasks = insertTaskDispatcherSubtask({
            newTask,
            placeholderId: 'graph_1-graph-placeholder',
            taskDispatcherContext: {index: 0, taskDispatcherId: 'graph_1'},
            tasks: [graphTask([existingTask])],
        });

        expect(updatedTasks[0].parameters?.nodes).toEqual([existingTask, placedAtFreeSpot(newTask)]);
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
            taskDispatcherContext: {index: 0, taskDispatcherId: 'graph_1'},
            tasks: [graphTask(existingTasks)],
        });

        const nodes = updatedTasks[0].parameters?.nodes as WorkflowTask[];

        expect(nodes).toHaveLength(existingTasks.length + 1);
        expect(nodes[existingTasks.length]).toEqual(placedAtFreeSpot(newTask));
    });

    // `thirdTask` carries a saved position deliberately: without one the generic
    // insert-at-index path's "clear the main axis so dagre can shift what follows" loop has
    // nothing to touch, and this test would read identically whether or not that loop ran.
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

    // Right-clicking a member and pasting hands a numeric declaration index with no
    // `placeholderId` (WorkflowNode.tsx -> getContextFromTaskNodeData), so the insert lands
    // mid-list. A graph member is a free-form frame child that dagre never lays out, so stripping
    // an axis off a following sibling's saved position leaves a half-cleared `{x, y: undefined}`
    // that still reads as pinned downstream — resolveMemberPositions accepts it and
    // toFrameChildPosition turns it into `{x, y: NaN}`, which placeGraphMembers cannot repair.
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

        expect(updatedNestedGraph.parameters?.nodes).toEqual([placedAtFreeSpot(newTask)]);
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
            taskDispatcherContext: {index: 0, taskDispatcherId: 'graph_1'},
            tasks: [graphTask([existingTask])],
        });

        expect((updatedTasks[0].parameters?.nodes as WorkflowTask[])[1].metadata?.ui?.nodePosition).toEqual({
            x: 410,
            y: 220,
        });
        expect(updatedTasks[0].parameters?.transitions).toEqual([{from: 'existing_action', to: 'httpClient_4'}]);
        expect(useWorkflowEditorStore.getState().graphPendingConnection).toBeUndefined();
    });

    // A component dropped into the box raises a pending connection purely to carry the drop
    // position; there is no source member, so it must not grow a transition out of one.
    it('should land a task at the pending drop position without a transition when there is no source', () => {
        const newTask = task({name: 'httpClient_5', type: 'httpClient/v1/get'});

        useWorkflowEditorStore
            .getState()
            .setGraphPendingConnection({dropPosition: {x: 60, y: 90}, from: '', graphId: 'graph_1'});

        const updatedTasks = insertTaskDispatcherSubtask({
            newTask,
            placeholderId: 'graph_1-graph-placeholder',
            taskDispatcherContext: {index: 0, taskDispatcherId: 'graph_1'},
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
            taskDispatcherContext: {index: 0, taskDispatcherId: 'graph_1'},
            tasks: [graphTask([])],
        });

        expect(updatedTasks[0].parameters?.nodes).toEqual([placedAtFreeSpot(newTask)]);
        expect(useWorkflowEditorStore.getState().graphPendingConnection).toEqual(pendingConnection);
    });

    // Adding a node is a first interaction with the graph, so it persists whatever the layout
    // pre-pass had to place itself — otherwise the next pass re-invents those spots around the
    // newcomer and the graph visibly rearranges. Taking from the channel, rather than peeking, is
    // what stops the same flush happening again on the next connect.
    it('should flush the pending auto-placed positions and empty the channel', () => {
        const autoPlacedGraphPositionsRef: AutoPlacedGraphPositionsRefType = {
            current: {graph_1: {existing_action: {x: 300, y: 120}}},
        };

        unregisterAutoPlacedGraphPositions = registerAutoPlacedGraphPositions(autoPlacedGraphPositionsRef);

        const updatedTasks = insertTaskDispatcherSubtask({
            newTask: task({name: 'httpClient_7', type: 'httpClient/v1/get'}),
            placeholderId: 'graph_1-graph-placeholder',
            taskDispatcherContext: {index: 0, taskDispatcherId: 'graph_1'},
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
        expect(updatedTasks[0].parameters?.nodes).toEqual([placedAtFreeSpot(newTask)]);
        expect(updatedTasks[0].parameters?.transitions).toEqual([]);
    });
});
