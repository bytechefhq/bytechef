import {GraphNodeType} from '@/shared/types';
import {describe, expect, it} from 'vitest';

import orderGraphNodeIndexes from './orderGraphNodeIndexes';

function makeNodes(entries: Array<{name: string; next?: string}>): Array<GraphNodeType> {
    return entries.map((entry) => ({name: entry.name, next: entry.next, tasks: []}));
}

describe('orderGraphNodeIndexes', () => {
    it('should return an empty permutation for no nodes', () => {
        expect(orderGraphNodeIndexes([])).toEqual([]);
    });

    it('should keep declaration order when no node declares a transition', () => {
        const nodes = makeNodes([{name: 'node_0'}, {name: 'node_1'}, {name: 'node_2'}]);

        expect(orderGraphNodeIndexes(nodes)).toEqual([0, 1, 2]);
    });

    it('should order a reversed chain so it reads front to back', () => {
        const nodes = makeNodes([
            {name: 'node_c'},
            {name: 'node_b', next: "'node_c'"},
            {name: 'node_a', next: "'node_b'"},
        ]);

        expect(orderGraphNodeIndexes(nodes)).toEqual([2, 1, 0]);
    });

    it('should break a cycle at the lowest declaration index', () => {
        const nodes = makeNodes([
            {name: 'node_0', next: "'node_3'"},
            {name: 'node_1', next: "'node_0'"},
            {name: 'node_2', next: "'node_1'"},
            {name: 'node_3', next: "'node_2'"},
        ]);

        expect(orderGraphNodeIndexes(nodes)).toEqual([0, 3, 2, 1]);
    });

    it('should rank nodes with no statically resolvable transition last', () => {
        const nodes = makeNodes([
            {name: 'node_0', next: 'steps.decision.value'},
            {name: 'node_1', next: "'node_2'"},
            {name: 'node_2'},
        ]);

        expect(orderGraphNodeIndexes(nodes)).toEqual([1, 2, 0]);
    });

    it('should prefer the startNode among ready nodes', () => {
        const nodes = makeNodes([
            {name: 'node_a'},
            {name: 'node_b', next: "'node_a'"},
            {name: 'node_c', next: "'node_a'"},
        ]);

        expect(orderGraphNodeIndexes(nodes)).toEqual([1, 2, 0]);
        expect(orderGraphNodeIndexes(nodes, 'node_c')).toEqual([2, 1, 0]);
    });

    it('should let a startNode with a fully dynamic next (nothing statically points at it either) still lead, instead of landing last', () => {
        // node_router (the startNode) has no literal `next` target and nothing statically
        // transitions to it — under the old rule it would never enter the ready queue and would
        // render at the far end of the canvas despite being the graph's entry point.
        const nodes = makeNodes([
            {name: 'node_0', next: "'node_1'"},
            {name: 'node_1'},
            {name: 'node_router', next: 'steps.router.value'},
        ]);

        expect(orderGraphNodeIndexes(nodes, 'node_router')).toEqual([2, 0, 1]);
    });

    it('should keep declaration order for a graph with no statically resolvable transitions anywhere, even with a startNode set', () => {
        const nodes = makeNodes([{name: 'node_0'}, {name: 'node_1', next: 'steps.decision.value'}, {name: 'node_2'}]);

        expect(orderGraphNodeIndexes(nodes, 'node_1')).toEqual([0, 1, 2]);
    });

    it('should ignore a startNode that does not resolve to a declared node', () => {
        const nodes = makeNodes([
            {name: 'node_a'},
            {name: 'node_b', next: "'node_a'"},
            {name: 'node_c', next: "'node_a'"},
        ]);

        expect(orderGraphNodeIndexes(nodes, 'node_missing')).toEqual([1, 2, 0]);
    });

    it('should treat a self-loop as no transition, ranking that node last', () => {
        const nodes = makeNodes([
            {name: 'node_0', next: "'node_0'"},
            {name: 'node_1', next: "'node_2'"},
            {name: 'node_2'},
        ]);

        expect(orderGraphNodeIndexes(nodes)).toEqual([1, 2, 0]);
    });

    it('should follow both branches of a ternary next expression', () => {
        const nodes = makeNodes([
            {name: 'node_end'},
            {name: 'node_start', next: "steps.check.ok ? 'node_mid' : 'node_end'"},
            {name: 'node_mid', next: "'node_end'"},
        ]);

        expect(orderGraphNodeIndexes(nodes)).toEqual([1, 2, 0]);
    });

    it('should end on the visually last lane, which is not the last declared node', () => {
        // The lane header's add-node button renders on whichever lane the permutation puts last,
        // so it lands beside the trailing add-node placeholder. Keying it off the last DECLARED
        // index instead strands it mid-container: here node_4 is declared last but node_1 renders
        // last.
        const nodes = makeNodes([
            {name: 'node_0', next: "'node_3'"},
            {name: 'node_1', next: "'node_0'"},
            {name: 'node_2', next: "'node_4'"},
            {name: 'node_3', next: "'node_2'"},
            {name: 'node_4', next: "'node_1'"},
        ]);

        const ordered = orderGraphNodeIndexes(nodes);

        expect(ordered).toEqual([0, 3, 2, 4, 1]);
        expect(ordered[ordered.length - 1]).toBe(1);
        expect(ordered[ordered.length - 1]).not.toBe(nodes.length - 1);
    });

    it('should return a permutation containing every declared index exactly once', () => {
        const nodes = makeNodes([
            {name: 'node_0', next: "'node_3'"},
            {name: 'node_1', next: "'node_0'"},
            {name: 'node_2'},
            {name: 'node_3', next: "'node_1'"},
            {name: 'node_4', next: 'dynamic.value'},
        ]);

        const ordered = orderGraphNodeIndexes(nodes);

        expect([...ordered].sort((first, second) => first - second)).toEqual([0, 1, 2, 3, 4]);
    });
});
