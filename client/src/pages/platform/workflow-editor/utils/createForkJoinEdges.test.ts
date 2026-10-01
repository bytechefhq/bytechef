import {Node} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import createForkJoinEdges from './createForkJoinEdges';

function buildForkJoinNode(branchTaskNames: string[][]): Node {
    return {
        data: {
            componentName: 'fork-join',
            parameters: {
                branches: branchTaskNames.map((taskNames) =>
                    taskNames.map((name) => ({name, type: 'accelo/v1/createCompany'}))
                ),
            },
            taskDispatcher: true,
            taskDispatcherId: 'fork-join_1',
        },
        id: 'fork-join_1',
        position: {x: 0, y: 0},
        type: 'workflow',
    };
}

describe('createForkJoinEdges', () => {
    it('should hang the add-a-branch chip on the entry edge of the last lane only', () => {
        const edges = createForkJoinEdges(
            buildForkJoinNode([
                ['accelo_1', 'accelo_2'],
                ['accelo_3', 'accelo_4'],
            ])
        );

        const chipEdges = edges.filter((edge) => edge.data?.addBranchPlaceholderId);

        expect(chipEdges.map((edge) => edge.id)).toEqual(['fork-join_1-forkJoin-top-ghost=>accelo_3']);
        expect(chipEdges[0].data?.addBranchPlaceholderId).toBe('fork-join_1-forkJoin-placeholder-2');
    });

    it('should hang the chip on the last lane that has tasks', () => {
        const edges = createForkJoinEdges(buildForkJoinNode([['accelo_1'], ['accelo_2'], []]));

        const chipEdges = edges.filter((edge) => edge.data?.addBranchPlaceholderId);

        expect(chipEdges.map((edge) => edge.id)).toEqual(['fork-join_1-forkJoin-top-ghost=>accelo_2']);
    });

    it('should hang a single lane from the bar right end, opposite the left rail', () => {
        const edges = createForkJoinEdges(buildForkJoinNode([['accelo_1', 'accelo_2'], []]));

        expect(edges.find((edge) => edge.target === 'accelo_1')?.sourceHandle).toBe(
            'fork-join_1-forkJoin-top-ghost-right'
        );
        expect(edges.find((edge) => edge.target === 'fork-join_1-taskDispatcher-left-ghost')?.sourceHandle).toBe(
            'fork-join_1-forkJoin-top-ghost-left'
        );
    });

    it('should hang no chip on a fork-join without lanes', () => {
        const edges = createForkJoinEdges(buildForkJoinNode([]));

        expect(edges.some((edge) => edge.data?.addBranchPlaceholderId)).toBe(false);
    });
});
