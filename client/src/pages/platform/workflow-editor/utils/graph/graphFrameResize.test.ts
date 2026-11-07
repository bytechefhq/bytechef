import {GRAPH_FRAME_NODE_TYPE, GRAPH_START_NODE_TYPE} from '@/shared/constants';
import {Node} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import {
    GRAPH_FRAME_MIN_HEIGHT,
    GRAPH_FRAME_MIN_WIDTH,
    GRAPH_MEMBER_BOX_WIDTH,
    getGraphFrameId,
    toFrameChildPosition,
} from './graphFrameGeometry';
import {resizeGraphFrameForMembers} from './graphFrameResize';

const GRAPH_ID = 'graph_1';
const FRAME_ID = getGraphFrameId(GRAPH_ID);

function buildFrameNode(size = {height: GRAPH_FRAME_MIN_HEIGHT, width: GRAPH_FRAME_MIN_WIDTH}): Node {
    return {
        data: {graphFrame: {graphId: GRAPH_ID, height: size.height, width: size.width}, graphId: GRAPH_ID},
        height: size.height,
        id: FRAME_ID,
        position: {x: 0, y: 0},
        type: GRAPH_FRAME_NODE_TYPE,
        width: size.width,
    };
}

interface BuildMemberNodeProps {
    contentPosition: {x: number; y: number};
    measured?: {height: number; width: number};
    name: string;
}

function buildMemberNode({contentPosition, measured, name}: BuildMemberNodeProps): Node {
    return {
        data: {graphData: {graphId: GRAPH_ID, index: 0}, workflowNodeName: name},
        id: name,
        parentId: FRAME_ID,
        position: toFrameChildPosition(contentPosition),
        type: 'workflow',
        ...(measured ? {measured} : {}),
    };
}

describe('resizeGraphFrameForMembers', () => {
    it('grows the frame when a member sits past the current right edge', () => {
        const nodes = [
            buildFrameNode(),
            buildMemberNode({contentPosition: {x: 500, y: 0}, measured: {height: 80, width: 300}, name: 'task_1'}),
        ];

        const resizedNodes = resizeGraphFrameForMembers({graphId: GRAPH_ID, nodes});

        const frameNode = resizedNodes.find((node) => node.id === FRAME_ID)!;

        expect(frameNode.width).toBe(500 + GRAPH_MEMBER_BOX_WIDTH + 500);
        expect(frameNode.data.graphFrame).toEqual({
            graphId: GRAPH_ID,
            height: GRAPH_FRAME_MIN_HEIGHT,
            width: 500 + GRAPH_MEMBER_BOX_WIDTH + 500,
        });
    });

    it('never shrinks the frame below the configured minimum', () => {
        const nodes = [
            buildFrameNode({height: 900, width: 900}),
            buildMemberNode({contentPosition: {x: 0, y: 0}, measured: {height: 40, width: 100}, name: 'task_1'}),
        ];

        const frameNode = resizeGraphFrameForMembers({graphId: GRAPH_ID, nodes}).find((node) => node.id === FRAME_ID)!;

        expect(frameNode.width).toBe(GRAPH_FRAME_MIN_WIDTH);
        expect(frameNode.height).toBe(GRAPH_FRAME_MIN_HEIGHT);
    });

    it('falls back to the nominal member size for a member React Flow has not measured yet', () => {
        const nodes = [buildFrameNode(), buildMemberNode({contentPosition: {x: 500, y: 400}, name: 'task_1'})];

        const frameNode = resizeGraphFrameForMembers({graphId: GRAPH_ID, nodes}).find((node) => node.id === FRAME_ID)!;

        expect(frameNode.width).toBe(500 + GRAPH_MEMBER_BOX_WIDTH + 500);
    });

    it('grows the frame around a dispatcher member subtree, not just the member node', () => {
        const subtreeNode: Node = {
            data: {loopData: {index: 0, loopId: 'loop_1'}, workflowNodeName: 'inner_1'},
            id: 'inner_1',
            measured: {height: 80, width: 200},
            parentId: FRAME_ID,
            position: toFrameChildPosition({x: 600, y: 120}),
            type: 'workflow',
        };

        const nodes = [
            buildFrameNode(),
            {
                ...buildMemberNode({contentPosition: {x: 0, y: 0}, measured: {height: 80, width: 100}, name: 'loop_1'}),
                data: {
                    graphData: {graphId: GRAPH_ID, index: 0},
                    taskDispatcher: true,
                    workflowNodeName: 'loop_1',
                },
            },
            subtreeNode,
        ];

        const frameNode = resizeGraphFrameForMembers({graphId: GRAPH_ID, nodes}).find((node) => node.id === FRAME_ID)!;

        expect(frameNode.width).toBe(600 + 200 + 72);
    });

    it('ignores the frame chrome, so a graph with no members stays at the minimum size', () => {
        const startPillNode: Node = {
            data: {graphId: GRAPH_ID, graphStart: {graphId: GRAPH_ID}, taskDispatcherId: GRAPH_ID},
            height: 32,
            id: `${GRAPH_ID}-graph-start`,
            parentId: FRAME_ID,
            position: toFrameChildPosition({x: 900, y: 900}),
            type: GRAPH_START_NODE_TYPE,
            width: 72,
        };

        const placeholderNode: Node = {
            data: {graphId: GRAPH_ID, label: '+', taskDispatcherId: GRAPH_ID},
            id: `${GRAPH_ID}-graph-placeholder`,
            parentId: FRAME_ID,
            position: toFrameChildPosition({x: 900, y: 900}),
            type: 'placeholder',
        };

        const frameNode = resizeGraphFrameForMembers({
            graphId: GRAPH_ID,
            nodes: [buildFrameNode(), startPillNode, placeholderNode],
        }).find((node) => node.id === FRAME_ID)!;

        expect(frameNode.width).toBe(GRAPH_FRAME_MIN_WIDTH);
        expect(frameNode.height).toBe(GRAPH_FRAME_MIN_HEIGHT);
    });

    it('returns the very same array when the frame is already the right size', () => {
        const nodes = [
            buildFrameNode({height: GRAPH_FRAME_MIN_HEIGHT, width: 500 + GRAPH_MEMBER_BOX_WIDTH + 500}),
            buildMemberNode({contentPosition: {x: 500, y: 0}, measured: {height: 80, width: 300}, name: 'task_1'}),
        ];

        expect(resizeGraphFrameForMembers({graphId: GRAPH_ID, nodes})).toBe(nodes);
    });

    it('returns the very same array when the graph has no frame node', () => {
        const nodes = [
            buildMemberNode({contentPosition: {x: 500, y: 0}, measured: {height: 80, width: 300}, name: 'task_1'}),
        ];

        expect(resizeGraphFrameForMembers({graphId: GRAPH_ID, nodes})).toBe(nodes);
    });
});
