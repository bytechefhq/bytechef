import {
    findExternalStoreMessageIndex,
    findTurnUserMessageIndex,
} from '@/shared/components/ai-chat/messages/findExternalStoreMessageIndex';
import {describe, expect, it} from 'vitest';

describe('findExternalStoreMessageIndex', () => {
    const identifiedMessages = [
        {id: 'user-1', role: 'user'},
        {id: 'assistant-1', role: 'assistant'},
        {id: 'user-2', role: 'user'},
    ];

    it('finds a message by its own id', () => {
        expect(findExternalStoreMessageIndex(identifiedMessages, 'user-2')).toBe(2);
    });

    it('finds an id-less message by the fallback id assistant-ui gives it', () => {
        const messages = [{role: 'user'}, {role: 'assistant'}, {role: 'user'}];

        expect(findExternalStoreMessageIndex(messages, '__external_store_fallback_2')).toBe(2);
    });

    it('does not resolve a fallback id onto a message that has its own id', () => {
        expect(findExternalStoreMessageIndex(identifiedMessages, '__external_store_fallback_1')).toBe(-1);
    });

    it('does not resolve a fallback id past the end of the messages', () => {
        expect(findExternalStoreMessageIndex([{role: 'user'}], '__external_store_fallback_5')).toBe(-1);
    });

    it('does not resolve a malformed fallback id', () => {
        expect(findExternalStoreMessageIndex([{role: 'user'}], '__external_store_fallback_x')).toBe(-1);
    });

    it('does not read a bare number as an index', () => {
        expect(findExternalStoreMessageIndex([{role: 'user'}, {role: 'assistant'}], '1')).toBe(-1);
    });

    it('does not resolve a missing id', () => {
        expect(findExternalStoreMessageIndex(identifiedMessages, null)).toBe(-1);
        expect(findExternalStoreMessageIndex(identifiedMessages, undefined)).toBe(-1);
    });
});

describe('findTurnUserMessageIndex', () => {
    const messages = [{role: 'user'}, {role: 'assistant'}, {role: 'user'}, {role: 'assistant'}, {role: 'assistant'}];

    it('finds the user message that opened the turn of a later assistant message', () => {
        expect(findTurnUserMessageIndex(messages, 4)).toBe(2);
    });

    it('returns the index itself for a user message', () => {
        expect(findTurnUserMessageIndex(messages, 2)).toBe(2);
    });

    it('returns -1 when no user message precedes the index', () => {
        expect(findTurnUserMessageIndex([{role: 'assistant'}], 0)).toBe(-1);
        expect(findTurnUserMessageIndex(messages, -1)).toBe(-1);
    });
});
