import {beforeEach, describe, expect, it} from 'vitest';

import {aiHubChatsStore} from '../useAiHubChatsStore';

describe('useAiHubChatsStore', () => {
    beforeEach(() => {
        aiHubChatsStore.getState().reset();
    });

    it('setActiveFilter updates activeFilter to the given value', () => {
        aiHubChatsStore.getState().setActiveFilter('ARCHIVED');

        expect(aiHubChatsStore.getState().activeFilter).toBe('ARCHIVED');
    });

    it('setCurrentChatId updates currentChatId to the given number', () => {
        aiHubChatsStore.getState().setCurrentChatId(42);

        expect(aiHubChatsStore.getState().currentChatId).toBe(42);
    });

    it('setCurrentChatId accepts undefined to clear the selection', () => {
        aiHubChatsStore.getState().setCurrentChatId(7);
        aiHubChatsStore.getState().setCurrentChatId(undefined);

        expect(aiHubChatsStore.getState().currentChatId).toBeUndefined();
    });

    it('setSearchTerm updates searchTerm to the given string', () => {
        aiHubChatsStore.getState().setSearchTerm('my chat');

        expect(aiHubChatsStore.getState().searchTerm).toBe('my chat');
    });

    describe('activity state', () => {
        it('sets and clears a thread activity state', () => {
            aiHubChatsStore.getState().setActivityState('thread-a', 'running');

            expect(aiHubChatsStore.getState().chatActivity).toEqual({'thread-a': 'running'});

            aiHubChatsStore.getState().clearActivityState('thread-a');

            expect(aiHubChatsStore.getState().chatActivity).toEqual({});
        });

        it('does not let running overwrite paused', () => {
            aiHubChatsStore.getState().setActivityState('thread-a', 'paused');
            aiHubChatsStore.getState().setActivityState('thread-a', 'running');

            expect(aiHubChatsStore.getState().chatActivity['thread-a']).toBe('paused');
        });

        it('lets paused overwrite running', () => {
            aiHubChatsStore.getState().setActivityState('thread-a', 'running');
            aiHubChatsStore.getState().setActivityState('thread-a', 'paused');

            expect(aiHubChatsStore.getState().chatActivity['thread-a']).toBe('paused');
        });
    });

    describe('LLM selection', () => {
        it('moves the draft selection into the new chat slot and clears the draft', () => {
            aiHubChatsStore.getState().setDraftLlmSelection('anthropic', 'claude');

            aiHubChatsStore.getState().consumeDraftLlmSelection(9);

            expect(aiHubChatsStore.getState().chatLlmSelections[9]).toEqual({model: 'claude', provider: 'anthropic'});
            expect(aiHubChatsStore.getState().draftLlmSelection).toBeNull();
        });

        it('is a no-op when no draft selection exists', () => {
            const before = aiHubChatsStore.getState().chatLlmSelections;

            aiHubChatsStore.getState().consumeDraftLlmSelection(9);

            expect(aiHubChatsStore.getState().chatLlmSelections).toBe(before);
        });

        it('stores null for a cleared draft', () => {
            aiHubChatsStore.getState().setDraftLlmSelection('anthropic', 'claude');
            aiHubChatsStore.getState().setDraftLlmSelection(null, null);

            expect(aiHubChatsStore.getState().draftLlmSelection).toBeNull();
        });
    });

    describe('title generation failures', () => {
        it('marks and clears a failure', () => {
            aiHubChatsStore.getState().markTitleGenerationFailed(3, 'boom');

            expect(aiHubChatsStore.getState().titleGenerationFailures).toEqual({3: 'boom'});

            aiHubChatsStore.getState().clearTitleGenerationFailure(3);

            expect(aiHubChatsStore.getState().titleGenerationFailures).toEqual({});
        });
    });
});
