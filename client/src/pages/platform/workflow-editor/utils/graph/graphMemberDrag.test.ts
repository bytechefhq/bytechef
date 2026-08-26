import {Node, XYPosition} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import {getGraphFrameId, toFrameChildPosition} from './graphFrameGeometry';
import {buildGraphMemberDragStopPositions, filterToSharedParent} from './graphMemberDrag';

const FRAME_ID = getGraphFrameId('graph_1');

describe('buildGraphMemberDragStopPositions', () => {
    it('converts the dropped frame-relative position back to content coordinates', () => {
        expect(
            buildGraphMemberDragStopPositions({
                draggedNodeId: 'task_1',
                draggedNodePosition: toFrameChildPosition({x: 120, y: 100}),
            })
        ).toEqual({task_1: {x: 120, y: 100}});
    });

    it('clamps the content position, not the frame-relative one, to non-negative', () => {
        // Frame-relative y = 10 is INSIDE the header band, i.e. content y = -30. Clamping before
        // the conversion would persist -30; clamping after it persists 0. `GRAPH_MEMBER_EXTENT`
        // means a dragged member cannot reach this position today, so the case is defensive — it
        // pins the order that survives a loosened extent.
        expect(
            buildGraphMemberDragStopPositions({
                draggedNodeId: 'task_1',
                draggedNodePosition: {x: -30, y: 10},
            })
        ).toEqual({task_1: {x: 0, y: 0}});
    });

    it('flushes the auto-placed siblings alongside the dragged member', () => {
        expect(
            buildGraphMemberDragStopPositions({
                autoPlacedPositions: {task_2: {x: 300, y: 0}, task_3: {x: 600, y: 0}},
                draggedNodeId: 'task_1',
                draggedNodePosition: toFrameChildPosition({x: 40, y: 20}),
            })
        ).toEqual({
            task_1: {x: 40, y: 20},
            task_2: {x: 300, y: 0},
            task_3: {x: 600, y: 0},
        });
    });

    it('lets the dropped position win over a stale auto-placed entry for the same member', () => {
        expect(
            buildGraphMemberDragStopPositions({
                autoPlacedPositions: {task_1: {x: 999, y: 999}},
                draggedNodeId: 'task_1',
                draggedNodePosition: toFrameChildPosition({x: 40, y: 20}),
            })
        ).toEqual({task_1: {x: 40, y: 20}});
    });
});

describe('filterToSharedParent', () => {
    const nodes: Node[] = [
        {data: {}, id: 'graph_1', position: {x: 0, y: 0}},
        {data: {}, id: FRAME_ID, position: {x: 0, y: 0}},
        {data: {}, id: 'task_1', parentId: FRAME_ID, position: {x: 0, y: 40}},
        {data: {}, id: 'inner_1', parentId: FRAME_ID, position: {x: 0, y: 140}},
        {data: {}, id: 'outer_1', position: {x: 0, y: 300}},
    ];

    it('drops frame children when the dragged node lives outside the frame', () => {
        const startPositions = new Map<string, XYPosition>([
            [FRAME_ID, {x: 0, y: 0}],
            ['task_1', {x: 0, y: 40}],
            ['outer_1', {x: 0, y: 300}],
        ]);

        expect([...filterToSharedParent(startPositions, nodes, undefined).keys()]).toEqual([FRAME_ID, 'outer_1']);
    });

    it('keeps the subtree of a dragged member, which shares its frame parent', () => {
        const startPositions = new Map<string, XYPosition>([
            ['inner_1', {x: 0, y: 140}],
            ['outer_1', {x: 0, y: 300}],
        ]);

        expect([...filterToSharedParent(startPositions, nodes, FRAME_ID).keys()]).toEqual(['inner_1']);
    });

    it('drops an entry whose node has since disappeared from the canvas', () => {
        const startPositions = new Map<string, XYPosition>([['gone_1', {x: 0, y: 0}]]);

        expect(filterToSharedParent(startPositions, nodes, undefined).size).toBe(0);
    });
});
