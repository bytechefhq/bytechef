import {afterEach, describe, expect, it} from 'vitest';

import {
    getCompletionContext,
    registerCompletionContext,
    unregisterCompletionContext,
} from './completionContextRegistry';

describe('completionContextRegistry', () => {
    afterEach(() => {
        unregisterCompletionContext('inmemory://model/1');
        unregisterCompletionContext('inmemory://model/2');
    });

    it('returns undefined for an unknown model', () => {
        expect(getCompletionContext('inmemory://model/1')).toBeUndefined();
    });

    it('returns the latest context registered for a model', () => {
        registerCompletionContext('inmemory://model/1', {kind: 'customComponent', language: 'javascript'});
        registerCompletionContext('inmemory://model/1', {kind: 'customComponent', language: 'python'});

        expect(getCompletionContext('inmemory://model/1')).toEqual({kind: 'customComponent', language: 'python'});
    });

    it('keeps other models when one is unregistered', () => {
        registerCompletionContext('inmemory://model/1', {kind: 'customComponent', language: 'javascript'});
        registerCompletionContext('inmemory://model/2', {kind: 'customComponent', language: 'ruby'});

        unregisterCompletionContext('inmemory://model/1');

        expect(getCompletionContext('inmemory://model/1')).toBeUndefined();
        expect(getCompletionContext('inmemory://model/2')).toEqual({kind: 'customComponent', language: 'ruby'});
    });
});
