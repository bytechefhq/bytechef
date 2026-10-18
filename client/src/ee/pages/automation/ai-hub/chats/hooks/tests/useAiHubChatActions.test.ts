import {aiHubChatsStore} from '@/ee/pages/automation/ai-hub/chats/stores/useAiHubChatsStore';
import {aiHubRunStateStore} from '@/ee/pages/automation/ai-hub/runtime-providers/stores/useAiHubRunStateStore';
import {aiHubStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubStore';
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {AiHubChatI} from '../../api/chats.api';

const {cancelAiHubRunMock, deleteChatMock, navigateMock, patchChatMock} = vi.hoisted(() => ({
    cancelAiHubRunMock: vi.fn(),
    deleteChatMock: vi.fn(),
    navigateMock: vi.fn(),
    patchChatMock: vi.fn(),
}));

vi.mock('react-router-dom', () => ({
    useNavigate: () => navigateMock,
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: {currentWorkspaceId: number}) => unknown) =>
        selector({currentWorkspaceId: 3}),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useCancelAiHubRunMutation: () => ({mutate: cancelAiHubRunMock}),
}));

vi.mock('@/ee/pages/automation/ai-hub/chats/hooks/useChats', () => ({
    useDeleteAiHubChatMutation: () => ({mutate: deleteChatMock}),
    usePatchAiHubChatMutation: () => ({mutate: patchChatMock}),
}));

const {useAiHubChatActions} = await import('../useAiHubChatActions');

const CHAT: AiHubChatI = {
    autoTitled: true,
    createdAt: '2026-10-18T00:00:00Z',
    environmentId: 0,
    id: 7,
    isOwner: true,
    lastPreview: null,
    messageCount: 2,
    ownerUserId: 1,
    status: 'ACTIVE',
    threadId: 'thread-7',
    title: 'Old title',
    updatedAt: '2026-10-18T00:00:00Z',
    userId: 1,
    workspaceId: 3,
};

describe('useAiHubChatActions', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        aiHubChatsStore.setState({chatActivity: {}, currentChatId: 7});
        aiHubRunStateStore.setState({runIdByChat: {}, runningByChat: {}});
        aiHubStore.setState({chatId: 'thread-7', messages: [{content: 'hi', role: 'user'}]});
    });

    it('archives a chat and leaves it when it is the current one', () => {
        const {result} = renderHook(() => useAiHubChatActions());

        act(() => {
            result.current.archiveChat(CHAT);
        });

        expect(patchChatMock).toHaveBeenCalledWith({chatId: 7, patch: {status: 'ARCHIVED'}, workspaceId: 3});
        expect(aiHubChatsStore.getState().currentChatId).toBeUndefined();
    });

    it('unarchives a chat', () => {
        const {result} = renderHook(() => useAiHubChatActions());

        act(() => {
            result.current.unarchiveChat({...CHAT, status: 'ARCHIVED'});
        });

        expect(patchChatMock).toHaveBeenCalledWith({chatId: 7, patch: {status: 'ACTIVE'}, workspaceId: 3});
    });

    it('renames with the trimmed title and skips an unchanged one', () => {
        const {result} = renderHook(() => useAiHubChatActions());

        act(() => {
            result.current.requestRename(CHAT);
        });

        act(() => {
            result.current.submitRename('  Old title  ');
        });

        expect(patchChatMock).not.toHaveBeenCalled();
        expect(result.current.renameTarget).toBeNull();

        act(() => {
            result.current.requestRename(CHAT);
        });

        act(() => {
            result.current.submitRename('  New title ');
        });

        expect(patchChatMock).toHaveBeenCalledWith({chatId: 7, patch: {title: 'New title'}, workspaceId: 3});
    });

    it('cancels a running turn, deletes the chat and returns home when the current chat is deleted', () => {
        aiHubRunStateStore.setState({runIdByChat: {'thread-7': 'run-1'}, runningByChat: {'thread-7': true}});

        const {result} = renderHook(() => useAiHubChatActions());

        act(() => {
            result.current.requestDelete(CHAT);
        });

        act(() => {
            result.current.confirmDelete();
        });

        expect(cancelAiHubRunMock).toHaveBeenCalledWith({id: '7', runId: 'run-1', workspaceId: '3'});
        expect(aiHubRunStateStore.getState().runningByChat['thread-7']).toBeFalsy();
        expect(deleteChatMock).toHaveBeenCalledWith({chatId: 7, workspaceId: 3});
        expect(aiHubChatsStore.getState().currentChatId).toBeUndefined();
        expect(aiHubStore.getState().messages).toEqual([]);
        expect(aiHubStore.getState().chatId).not.toBe('thread-7');
        expect(navigateMock).toHaveBeenCalledWith('/automation/ai-hub');
        expect(result.current.deleteTarget).toBeNull();
    });

    it('does not cancel anything when the deleted chat is idle', () => {
        aiHubChatsStore.setState({currentChatId: 9});

        const {result} = renderHook(() => useAiHubChatActions());

        act(() => {
            result.current.requestDelete(CHAT);
        });

        act(() => {
            result.current.confirmDelete();
        });

        expect(cancelAiHubRunMock).not.toHaveBeenCalled();
        expect(deleteChatMock).toHaveBeenCalled();
        expect(navigateMock).not.toHaveBeenCalled();
        expect(aiHubChatsStore.getState().currentChatId).toBe(9);
    });
});
