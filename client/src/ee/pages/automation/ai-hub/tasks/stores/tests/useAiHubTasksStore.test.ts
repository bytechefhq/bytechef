import {beforeEach, describe, expect, it} from 'vitest';

import {aiHubTasksStore} from '../useAiHubTasksStore';

describe('useAiHubTasksStore', () => {
    beforeEach(() => {
        aiHubTasksStore.setState({
            activeFilter: 'ACTIVE',
            currentTaskId: undefined,
            searchTerm: '',
        });
    });

    it('setActiveFilter updates activeFilter to the given value', () => {
        aiHubTasksStore.getState().setActiveFilter('ARCHIVED');

        expect(aiHubTasksStore.getState().activeFilter).toBe('ARCHIVED');
    });

    it('setCurrentTaskId updates currentTaskId to the given number', () => {
        aiHubTasksStore.getState().setCurrentTaskId(42);

        expect(aiHubTasksStore.getState().currentTaskId).toBe(42);
    });

    it('setCurrentTaskId accepts undefined to clear the selection', () => {
        aiHubTasksStore.getState().setCurrentTaskId(7);
        aiHubTasksStore.getState().setCurrentTaskId(undefined);

        expect(aiHubTasksStore.getState().currentTaskId).toBeUndefined();
    });

    it('setSearchTerm updates searchTerm to the given string', () => {
        aiHubTasksStore.getState().setSearchTerm('my task');

        expect(aiHubTasksStore.getState().searchTerm).toBe('my task');
    });
});
