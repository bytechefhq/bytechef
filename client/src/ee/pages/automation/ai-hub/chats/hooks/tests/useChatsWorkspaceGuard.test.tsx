import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {renderHook, waitFor} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {getChatArtifactsMock, getChatMessagesMock, listChatsMock} = vi.hoisted(() => ({
    getChatArtifactsMock: vi.fn(),
    getChatMessagesMock: vi.fn(),
    listChatsMock: vi.fn(),
}));

vi.mock('../../api/chats.api', async (importOriginal) => {
    const actual = await importOriginal<typeof import('../../api/chats.api')>();

    return {
        ...actual,
        getChatArtifacts: getChatArtifactsMock,
        getChatMessages: getChatMessagesMock,
        listChats: listChatsMock,
    };
});

const {useAiHubChatArtifactsQuery, useAiHubChatMessagesQuery, useAiHubChatsQuery} = await import('../useChats');

const createWrapper = () => {
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});

    const Wrapper = ({children}: {children: ReactNode}) => (
        <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );

    return Wrapper;
};

beforeEach(() => {
    getChatArtifactsMock.mockReset().mockResolvedValue([]);
    getChatMessagesMock.mockReset().mockResolvedValue([]);
    listChatsMock.mockReset().mockResolvedValue([]);
});

describe('AI Hub chat queries before the workspace store hydrates', () => {
    it('does not list chats while the workspace id is unset and lists them once it is set', async () => {
        const {rerender} = renderHook(
            ({workspaceId}: {workspaceId: number | undefined}) => useAiHubChatsQuery(workspaceId, 2, 'ACTIVE'),
            {initialProps: {workspaceId: undefined as number | undefined}, wrapper: createWrapper()}
        );

        await new Promise((resolve) => setTimeout(resolve, 0));

        expect(listChatsMock).not.toHaveBeenCalled();

        rerender({workspaceId: 1049});

        await waitFor(() => expect(listChatsMock).toHaveBeenCalledTimes(1));

        expect(listChatsMock).toHaveBeenCalledWith({environment: 2, status: 'ACTIVE', workspaceId: 1049});
    });

    it('treats workspace id 0 as unknown and does not list chats', async () => {
        renderHook(() => useAiHubChatsQuery(0, 2, 'ACTIVE'), {wrapper: createWrapper()});

        await new Promise((resolve) => setTimeout(resolve, 0));

        expect(listChatsMock).not.toHaveBeenCalled();
    });

    it('does not load chat messages while the workspace id is unset and loads them once it is set', async () => {
        const {rerender} = renderHook(
            ({workspaceId}: {workspaceId: number | undefined}) => useAiHubChatMessagesQuery(1052, workspaceId),
            {initialProps: {workspaceId: undefined as number | undefined}, wrapper: createWrapper()}
        );

        await new Promise((resolve) => setTimeout(resolve, 0));

        expect(getChatMessagesMock).not.toHaveBeenCalled();

        rerender({workspaceId: 1049});

        await waitFor(() => expect(getChatMessagesMock).toHaveBeenCalledWith({chatId: 1052, workspaceId: 1049}));
    });

    it('does not load chat artifacts while the workspace id is unset and loads them once it is set', async () => {
        const {rerender} = renderHook(
            ({workspaceId}: {workspaceId: number | undefined}) => useAiHubChatArtifactsQuery(1052, workspaceId),
            {initialProps: {workspaceId: undefined as number | undefined}, wrapper: createWrapper()}
        );

        await new Promise((resolve) => setTimeout(resolve, 0));

        expect(getChatArtifactsMock).not.toHaveBeenCalled();

        rerender({workspaceId: 1049});

        await waitFor(() => expect(getChatArtifactsMock).toHaveBeenCalledWith({chatId: 1052, workspaceId: 1049}));
    });
});
