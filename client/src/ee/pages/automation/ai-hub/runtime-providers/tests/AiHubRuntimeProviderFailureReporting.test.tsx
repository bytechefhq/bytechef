import {createAiHubChat} from '@/ee/pages/automation/ai-hub/chats/api/chats.api';
import {aiHubChatsStore} from '@/ee/pages/automation/ai-hub/chats/stores/useAiHubChatsStore';
import {aiHubRunStateStore} from '@/ee/pages/automation/ai-hub/runtime-providers/stores/useAiHubRunStateStore';
import {aiHubStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {aiChatRetryableErrorStore} from '@/shared/components/ai-chat/stores/useAiChatRetryableErrorStore';
import {AppendMessage, useExternalStoreRuntime} from '@assistant-ui/react';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {act, render} from '@testing-library/react';
import {ReactNode} from 'react';
import {toast} from 'sonner';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {AiHubRuntimeProvider} from '../AiHubRuntimeProvider';

vi.mock('@assistant-ui/react', () => ({
    AssistantRuntimeProvider: ({children}: {children: ReactNode}) => <div>{children}</div>,
    useExternalStoreRuntime: vi.fn(() => ({thread: {composer: {setText: vi.fn()}}})),
}));

vi.mock('@/ee/pages/automation/ai-hub/chats/api/chats.api', () => ({
    createAiHubChat: vi.fn(),
    generateAiHubChatTitle: vi.fn(() => Promise.resolve()),
    patchChat: vi.fn(() => Promise.resolve()),
}));

vi.mock('sonner', () => ({
    toast: {error: vi.fn(), info: vi.fn()},
}));

function getRuntimeOptions() {
    return vi.mocked(useExternalStoreRuntime).mock.lastCall![0] as unknown as {
        onNew: (message: AppendMessage) => Promise<void>;
        onReload: (parentId: string | null) => Promise<void>;
    };
}

const renderProvider = () =>
    render(
        <QueryClientProvider client={new QueryClient()}>
            <AiHubRuntimeProvider>child</AiHubRuntimeProvider>
        </QueryClientProvider>
    );

const sendMessage = async (text: string) => {
    await act(async () => {
        await getRuntimeOptions().onNew({content: [{text, type: 'text'}]} as unknown as AppendMessage);
    });
};

describe('AiHubRuntimeProvider failure reporting', () => {
    beforeEach(() => {
        useWorkspaceStore.setState({currentWorkspaceId: 1});

        aiHubRunStateStore.getState().reset();
        aiHubChatsStore.getState().reset();
        aiHubChatsStore.getState().setCurrentChatId(5);
        aiHubStore.setState({chatId: 'thread-1', messages: []});
        aiChatRetryableErrorStore.setState({currentError: undefined});

        vi.spyOn(globalThis, 'fetch').mockImplementation(
            async () =>
                new Response(JSON.stringify({error: 'An unexpected error occurred while dispatching the agent'}), {
                    headers: {'Content-Type': 'application/json'},
                    status: 500,
                })
        );
    });

    afterEach(() => {
        vi.restoreAllMocks();
        vi.clearAllMocks();
    });

    it('reports a failed turn request only in the retry banner', async () => {
        renderProvider();

        await sendMessage('Build me a workflow');

        expect(aiChatRetryableErrorStore.getState().currentError).toMatchObject({
            lastUserMessage: 'Build me a workflow',
            toolName: 'aiHub',
        });
        expect(aiChatRetryableErrorStore.getState().currentError?.errorMessage).toContain('HTTP 500');
        expect(toast.error).not.toHaveBeenCalled();
    });

    it('reports a chat that failed to be created only in the retry banner', async () => {
        aiHubChatsStore.getState().reset();

        vi.mocked(createAiHubChat).mockRejectedValue(new Error('database down'));

        renderProvider();

        await sendMessage('Build me a workflow');

        expect(aiChatRetryableErrorStore.getState().currentError).toEqual({
            errorMessage: 'database down',
            lastUserMessage: 'Build me a workflow',
            toolName: 'aiHub',
        });
        expect(toast.error).not.toHaveBeenCalled();
    });

    it('reports a hub without a chat id only in a toast, since a retry cannot help', async () => {
        aiHubStore.setState({chatId: undefined, messages: []});

        renderProvider();

        await sendMessage('Build me a workflow');

        expect(toast.error).toHaveBeenCalledWith('AI Hub is not ready: missing chat id.');
        expect(aiChatRetryableErrorStore.getState().currentError).toBeUndefined();
    });

    it('reports a failed refresh only in a toast, since Retry would send the message again', async () => {
        aiHubChatsStore.getState().reset();
        aiHubStore.setState({
            chatId: 'thread-1',
            messages: [
                {content: 'Build me a workflow', id: 'user-1', role: 'user'},
                {content: 'Done.', id: 'assistant-1', role: 'assistant'},
            ],
        });

        renderProvider();

        await act(async () => {
            await getRuntimeOptions().onReload('user-1');
        });

        expect(toast.error).toHaveBeenCalledWith(expect.stringContaining('AI Hub failed: HTTP 500'));
        expect(aiChatRetryableErrorStore.getState().currentError).toBeUndefined();
    });
});
