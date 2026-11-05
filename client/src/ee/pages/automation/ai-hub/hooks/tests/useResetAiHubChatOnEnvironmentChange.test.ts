import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {useResetAiHubChatOnEnvironmentChange} from '../useResetAiHubChatOnEnvironmentChange';

const hoisted = vi.hoisted(() => ({
    currentEnvironmentId: 1,
    generateChatId: vi.fn(),
    navigate: vi.fn(),
    resetMessages: vi.fn(),
    setCurrentChatId: vi.fn(),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: Record<string, unknown>) => unknown) =>
        selector({currentEnvironmentId: hoisted.currentEnvironmentId}),
}));

vi.mock('react-router-dom', () => ({
    useNavigate: () => hoisted.navigate,
}));

vi.mock('../../chats/stores/useAiHubChatsStore', () => ({
    aiHubChatsStore: {
        getState: () => ({setCurrentChatId: hoisted.setCurrentChatId}),
    },
}));

vi.mock('../../stores/useAiHubStore', () => ({
    aiHubStore: {
        getState: () => ({
            generateChatId: hoisted.generateChatId,
            resetMessages: hoisted.resetMessages,
        }),
    },
}));

describe('useResetAiHubChatOnEnvironmentChange', () => {
    beforeEach(() => {
        hoisted.currentEnvironmentId = 1;

        vi.clearAllMocks();
    });

    it('does not reset on the initial mount', () => {
        renderHook(() => useResetAiHubChatOnEnvironmentChange());

        expect(hoisted.setCurrentChatId).not.toHaveBeenCalled();
        expect(hoisted.resetMessages).not.toHaveBeenCalled();
        expect(hoisted.navigate).not.toHaveBeenCalled();
    });

    it('clears the active chat and routes home when the environment changes', () => {
        const {rerender} = renderHook(() => useResetAiHubChatOnEnvironmentChange());

        hoisted.currentEnvironmentId = 2;

        rerender();

        expect(hoisted.setCurrentChatId).toHaveBeenCalledWith(undefined);
        expect(hoisted.resetMessages).toHaveBeenCalledTimes(1);
        expect(hoisted.generateChatId).toHaveBeenCalledTimes(1);
        expect(hoisted.navigate).toHaveBeenCalledWith('/automation/ai-hub');
    });

    it('does not reset when a rerender leaves the environment unchanged', () => {
        const {rerender} = renderHook(() => useResetAiHubChatOnEnvironmentChange());

        rerender();

        expect(hoisted.setCurrentChatId).not.toHaveBeenCalled();
        expect(hoisted.navigate).not.toHaveBeenCalled();
    });
});
