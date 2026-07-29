import {describe, expect, it} from 'vitest';

import {nestedBottomGhostIdForDispatcherTask, nestedDispatcherGhostSegment} from './nestedBottomGhostId';

describe('nestedDispatcherGhostSegment', () => {
    it('remaps the two hyphenated componentNames to their camelCase ghost segments', () => {
        expect(nestedDispatcherGhostSegment('fork-join')).toBe('forkJoin');
        expect(nestedDispatcherGhostSegment('on-error')).toBe('onError');
    });

    it('passes every other dispatcher name through verbatim', () => {
        for (const componentName of ['condition', 'loop', 'branch', 'each', 'map', 'parallel']) {
            expect(nestedDispatcherGhostSegment(componentName)).toBe(componentName);
        }
    });
});

describe('nestedBottomGhostIdForDispatcherTask', () => {
    it('builds the camelCase bottom-ghost id for fork-join, matching createForkJoinNode', () => {
        // createForkJoinNode emits `${forkJoinId}-forkJoin-bottom-ghost`
        expect(nestedBottomGhostIdForDispatcherTask('fork-join_1')).toBe('fork-join_1-forkJoin-bottom-ghost');
    });

    it('builds the camelCase bottom-ghost id for on-error, matching createOnErrorNode', () => {
        // createOnErrorNode emits `${onErrorId}-onError-bottom-ghost`
        expect(nestedBottomGhostIdForDispatcherTask('on-error_2')).toBe('on-error_2-onError-bottom-ghost');
    });

    it('uses the componentName verbatim for the remaining dispatchers', () => {
        expect(nestedBottomGhostIdForDispatcherTask('condition_3')).toBe('condition_3-condition-bottom-ghost');
        expect(nestedBottomGhostIdForDispatcherTask('loop_1')).toBe('loop_1-loop-bottom-ghost');
        expect(nestedBottomGhostIdForDispatcherTask('branch_4')).toBe('branch_4-branch-bottom-ghost');
    });
});
