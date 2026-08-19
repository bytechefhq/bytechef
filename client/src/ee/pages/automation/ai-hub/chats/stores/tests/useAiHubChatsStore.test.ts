import {beforeEach, describe, expect, it} from 'vitest';

import {aiHubChatsStore} from '../useAiHubChatsStore';

describe('useAiHubChatsStore', () => {
    beforeEach(() => {
        aiHubChatsStore.setState({
            activeFilter: 'ACTIVE',
            currentChatId: undefined,
            searchTerm: '',
        });
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
});
