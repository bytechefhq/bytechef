import {GraphTransitionType} from '@/shared/types';
import {describe, expect, it} from 'vitest';

import {
    addTransition,
    findNodesWithDuplicateDefault,
    isDynamicTransitionTarget,
    isUnconditional,
    moveTransition,
    removeTransition,
    removeTransitionsForNode,
    transitionsFrom,
    updateTransition,
} from './graphTransitionMutations';

describe('graphTransitionMutations', () => {
    const base: GraphTransitionType[] = [
        {from: 'a', to: 'b'},
        {condition: '=x', from: 'a', to: 'c'},
        {from: 'b', to: 'a'},
    ];

    it('addTransition appends and dedupes', () => {
        expect(addTransition(base, 'c', 'a')).toHaveLength(4);
        expect(addTransition(base, 'a', 'b')).toBe(base);
    });

    it('addTransition appends onto an empty array', () => {
        expect(addTransition([], 'a', 'b')).toEqual([{from: 'a', to: 'b'}]);
    });

    it('removeTransition drops the transition at the given index', () => {
        expect(removeTransition(base, 1)).toEqual([
            {from: 'a', to: 'b'},
            {from: 'b', to: 'a'},
        ]);
    });

    it('removeTransition never mutates the input array', () => {
        const original = [...base];

        removeTransition(base, 0);

        expect(base).toEqual(original);
    });

    it('updateTransition merges a patch onto the transition at the given index', () => {
        const updated = updateTransition(base, 0, {condition: '=y'});

        expect(updated[0]).toEqual({condition: '=y', from: 'a', to: 'b'});
        expect(base[0]).toEqual({from: 'a', to: 'b'});
    });

    it('removeTransitionsForNode drops both directions', () => {
        expect(removeTransitionsForNode(base, 'b')).toEqual([{condition: '=x', from: 'a', to: 'c'}]);
    });

    it('removeTransitionsForNode is a no-op array copy when the node is absent', () => {
        expect(removeTransitionsForNode(base, 'z')).toEqual(base);
    });

    it('transitionsFrom returns matching transitions with their original index', () => {
        expect(transitionsFrom(base, 'a')).toEqual([
            {index: 0, transition: {from: 'a', to: 'b'}},
            {index: 1, transition: {condition: '=x', from: 'a', to: 'c'}},
        ]);
    });

    it('transitionsFrom returns an empty array when no transitions match', () => {
        expect(transitionsFrom(base, 'nonexistent')).toEqual([]);
    });

    it('findNodesWithDuplicateDefault flags nodes with two unconditional edges', () => {
        expect(findNodesWithDuplicateDefault([...base, {from: 'a', to: 'd'}])).toEqual(['a']);
        expect(findNodesWithDuplicateDefault(base)).toEqual([]);
    });

    it('findNodesWithDuplicateDefault returns an empty array for an empty input', () => {
        expect(findNodesWithDuplicateDefault([])).toEqual([]);
    });

    it('isDynamicTransitionTarget detects expressions', () => {
        expect(isDynamicTransitionTarget('=nextNode')).toBe(true);
        expect(isDynamicTransitionTarget('${a.b}')).toBe(true);
        expect(isDynamicTransitionTarget('approve')).toBe(false);
    });

    // The shared property editor holds `to` empty between a field being cleared and the next value
    // being committed, and this runs on every layout pass — reading it unguarded took the canvas
    // down rather than drawing a transition that was briefly missing its target.
    it('isDynamicTransitionTarget treats an absent target as not dynamic rather than throwing', () => {
        expect(isDynamicTransitionTarget(null)).toBe(false);
        expect(isDynamicTransitionTarget(undefined)).toBe(false);
        expect(isDynamicTransitionTarget('')).toBe(false);
    });

    it('isUnconditional treats a missing or blank condition as unconditional', () => {
        expect(isUnconditional({from: 'a', to: 'b'})).toBe(true);
        expect(isUnconditional({condition: '', from: 'a', to: 'b'})).toBe(true);
        expect(isUnconditional({condition: '   ', from: 'a', to: 'b'})).toBe(true);
        expect(isUnconditional({condition: '=x', from: 'a', to: 'b'})).toBe(false);
    });

    it('moveTransition swaps within the same from-group only', () => {
        const moved = moveTransition(base, 1, -1);

        expect(moved[0]).toEqual({condition: '=x', from: 'a', to: 'c'});
        expect(moveTransition(base, 2, -1)).toBe(base); // 'b' group has one edge
    });

    it('moveTransition swaps forward within the same from-group', () => {
        const moved = moveTransition(base, 0, 1);

        expect(moved[1]).toEqual({from: 'a', to: 'b'});
        expect(moved[0]).toEqual({condition: '=x', from: 'a', to: 'c'});
    });

    it('moveTransition is a no-op when moving past the end of the group', () => {
        expect(moveTransition(base, 1, 1)).toBe(base);
    });

    it('moveTransition is a no-op for an out-of-range index', () => {
        expect(moveTransition(base, 99, 1)).toBe(base);
    });
});
