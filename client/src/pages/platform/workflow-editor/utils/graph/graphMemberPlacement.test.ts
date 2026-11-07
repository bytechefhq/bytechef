import {Node} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import {
    GRAPH_MEMBER_BOX_WIDTH,
    GRAPH_MEMBER_LAYER_SPACING,
    GRAPH_MEMBER_NOMINAL_SIZE,
    toFrameChildPosition,
} from './graphFrameGeometry';
import {collectGraphMemberSizes, placeGraphMembers, readGraphMemberCanvasState} from './graphMemberPlacement';

import type {WorkflowTask} from '@/shared/middleware/platform/configuration';

interface BuildMemberNodeProps {
    contentPosition: {x: number; y: number};
    measured?: Node['measured'];
    name: string;
}

function buildMemberNode({contentPosition, measured, name}: BuildMemberNodeProps): Node {
    return {
        data: {graphData: {graphId: 'graph_1', index: 0}, name},
        id: name,
        measured,
        parentId: 'graph_1-graph-frame',
        position: toFrameChildPosition(contentPosition),
        type: 'workflow',
    };
}

interface BuildMemberChildNodeProps {
    contentPosition: {x: number; y: number};
    measured?: Node['measured'];
    name: string;
    ownerName: string;
}

function buildMemberChildNode({contentPosition, measured, name, ownerName}: BuildMemberChildNodeProps): Node {
    return {
        data: {loopData: {index: 0, loopId: ownerName}, name},
        id: name,
        measured,
        parentId: 'graph_1-graph-frame',
        position: toFrameChildPosition(contentPosition),
        type: 'workflow',
    };
}

function buildMemberTask(name: string, contentPosition?: {x: number; y: number}): WorkflowTask {
    return {
        name,
        type: 'mailchimp/v1/subscribe',
        ...(contentPosition ? {metadata: {ui: {nodePosition: contentPosition}}} : {}),
    };
}

describe('readGraphMemberCanvasState', () => {
    it('reads member positions back in content coordinates and skips other graphs', () => {
        const otherGraphNode: Node = {
            data: {graphData: {graphId: 'graph_2', index: 0}, name: 'other_1'},
            id: 'other_1',
            position: {x: 0, y: 0},
            type: 'workflow',
        };

        const canvasState = readGraphMemberCanvasState('graph_1', [
            buildMemberNode({contentPosition: {x: 24, y: 0}, measured: {height: 72, width: 240}, name: 'task_1'}),
            buildMemberNode({contentPosition: {x: 300, y: 120}, name: 'task_2'}),
            otherGraphNode,
        ]);

        expect(canvasState.positions).toEqual({task_1: {x: 24, y: 0}, task_2: {x: 300, y: 120}});
        expect(canvasState.sizes).toEqual({
            task_1: {height: 72, paintedWidth: GRAPH_MEMBER_BOX_WIDTH, width: 240},
            task_2: {...GRAPH_MEMBER_NOMINAL_SIZE, paintedWidth: GRAPH_MEMBER_BOX_WIDTH},
        });
    });
});

describe('collectGraphMemberSizes', () => {
    it('names every member and places it by its painted box, not its label width', () => {
        expect(
            collectGraphMemberSizes('graph_1', [
                buildMemberNode({contentPosition: {x: 0, y: 0}, measured: {height: 72, width: 240}, name: 'task_1'}),
                buildMemberNode({contentPosition: {x: 0, y: 0}, name: 'task_2'}),
            ])
        ).toEqual([
            {height: 72, labelOverhang: 240 - GRAPH_MEMBER_BOX_WIDTH, name: 'task_1', width: GRAPH_MEMBER_BOX_WIDTH},
            {
                height: GRAPH_MEMBER_NOMINAL_SIZE.height,
                labelOverhang: GRAPH_MEMBER_NOMINAL_SIZE.width - GRAPH_MEMBER_BOX_WIDTH,
                name: 'task_2',
                width: GRAPH_MEMBER_BOX_WIDTH,
            },
        ]);
    });

    it('measures a dispatcher member over the whole subtree it owns', () => {
        expect(
            collectGraphMemberSizes('graph_1', [
                buildMemberNode({contentPosition: {x: 0, y: 0}, measured: {height: 100, width: 240}, name: 'loop_1'}),
                buildMemberChildNode({
                    contentPosition: {x: 20, y: 140},
                    measured: {height: 80, width: 200},
                    name: 'task_1',
                    ownerName: 'loop_1',
                }),
                buildMemberNode({contentPosition: {x: 400, y: 0}, measured: {height: 72, width: 240}, name: 'task_2'}),
            ])
        ).toEqual([
            {height: 220, labelOverhang: 0, name: 'loop_1', width: 240},
            {height: 72, labelOverhang: 240 - GRAPH_MEMBER_BOX_WIDTH, name: 'task_2', width: GRAPH_MEMBER_BOX_WIDTH},
        ]);
    });
});

describe('placeGraphMembers', () => {
    const CANVAS_STATE = {
        positions: {task_1: {x: 24, y: 0}, task_2: {x: 300, y: 0}},
        sizes: {
            task_1: {height: 72, paintedWidth: GRAPH_MEMBER_BOX_WIDTH, width: 240},
            task_2: {height: 72, paintedWidth: GRAPH_MEMBER_BOX_WIDTH, width: 240},
        },
    };

    it('pins a newly appended member at the drop position', () => {
        const {addedMemberName, members} = placeGraphMembers({
            canvasState: CANVAS_STATE,
            dropPosition: {x: 410, y: 220},
            previousMembers: [buildMemberTask('task_1', {x: 24, y: 0})],
            updatedMembers: [buildMemberTask('task_1', {x: 24, y: 0}), buildMemberTask('task_3')],
        });

        expect(addedMemberName).toBe('task_3');
        expect(members[1].metadata?.ui?.nodePosition).toEqual({x: 410, y: 220});
    });

    it('puts a member added from the header one layer below the last one in TB', () => {
        const {members} = placeGraphMembers({
            canvasState: CANVAS_STATE,
            direction: 'TB',
            previousMembers: [buildMemberTask('task_1', {x: 24, y: 0}), buildMemberTask('task_2', {x: 300, y: 0})],
            updatedMembers: [
                buildMemberTask('task_1', {x: 24, y: 0}),
                buildMemberTask('task_2', {x: 300, y: 0}),
                buildMemberTask('task_3'),
            ],
        });

        expect(members[2].metadata?.ui?.nodePosition).toEqual({x: 24, y: 72 + GRAPH_MEMBER_LAYER_SPACING});
    });

    it('puts a member added from the header one layer right of the last one in LR', () => {
        const {members} = placeGraphMembers({
            canvasState: CANVAS_STATE,
            direction: 'LR',
            previousMembers: [buildMemberTask('task_1', {x: 24, y: 0}), buildMemberTask('task_2', {x: 300, y: 0})],
            updatedMembers: [
                buildMemberTask('task_1', {x: 24, y: 0}),
                buildMemberTask('task_2', {x: 300, y: 0}),
                buildMemberTask('task_3'),
            ],
        });

        expect(members[2].metadata?.ui?.nodePosition).toEqual({
            x: 300 + GRAPH_MEMBER_BOX_WIDTH + GRAPH_MEMBER_LAYER_SPACING,
            y: 0,
        });
    });

    it('leaves the first member of an empty graph to auto-placement', () => {
        const {addedMemberName, members} = placeGraphMembers({
            canvasState: {positions: {}, sizes: {}},
            direction: 'LR',
            previousMembers: [],
            updatedMembers: [buildMemberTask('task_1')],
        });

        expect(addedMemberName).toBe('task_1');
        expect(members[0].metadata?.ui?.nodePosition).toBeUndefined();
    });

    it('flushes the pending auto-placed position of a member that carries none', () => {
        const {members} = placeGraphMembers({
            autoPlacedPositions: {task_1: {x: 24, y: 0}},
            canvasState: CANVAS_STATE,
            previousMembers: [buildMemberTask('task_1'), buildMemberTask('task_2', {x: 300, y: 0})],
            updatedMembers: [
                buildMemberTask('task_1'),
                buildMemberTask('task_2', {x: 300, y: 0}),
                buildMemberTask('task_3'),
            ],
        });

        expect(members[0].metadata?.ui?.nodePosition).toEqual({x: 24, y: 0});
    });

    it('leaves a member with neither a stored nor a pending position unplaced', () => {
        const {members} = placeGraphMembers({
            canvasState: CANVAS_STATE,
            previousMembers: [buildMemberTask('task_1')],
            updatedMembers: [buildMemberTask('task_1'), buildMemberTask('task_3')],
        });

        expect(members[0].metadata?.ui?.nodePosition).toBeUndefined();
    });

    it('keeps clear of an unplaced member that the canvas already shows', () => {
        const {members} = placeGraphMembers({
            canvasState: CANVAS_STATE,
            previousMembers: [buildMemberTask('task_1'), buildMemberTask('task_2')],
            updatedMembers: [buildMemberTask('task_1'), buildMemberTask('task_2'), buildMemberTask('task_3')],
        });

        expect(members[2].metadata?.ui?.nodePosition).toEqual({x: 24, y: 72 + GRAPH_MEMBER_LAYER_SPACING});
    });

    it('places a newcomer clear of a dispatcher member SUBTREE, not just of its node box', () => {
        const canvasState = readGraphMemberCanvasState('graph_1', [
            buildMemberNode({contentPosition: {x: 0, y: 0}, measured: {height: 100, width: 240}, name: 'condition_1'}),
            buildMemberChildNode({
                contentPosition: {x: 0, y: 140},
                measured: {height: 80, width: 200},
                name: 'task_1',
                ownerName: 'condition_1',
            }),
            buildMemberChildNode({
                contentPosition: {x: 300, y: 140},
                measured: {height: 80, width: 200},
                name: 'task_2',
                ownerName: 'condition_1',
            }),
        ]);

        const placeIn = (direction: 'LR' | 'TB') =>
            placeGraphMembers({
                canvasState,
                direction,
                previousMembers: [buildMemberTask('condition_1', {x: 0, y: 0})],
                updatedMembers: [buildMemberTask('condition_1', {x: 0, y: 0}), buildMemberTask('task_3')],
            }).members[1].metadata?.ui?.nodePosition;

        expect(placeIn('TB')).toEqual({x: 0, y: 220 + GRAPH_MEMBER_LAYER_SPACING});
        expect(placeIn('LR')).toEqual({x: 500 + GRAPH_MEMBER_LAYER_SPACING, y: 0});
    });

    it('leaves an already positioned member untouched', () => {
        const previousMembers = [buildMemberTask('task_1', {x: 11, y: 22})];

        const {members} = placeGraphMembers({
            canvasState: CANVAS_STATE,
            previousMembers,
            updatedMembers: [...previousMembers, buildMemberTask('task_3')],
        });

        expect(members[0]).toBe(previousMembers[0]);
    });

    it('reports no added member and stamps nothing new when the list only shrank', () => {
        const previousMembers = [buildMemberTask('task_1', {x: 24, y: 0}), buildMemberTask('task_2', {x: 300, y: 0})];

        const {addedMemberName, members} = placeGraphMembers({
            canvasState: CANVAS_STATE,
            previousMembers,
            updatedMembers: [previousMembers[0]],
        });

        expect(addedMemberName).toBeUndefined();
        expect(members).toEqual([previousMembers[0]]);
    });
});

describe('getMemberSize via collectGraphMemberSizes', () => {
    it('prefers a cluster root box over its measured card size', () => {
        const nodes: Node[] = [
            {
                data: {
                    clusterFrame: {clusterRootId: 'aiAgent_1', height: 320, width: 640},
                    clusterRoot: true,
                    graphData: {graphId: 'graph_1', index: 0},
                    workflowNodeName: 'aiAgent_1',
                },
                id: 'aiAgent_1',
                measured: {height: 80, width: 240},
                parentId: 'graph_1-graph-frame',
                position: {x: 0, y: 0},
                type: 'workflow',
            },
        ];

        expect(collectGraphMemberSizes('graph_1', nodes)).toEqual([
            {height: 320, labelOverhang: 0, name: 'aiAgent_1', width: 640},
        ]);
    });

    it('falls back to the measured size when no pre-pass box is present', () => {
        const nodes: Node[] = [
            {
                data: {graphData: {graphId: 'graph_1', index: 0}, workflowNodeName: 'task_1'},
                id: 'task_1',
                measured: {height: 80, width: 240},
                parentId: 'graph_1-graph-frame',
                position: {x: 0, y: 0},
                type: 'workflow',
            },
        ];

        expect(collectGraphMemberSizes('graph_1', nodes)).toEqual([
            {height: 80, labelOverhang: 240 - GRAPH_MEMBER_BOX_WIDTH, name: 'task_1', width: GRAPH_MEMBER_BOX_WIDTH},
        ]);
    });
});
