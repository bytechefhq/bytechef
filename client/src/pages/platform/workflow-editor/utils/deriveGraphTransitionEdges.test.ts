import {GraphNodeType} from '@/shared/types';
import {describe, expect, it} from 'vitest';

import deriveGraphTransitionEdges from './deriveGraphTransitionEdges';

function graphNode(name: string, next?: string): GraphNodeType {
    return {name, next, tasks: []};
}

describe('deriveGraphTransitionEdges', () => {
    it('should return an empty array for an empty graph', () => {
        expect(deriveGraphTransitionEdges([])).toEqual([]);
    });

    it('should derive a forward edge and a back edge for a cyclic pair of nodes', () => {
        const nodes = [graphNode('nodeA', "'nodeB'"), graphNode('nodeB', "'nodeA'")];

        expect(deriveGraphTransitionEdges(nodes)).toEqual([
            {kind: 'forward', sourceIndex: 0, targetIndex: 1},
            {kind: 'back', sourceIndex: 1, targetIndex: 0},
        ]);
    });

    it('should derive a self edge when a node targets itself', () => {
        const nodes = [graphNode('nodeA', "'nodeA'")];

        expect(deriveGraphTransitionEdges(nodes)).toEqual([{kind: 'self', sourceIndex: 0, targetIndex: 0}]);
    });

    it('should derive one edge per target of a multi-target ternary', () => {
        const nodes = [
            graphNode('nodeA', "steps.decision.value == 'yes' ? 'nodeB' : 'nodeC'"),
            graphNode('nodeB'),
            graphNode('nodeC'),
        ];

        expect(deriveGraphTransitionEdges(nodes)).toEqual([
            {kind: 'forward', sourceIndex: 0, targetIndex: 1},
            {kind: 'forward', sourceIndex: 0, targetIndex: 2},
        ]);
    });

    it('should derive no edges for a node whose next expression is fully dynamic', () => {
        const nodes = [graphNode('nodeA', 'steps.decision.nextNode'), graphNode('nodeB')];

        expect(deriveGraphTransitionEdges(nodes)).toEqual([]);
    });

    it('should derive no edge for a dangling literal target — it stays badge-only', () => {
        const nodes = [graphNode('nodeA', "'missingNode'")];

        expect(deriveGraphTransitionEdges(nodes)).toEqual([]);
    });

    it('should derive no edges for a terminal node with no next expression', () => {
        const nodes = [graphNode('nodeA'), graphNode('nodeB')];

        expect(deriveGraphTransitionEdges(nodes)).toEqual([]);
    });

    it('should derive a mix of forward, back, and self edges across a larger graph', () => {
        const nodes = [
            graphNode('nodeA', "'nodeB'"),
            graphNode('nodeB', "'nodeC'"),
            graphNode('nodeC', "'nodeA'"),
            graphNode('nodeD', "'nodeD'"),
        ];

        expect(deriveGraphTransitionEdges(nodes)).toEqual([
            {kind: 'forward', sourceIndex: 0, targetIndex: 1},
            {kind: 'forward', sourceIndex: 1, targetIndex: 2},
            {kind: 'back', sourceIndex: 2, targetIndex: 0},
            {kind: 'self', sourceIndex: 3, targetIndex: 3},
        ]);
    });

    it('should derive edge kind from visual positions when a permutation is supplied', () => {
        const nodes: Array<GraphNodeType> = [
            {name: 'node_0', next: "'node_3'", tasks: []},
            {name: 'node_1', next: "'node_0'", tasks: []},
            {name: 'node_2', next: "'node_1'", tasks: []},
            {name: 'node_3', next: "'node_2'", tasks: []},
        ];

        // Visual order is node_0, node_3, node_2, node_1
        const visualPositionByIndex = new Map([
            [0, 0],
            [3, 1],
            [2, 2],
            [1, 3],
        ]);

        const edges = deriveGraphTransitionEdges(nodes, visualPositionByIndex);

        const kindOf = (sourceIndex: number, targetIndex: number): string =>
            edges.find((edge) => edge.sourceIndex === sourceIndex && edge.targetIndex === targetIndex)!.kind;

        // Three adjacent forward hops along the visual order, one return arc
        expect(kindOf(0, 3)).toBe('forward');
        expect(kindOf(3, 2)).toBe('forward');
        expect(kindOf(2, 1)).toBe('forward');
        expect(kindOf(1, 0)).toBe('back');
    });

    it('should keep declaration-index kinds when no permutation is supplied', () => {
        const nodes: Array<GraphNodeType> = [
            {name: 'node_0', next: "'node_3'", tasks: []},
            {name: 'node_1', next: "'node_0'", tasks: []},
            {name: 'node_2', next: "'node_1'", tasks: []},
            {name: 'node_3', next: "'node_2'", tasks: []},
        ];

        const edges = deriveGraphTransitionEdges(nodes);

        const kindOf = (sourceIndex: number, targetIndex: number): string =>
            edges.find((edge) => edge.sourceIndex === sourceIndex && edge.targetIndex === targetIndex)!.kind;

        expect(kindOf(0, 3)).toBe('forward');
        expect(kindOf(3, 2)).toBe('back');
        expect(kindOf(2, 1)).toBe('back');
        expect(kindOf(1, 0)).toBe('back');
    });
});
