import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {Node} from '@xyflow/react';
import {afterEach, describe, expect, it} from 'vitest';

import {AutoPlacedGraphPositionsRefType, registerAutoPlacedGraphPositions} from './autoPlacedGraphPositions';
import {toFrameChildPosition} from './graphFrameGeometry';
import {applyGraphMemberInsertion} from './graphMemberInsertion';

const GRAPH_ID = 'graph_1';

function buildMemberTask(name: string, contentPosition?: {x: number; y: number}): WorkflowTask {
    return {
        name,
        type: 'mailchimp/v1/subscribe',
        ...(contentPosition ? {metadata: {ui: {nodePosition: contentPosition}}} : {}),
    };
}

function buildGraphTask(members: WorkflowTask[], transitions: Array<{from: string; to: string}> = []): WorkflowTask {
    return {
        name: GRAPH_ID,
        parameters: {maxTransitions: 100, nodes: members, transitions},
        type: 'graph/v1',
    };
}

function buildMemberNode(name: string, contentPosition: {x: number; y: number}): Node {
    return {
        data: {graphData: {graphId: GRAPH_ID, index: 0}, name},
        id: name,
        measured: {height: 72, width: 72},
        parentId: `${GRAPH_ID}-graph-frame`,
        position: toFrameChildPosition(contentPosition),
        type: 'workflow',
    };
}

function getMemberPosition(graphTask: WorkflowTask, memberName: string) {
    const members = graphTask.parameters?.nodes as Array<WorkflowTask>;

    return members.find((member) => member.name === memberName)?.metadata?.ui?.nodePosition;
}

describe('applyGraphMemberInsertion', () => {
    let unregisterAutoPlacedPositions: (() => void) | undefined;

    afterEach(() => {
        unregisterAutoPlacedPositions?.();

        unregisterAutoPlacedPositions = undefined;
    });

    it('places the added member at the drop position and connects it from the pending source', () => {
        const previousGraphTask = buildGraphTask([buildMemberTask('task_1', {x: 0, y: 0})]);
        const updatedGraphTask = buildGraphTask([buildMemberTask('task_1', {x: 0, y: 0}), buildMemberTask('task_2')]);

        const insertedGraphTask = applyGraphMemberInsertion({
            nodes: [buildMemberNode('task_1', {x: 0, y: 0})],
            pendingConnection: {dropPosition: {x: 200, y: 160}, from: 'task_1', graphId: GRAPH_ID},
            previousGraphTask,
            updatedGraphTask,
        });

        expect(getMemberPosition(insertedGraphTask, 'task_2')).toEqual({x: 200, y: 160});
        expect(insertedGraphTask.parameters?.transitions).toEqual([{from: 'task_1', to: 'task_2'}]);
    });

    it('adds no transition when the pending connection has no source node', () => {
        const previousGraphTask = buildGraphTask([buildMemberTask('task_1', {x: 0, y: 0})]);
        const updatedGraphTask = buildGraphTask([buildMemberTask('task_1', {x: 0, y: 0}), buildMemberTask('task_2')]);

        const insertedGraphTask = applyGraphMemberInsertion({
            nodes: [buildMemberNode('task_1', {x: 0, y: 0})],
            pendingConnection: {dropPosition: {x: 40, y: 300}, from: '', graphId: GRAPH_ID},
            previousGraphTask,
            updatedGraphTask,
        });

        expect(getMemberPosition(insertedGraphTask, 'task_2')).toEqual({x: 40, y: 300});
        expect(insertedGraphTask.parameters?.transitions).toEqual([]);
    });

    it('does not duplicate a transition the graph already declares', () => {
        const existingTransitions = [{from: 'task_1', to: 'task_2'}];

        const previousGraphTask = buildGraphTask([buildMemberTask('task_1', {x: 0, y: 0})], existingTransitions);
        const updatedGraphTask = buildGraphTask(
            [buildMemberTask('task_1', {x: 0, y: 0}), buildMemberTask('task_2')],
            existingTransitions
        );

        const insertedGraphTask = applyGraphMemberInsertion({
            nodes: [],
            pendingConnection: {dropPosition: {x: 0, y: 200}, from: 'task_1', graphId: GRAPH_ID},
            previousGraphTask,
            updatedGraphTask,
        });

        expect(insertedGraphTask.parameters?.transitions).toEqual(existingTransitions);
    });

    it('finds a free spot past the existing members when nothing was dropped at a position', () => {
        const previousGraphTask = buildGraphTask([buildMemberTask('task_1', {x: 0, y: 0})]);
        const updatedGraphTask = buildGraphTask([buildMemberTask('task_1', {x: 0, y: 0}), buildMemberTask('task_2')]);

        const insertedGraphTask = applyGraphMemberInsertion({
            nodes: [buildMemberNode('task_1', {x: 0, y: 0})],
            previousGraphTask,
            updatedGraphTask,
        });

        const addedMemberPosition = getMemberPosition(insertedGraphTask, 'task_2');

        expect(addedMemberPosition).toBeDefined();
        expect(addedMemberPosition!.y).toBeGreaterThan(0);
        expect(getMemberPosition(insertedGraphTask, 'task_1')).toEqual({x: 0, y: 0});
    });

    it('pins auto-placed members that have no saved position and consumes the registered positions', () => {
        const autoPlacedPositionsRef: AutoPlacedGraphPositionsRefType = {
            current: {[GRAPH_ID]: {task_1: {x: 24, y: 48}}},
        };

        unregisterAutoPlacedPositions = registerAutoPlacedGraphPositions(autoPlacedPositionsRef);

        const previousGraphTask = buildGraphTask([buildMemberTask('task_1')]);
        const updatedGraphTask = buildGraphTask([buildMemberTask('task_1'), buildMemberTask('task_2')]);

        const insertedGraphTask = applyGraphMemberInsertion({
            nodes: [],
            pendingConnection: {dropPosition: {x: 300, y: 48}, from: 'task_1', graphId: GRAPH_ID},
            previousGraphTask,
            updatedGraphTask,
        });

        expect(getMemberPosition(insertedGraphTask, 'task_1')).toEqual({x: 24, y: 48});
        expect(getMemberPosition(insertedGraphTask, 'task_2')).toEqual({x: 300, y: 48});
        expect(autoPlacedPositionsRef.current[GRAPH_ID]).toBeUndefined();
    });

    it('keeps every other graph parameter in place', () => {
        const previousGraphTask = buildGraphTask([]);
        const updatedGraphTask = buildGraphTask([buildMemberTask('task_1')]);

        const insertedGraphTask = applyGraphMemberInsertion({
            nodes: [],
            pendingConnection: {dropPosition: {x: 0, y: 0}, from: '', graphId: GRAPH_ID},
            previousGraphTask,
            updatedGraphTask,
        });

        expect(insertedGraphTask.name).toBe(GRAPH_ID);
        expect(insertedGraphTask.parameters?.maxTransitions).toBe(100);
    });
});
