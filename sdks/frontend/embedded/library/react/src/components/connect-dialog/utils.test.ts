import {describe, expect, it} from 'vitest';

import {mergeWorkflowInputs, optionsCacheKey, stableSerialize} from './utils';

describe('optionsCacheKey', () => {
    it('keys by component reference, property, and dependency values', () => {
        expect(optionsCacheKey('slack', 1, 'channel', 'channel', {workspace: 'W1'})).toBe(
            'slack:1:channel:channel:{"workspace":"W1"}'
        );
    });

    it('treats undefined dependency values as empty', () => {
        expect(optionsCacheKey('slack', 1, 'channel', 'channel', undefined as never)).toBe(
            'slack:1:channel:channel:{}'
        );
    });

    it('produces the same key regardless of dependency key order', () => {
        expect(optionsCacheKey('slack', 1, 'channel', 'channel', {team: 'T1', workspace: 'W1'})).toBe(
            optionsCacheKey('slack', 1, 'channel', 'channel', {workspace: 'W1', team: 'T1'})
        );
    });
});

describe('stableSerialize', () => {
    it('sorts keys deterministically', () => {
        expect(stableSerialize({b: 2, a: 1})).toBe('{"a":1,"b":2}');
    });

    it('falls back to String() for values JSON.stringify cannot handle', () => {
        expect(stableSerialize({big: BigInt(1)})).toBe('{"big":1}');
    });

    it('serializes undefined values without throwing', () => {
        expect(stableSerialize({missing: undefined})).toBe('{"missing":undefined}');
    });
});

describe('mergeWorkflowInputs', () => {
    it('overrides scalar inputs', () => {
        expect(mergeWorkflowInputs({name: 'server', other: 'kept'}, {name: 'override'})).toEqual({
            name: 'override',
            other: 'kept',
        });
    });

    it('merges group inputs one level deep, preserving untouched server-side members', () => {
        expect(
            mergeWorkflowInputs({channel: {channelId: 'C0', workspace: 'W1'}}, {channel: {channelId: 'C1'}})
        ).toEqual({
            channel: {channelId: 'C1', workspace: 'W1'},
        });
    });

    it('replaces the server value when only one side is an object', () => {
        expect(mergeWorkflowInputs({channel: 'plain'}, {channel: {channelId: 'C1'}})).toEqual({
            channel: {channelId: 'C1'},
        });

        expect(mergeWorkflowInputs({channel: {channelId: 'C0'}}, {channel: 'plain'})).toEqual({
            channel: 'plain',
        });
    });

    it('returns the server inputs untouched when there are no overrides', () => {
        expect(mergeWorkflowInputs({name: 'server'}, undefined)).toEqual({name: 'server'});
    });
});
