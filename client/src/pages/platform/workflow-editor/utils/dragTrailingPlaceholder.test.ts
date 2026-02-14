import {Edge, Node} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import {
    DraggingPlaceholderStateType,
    buildDraggingPlaceholderState,
    computePlaceholderDragPosition,
} from './dragTrailingPlaceholder';

const TRAILING_PLACEHOLDER_ID = 'trailing-placeholder';

function makeNode(id: string, position = {x: 0, y: 0}): Node {
    return {data: {}, id, position, type: 'workflow'};
}

function makeEdge(source: string, target: string): Edge {
    return {id: `${source}=>${target}`, source, target, type: 'workflow'};
}

describe('buildDraggingPlaceholderState', () => {
    it('returns state when dragged node is the direct predecessor', () => {
        const draggedNode = makeNode('accelo_1', {x: 100, y: 200});
        const placeholder = makeNode(TRAILING_PLACEHOLDER_ID, {x: 100, y: 370});
        const edges = [makeEdge('accelo_1', TRAILING_PLACEHOLDER_ID)];
        const nodes = [draggedNode, placeholder];

        const result = buildDraggingPlaceholderState(
            draggedNode,
            false,
            TRAILING_PLACEHOLDER_ID,
            edges,
            nodes,
            new Map()
        );

        expect(result).toEqual({
            nodeId: 'accelo_1',
            nodeStartPosition: {x: 100, y: 200},
            placeholderStartPosition: {x: 100, y: 370},
        });
    });

    it('returns state when dragged dispatcher has a child as predecessor', () => {
        const dispatcher = makeNode('loop_1', {x: 100, y: 200});
        const bottomGhost = makeNode('loop_1-bottom-ghost', {x: 100, y: 500});
        const placeholder = makeNode(TRAILING_PLACEHOLDER_ID, {x: 100, y: 670});
        const edges = [makeEdge('loop_1-bottom-ghost', TRAILING_PLACEHOLDER_ID)];
        const nodes = [dispatcher, bottomGhost, placeholder];
        const childIds = new Map([['loop_1-bottom-ghost', {x: 100, y: 500}]]);

        const result = buildDraggingPlaceholderState(dispatcher, true, TRAILING_PLACEHOLDER_ID, edges, nodes, childIds);

        expect(result).toEqual({
            nodeId: 'loop_1',
            nodeStartPosition: {x: 100, y: 200},
            placeholderStartPosition: {x: 100, y: 670},
        });
    });

    it('returns null when dragged node is not the predecessor', () => {
        const draggedNode = makeNode('accelo_1', {x: 100, y: 200});
        const lastNode = makeNode('httpClient_1', {x: 100, y: 370});
        const placeholder = makeNode(TRAILING_PLACEHOLDER_ID, {x: 100, y: 540});
        const edges = [makeEdge('accelo_1', 'httpClient_1'), makeEdge('httpClient_1', TRAILING_PLACEHOLDER_ID)];
        const nodes = [draggedNode, lastNode, placeholder];

        const result = buildDraggingPlaceholderState(
            draggedNode,
            false,
            TRAILING_PLACEHOLDER_ID,
            edges,
            nodes,
            new Map()
        );

        expect(result).toBeNull();
    });

    it('returns null when no edge targets the trailing placeholder', () => {
        const draggedNode = makeNode('accelo_1', {x: 100, y: 200});
        const nodes = [draggedNode];
        const edges: Edge[] = [];

        const result = buildDraggingPlaceholderState(
            draggedNode,
            false,
            TRAILING_PLACEHOLDER_ID,
            edges,
            nodes,
            new Map()
        );

        expect(result).toBeNull();
    });

    it('returns null when trailing placeholder node is missing', () => {
        const draggedNode = makeNode('accelo_1', {x: 100, y: 200});
        const edges = [makeEdge('accelo_1', TRAILING_PLACEHOLDER_ID)];
        const nodes = [draggedNode]; // placeholder not in nodes

        const result = buildDraggingPlaceholderState(
            draggedNode,
            false,
            TRAILING_PLACEHOLDER_ID,
            edges,
            nodes,
            new Map()
        );

        expect(result).toBeNull();
    });

    it('returns null when dispatcher child is predecessor but node is not a dispatcher', () => {
        const regularNode = makeNode('accelo_1', {x: 100, y: 200});
        const bottomGhost = makeNode('loop_1-bottom-ghost', {x: 100, y: 500});
        const placeholder = makeNode(TRAILING_PLACEHOLDER_ID, {x: 100, y: 670});
        const edges = [makeEdge('loop_1-bottom-ghost', TRAILING_PLACEHOLDER_ID)];
        const nodes = [regularNode, bottomGhost, placeholder];
        const childIds = new Map([['loop_1-bottom-ghost', {x: 100, y: 500}]]);

        const result = buildDraggingPlaceholderState(
            regularNode,
            false,
            TRAILING_PLACEHOLDER_ID,
            edges,
            nodes,
            childIds
        );

        expect(result).toBeNull();
    });
});

describe('computePlaceholderDragPosition', () => {
    it('computes position from positive delta in TB mode', () => {
        const state: DraggingPlaceholderStateType = {
            nodeId: 'accelo_1',
            nodeStartPosition: {x: 100, y: 200},
            placeholderStartPosition: {x: 100, y: 370},
        };

        const result = computePlaceholderDragPosition(state, {x: 100, y: 280});

        expect(result).toEqual({x: 100, y: 450});
    });

    it('computes position from negative delta in TB mode', () => {
        const state: DraggingPlaceholderStateType = {
            nodeId: 'accelo_1',
            nodeStartPosition: {x: 100, y: 200},
            placeholderStartPosition: {x: 100, y: 370},
        };

        const result = computePlaceholderDragPosition(state, {x: 100, y: 150});

        expect(result).toEqual({x: 100, y: 320});
    });

    it('computes position from delta in LR mode', () => {
        const state: DraggingPlaceholderStateType = {
            nodeId: 'loop_1',
            nodeStartPosition: {x: 300, y: 400},
            placeholderStartPosition: {x: 600, y: 400},
        };

        const result = computePlaceholderDragPosition(state, {x: 380, y: 420});

        expect(result).toEqual({x: 680, y: 420});
    });

    it('returns same position when delta is zero', () => {
        const state: DraggingPlaceholderStateType = {
            nodeId: 'accelo_1',
            nodeStartPosition: {x: 100, y: 200},
            placeholderStartPosition: {x: 100, y: 370},
        };

        const result = computePlaceholderDragPosition(state, {x: 100, y: 200});

        expect(result).toEqual({x: 100, y: 370});
    });
});
