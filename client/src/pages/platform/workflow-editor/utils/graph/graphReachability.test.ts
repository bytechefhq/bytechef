import {describe, expect, it} from 'vitest';

import {findGraphMembersPrecedingMember} from './graphReachability';

describe('findGraphMembersPrecedingMember', () => {
    // The case that made a graph member offer a sibling branch's output: var_3 is DECLARED before
    // var_5, but the only route to var_5 runs through var_4, so var_3 cannot have produced anything.
    it('walks transitions backwards and leaves a sibling branch out', () => {
        const preceding = findGraphMembersPrecedingMember('var_5', [
            {from: 'var_1', to: 'var_4'},
            {from: 'var_1', to: 'var_3'},
            {from: 'var_4', to: 'var_5'},
        ]);

        expect([...preceding!].sort()).toEqual(['var_1', 'var_4']);
    });

    it('returns nothing preceding an entry member', () => {
        const preceding = findGraphMembersPrecedingMember('var_1', [{from: 'var_1', to: 'var_2'}]);

        expect([...preceding!]).toEqual([]);
    });

    // On the second lap the member's own earlier output really has been produced, so a member on a
    // cycle reaches itself.
    it('includes a member that a cycle leads back to', () => {
        const preceding = findGraphMembersPrecedingMember('a', [
            {from: 'a', to: 'b'},
            {from: 'b', to: 'a'},
        ]);

        expect([...preceding!].sort()).toEqual(['a', 'b']);
    });

    it('terminates on a cycle that does not reach the member', () => {
        const preceding = findGraphMembersPrecedingMember('c', [
            {from: 'a', to: 'b'},
            {from: 'b', to: 'a'},
        ]);

        expect([...preceding!]).toEqual([]);
    });

    // A target written as an expression may land on any member, so nothing can be ruled out — the
    // caller filters nothing rather than hiding an output that may genuinely be there.
    it('declines to answer when any transition targets an expression', () => {
        expect(
            findGraphMembersPrecedingMember('var_5', [
                {from: 'var_1', to: 'var_4'},
                {from: 'var_4', to: '=nextStep'},
            ])
        ).toBeUndefined();
    });
});
