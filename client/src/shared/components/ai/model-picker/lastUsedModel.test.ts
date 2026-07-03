import {beforeEach, describe, expect, it} from 'vitest';

import {readLastUsedModel, writeLastUsedModel} from './lastUsedModel';

describe('lastUsedModel', () => {
    beforeEach(() => {
        localStorage.clear();
    });

    it('returns null when nothing stored', () => {
        expect(readLastUsedModel(42)).toBeNull();
    });

    it('round-trips a written selection per workspace', () => {
        writeLastUsedModel(42, 'ai.provider.openAi', 'gpt-4o');

        expect(readLastUsedModel(42)).toEqual({model: 'gpt-4o', provider: 'ai.provider.openAi'});
        expect(readLastUsedModel(43)).toBeNull();
    });

    it('clears when provider or model is null', () => {
        writeLastUsedModel(42, 'ai.provider.openAi', 'gpt-4o');
        writeLastUsedModel(42, null, null);

        expect(readLastUsedModel(42)).toBeNull();
    });
});
