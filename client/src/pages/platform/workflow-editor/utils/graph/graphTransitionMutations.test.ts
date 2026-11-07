import {GraphTransitionType} from '@/shared/types';
import {describe, expect, it} from 'vitest';

import {
    addTransition,
    findNodesWithDuplicateDefault,
    isDynamicTransitionTarget,
    isSameTransition,
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
        expect(addTransition({from: 'c', to: 'a', transitions: base})).toHaveLength(4);
        expect(addTransition({from: 'a', to: 'b', transitions: base})).toBe(base);
    });

    it('addTransition appends onto an empty array', () => {
        expect(addTransition({from: 'a', to: 'b', transitions: []})).toEqual([{from: 'a', to: 'b'}]);
    });

    it('removeTransition drops the transition at the given index', () => {
        expect(removeTransition({expectedTransition: base[1], index: 1, transitions: base})).toEqual([
            {from: 'a', to: 'b'},
            {from: 'b', to: 'a'},
        ]);
    });

    it('removeTransition never mutates the input array', () => {
        const original = [...base];

        removeTransition({expectedTransition: base[0], index: 0, transitions: base});

        expect(base).toEqual(original);
    });

    it('removeTransition leaves the list alone when another transition has shifted into the index', () => {
        const shifted = base.slice(1);

        expect(removeTransition({expectedTransition: base[0], index: 0, transitions: shifted})).toBe(shifted);
    });

    it('updateTransition merges a patch onto the transition at the given index', () => {
        const updated = updateTransition({
            expectedTransition: base[0],
            index: 0,
            patch: {condition: '=y'},
            transitions: base,
        });

        expect(updated[0]).toEqual({condition: '=y', from: 'a', to: 'b'});
        expect(base[0]).toEqual({from: 'a', to: 'b'});
    });

    it('updateTransition leaves the list alone when another transition has shifted into the index', () => {
        const shifted = base.slice(1);

        expect(updateTransition({expectedTransition: base[0], index: 0, patch: {to: 'c'}, transitions: shifted})).toBe(
            shifted
        );
    });

    it('isSameTransition treats a missing and a blank condition as the same transition', () => {
        expect(isSameTransition({from: 'a', to: 'b'}, {condition: '', from: 'a', to: 'b'})).toBe(true);
        expect(isSameTransition({from: 'a', to: 'b'}, {condition: '=x', from: 'a', to: 'b'})).toBe(false);
        expect(isSameTransition({from: 'a', to: 'b'}, undefined)).toBe(false);
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

    it('isUnconditional treats a bare formula prefix as unconditional, matching the server', () => {
        expect(isUnconditional({condition: '=', from: 'a', to: 'b'})).toBe(true);
        expect(isUnconditional({condition: '  =  ', from: 'a', to: 'b'})).toBe(true);
        expect(findNodesWithDuplicateDefault([...base, {condition: '=', from: 'a', to: 'd'}])).toEqual(['a']);
    });

    const prioritised: GraphTransitionType[] = [
        {condition: '=x', from: 'a', to: 'b'},
        {from: 'a', to: 'd'},
        {condition: '=y', from: 'a', to: 'c'},
        {condition: '=z', from: 'b', to: 'a'},
    ];

    it('moveTransition swaps a conditional transition with the next conditional one leaving the same node', () => {
        const moved = moveTransition({
            direction: 1,
            expectedTransition: prioritised[0],
            index: 0,
            transitions: prioritised,
        });

        expect(moved).toEqual([
            {condition: '=y', from: 'a', to: 'c'},
            {from: 'a', to: 'd'},
            {condition: '=x', from: 'a', to: 'b'},
            {condition: '=z', from: 'b', to: 'a'},
        ]);
    });

    it('moveTransition swaps backward among the conditional transitions leaving the same node', () => {
        const moved = moveTransition({
            direction: -1,
            expectedTransition: prioritised[2],
            index: 2,
            transitions: prioritised,
        });

        expect(moved[0]).toEqual({condition: '=y', from: 'a', to: 'c'});
        expect(moved[2]).toEqual({condition: '=x', from: 'a', to: 'b'});
    });

    it('moveTransition never swaps a conditional transition with an unconditional one', () => {
        expect(
            moveTransition({direction: -1, expectedTransition: prioritised[1], index: 1, transitions: prioritised})
        ).toBe(prioritised);
    });

    it('moveTransition is a no-op when moving past the end of the group', () => {
        expect(
            moveTransition({direction: 1, expectedTransition: prioritised[2], index: 2, transitions: prioritised})
        ).toBe(prioritised);
    });

    it('moveTransition is a no-op for an out-of-range index', () => {
        expect(moveTransition({direction: 1, expectedTransition: base[0], index: 99, transitions: base})).toBe(base);
    });

    it('moveTransition is a no-op when another transition has shifted into the index', () => {
        const shifted = base.slice(1);

        expect(moveTransition({direction: 1, expectedTransition: base[0], index: 0, transitions: shifted})).toBe(
            shifted
        );
    });
});
