import {Node} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import createParallelEdges from './createParallelEdges';

function buildParallelNode(taskNames: string[]): Node {
    return {
        data: {
            componentName: 'parallel',
            parameters: {tasks: taskNames.map((name) => ({name, type: 'accelo/v1/createCompany'}))},
            taskDispatcher: true,
            taskDispatcherId: 'parallel_1',
        },
        id: 'parallel_1',
        position: {x: 0, y: 0},
        type: 'workflow',
    };
}

describe('createParallelEdges', () => {
    it('should hang the add-a-branch chip on the entry edge of the last lane only', () => {
        const edges = createParallelEdges(buildParallelNode(['accelo_1', 'accelo_2', 'accelo_3']));

        const chipEdges = edges.filter((edge) => edge.data?.addBranchPlaceholderId);

        expect(chipEdges.map((edge) => edge.id)).toEqual(['parallel_1-parallel-top-ghost=>accelo_3']);
        expect(chipEdges[0].data?.addBranchPlaceholderId).toBe('parallel_1-parallel-placeholder-0');
    });

    it('should hang a single lane from the bar right end, opposite the left rail', () => {
        const edges = createParallelEdges(buildParallelNode(['accelo_1']));

        expect(edges.find((edge) => edge.target === 'accelo_1')?.sourceHandle).toBe(
            'parallel_1-parallel-top-ghost-right'
        );
        expect(edges.find((edge) => edge.target === 'parallel_1-taskDispatcher-left-ghost')?.sourceHandle).toBe(
            'parallel_1-parallel-top-ghost-left'
        );
    });

    it('should hang no chip on a parallel without lanes', () => {
        const edges = createParallelEdges(buildParallelNode([]));

        expect(edges.some((edge) => edge.data?.addBranchPlaceholderId)).toBe(false);
    });
});
