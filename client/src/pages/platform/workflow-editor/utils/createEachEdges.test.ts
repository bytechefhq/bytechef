import {Node} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import createEachEdges from './createEachEdges';

function eachNodeWithIteratee(iterateeName: string | undefined): Node {
    return {
        data: {
            componentName: 'each',
            parameters: iterateeName ? {iteratee: {name: iterateeName}} : {},
            taskDispatcher: true,
            taskDispatcherId: 'each_1',
            workflowNodeName: 'each_1',
        },
        id: 'each_1',
        position: {x: 0, y: 0},
        type: 'workflow',
    };
}

describe('createEachEdges nested dispatcher exit edge', () => {
    it('wires a nested fork-join to the each bottom ghost via its camelCase ghost id', () => {
        const edges = createEachEdges(eachNodeWithIteratee('fork-join_1'));

        // createForkJoinNode emits `fork-join_1-forkJoin-bottom-ghost`, so the
        // merge edge must reference that camelCase id — not the kebab-case
        // `fork-join_1-fork-join-bottom-ghost`, which no node has and which the
        // dagre missing-node filter would silently drop.
        const mergeEdge = edges.find(
            (edge) => edge.target === 'each_1-each-bottom-ghost' && !edge.source.includes('-taskDispatcher-left-ghost')
        );

        expect(mergeEdge?.source).toBe('fork-join_1-forkJoin-bottom-ghost');
    });

    it('wires a nested on-error to the each bottom ghost via its camelCase ghost id', () => {
        const edges = createEachEdges(eachNodeWithIteratee('on-error_1'));

        const mergeEdge = edges.find(
            (edge) => edge.target === 'each_1-each-bottom-ghost' && !edge.source.includes('-taskDispatcher-left-ghost')
        );

        expect(mergeEdge?.source).toBe('on-error_1-onError-bottom-ghost');
    });

    it('wires a nested condition to the each bottom ghost verbatim', () => {
        const edges = createEachEdges(eachNodeWithIteratee('condition_2'));

        const mergeEdge = edges.find(
            (edge) => edge.target === 'each_1-each-bottom-ghost' && !edge.source.includes('-taskDispatcher-left-ghost')
        );

        expect(mergeEdge?.source).toBe('condition_2-condition-bottom-ghost');
    });

    it('wires a plain task straight to the each bottom ghost', () => {
        const edges = createEachEdges(eachNodeWithIteratee('mailchimp_1'));

        const mergeEdge = edges.find(
            (edge) => edge.target === 'each_1-each-bottom-ghost' && !edge.source.includes('-taskDispatcher-left-ghost')
        );

        expect(mergeEdge?.source).toBe('mailchimp_1');
    });
});
