import {describe, expect, it} from 'vitest';

import {findGraphMembersPrecedingMember} from './graphReachability';

describe('findGraphMembersPrecedingMember', () => {
    it('walks transitions backwards and leaves a sibling branch out', () => {
        const preceding = findGraphMembersPrecedingMember(
            'var_5',
            [
                {from: 'var_1', to: 'var_4'},
                {from: 'var_1', to: 'var_3'},
                {from: 'var_4', to: 'var_5'},
            ],
            ['var_1', 'var_3', 'var_4', 'var_5']
        );

        expect([...preceding].sort()).toEqual(['var_1', 'var_4']);
    });

    it('returns nothing preceding an entry member', () => {
        const preceding = findGraphMembersPrecedingMember('var_1', [{from: 'var_1', to: 'var_2'}], ['var_1', 'var_2']);

        expect([...preceding]).toEqual([]);
    });

    it('includes a member that a cycle leads back to', () => {
        const preceding = findGraphMembersPrecedingMember(
            'a',
            [
                {from: 'a', to: 'b'},
                {from: 'b', to: 'a'},
            ],
            ['a', 'b']
        );

        expect([...preceding].sort()).toEqual(['a', 'b']);
    });

    it('terminates on a cycle that does not reach the member', () => {
        const preceding = findGraphMembersPrecedingMember(
            'c',
            [
                {from: 'a', to: 'b'},
                {from: 'b', to: 'a'},
            ],
            ['a', 'b', 'c']
        );

        expect([...preceding]).toEqual([]);
    });

    it('ignores a transition whose source is not a declared member', () => {
        const preceding = findGraphMembersPrecedingMember(
            'var_2',
            [
                {from: 'var_1', to: 'var_2'},
                {from: 'removed', to: 'var_2'},
            ],
            ['var_1', 'var_2']
        );

        expect([...preceding]).toEqual(['var_1']);
    });

    it('returns every member, the member itself included, when any transition targets an expression', () => {
        const preceding = findGraphMembersPrecedingMember(
            'var_1',
            [
                {from: 'var_1', to: 'var_4'},
                {from: 'var_4', to: '=nextStep'},
            ],
            ['var_1', 'var_4', 'var_5']
        );

        expect([...preceding].sort()).toEqual(['var_1', 'var_4', 'var_5']);
    });
});
