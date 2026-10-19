import {generateAiHubChatTitle, patchChat} from '@/ee/pages/automation/ai-hub/chats/api/chats.api';
import {aiHubChatsStore} from '@/ee/pages/automation/ai-hub/chats/stores/useAiHubChatsStore';
import {aiHubRunStateStore} from '@/ee/pages/automation/ai-hub/runtime-providers/stores/useAiHubRunStateStore';
import {aiHubStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {aiChatRetryableErrorStore} from '@/shared/components/ai-chat/stores/useAiChatRetryableErrorStore';
import {AppendMessage, ThreadMessageLike, useExternalStoreRuntime} from '@assistant-ui/react';
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
        isRunning: boolean;
        onNew: (message: AppendMessage) => Promise<void>;
    };
}

const runErrorParts = (messages: ThreadMessageLike[]) =>
    messages.flatMap((message) =>
        Array.isArray(message.content)
            ? (message.content as unknown as Array<{data?: {code?: string; message: string}; type: string}>).filter(
                  (part) => part.type === 'data-run-error'
              )
            : []
    );

describe('AiHubRuntimeProvider when the server refuses the turn with 403', () => {
    beforeEach(() => {
        useWorkspaceStore.setState({currentWorkspaceId: 1});

        aiHubRunStateStore.getState().reset();
        aiHubChatsStore.getState().reset();
        aiHubChatsStore.getState().setCurrentChatId(5);
        aiHubStore.setState({chatId: 'thread-1', messages: []});
        aiChatRetryableErrorStore.setState({currentError: undefined});

        vi.spyOn(globalThis, 'fetch').mockImplementation(
            async () =>
                new Response(JSON.stringify({error: 'Workspace is not accessible to the current user'}), {
                    headers: {'Content-Type': 'application/json'},
                    status: 403,
                })
        );
    });

    afterEach(() => {
        vi.restoreAllMocks();
        vi.clearAllMocks();
    });

    it('shows a permission run error in the chat instead of a failure toast or a retry banner', async () => {
        render(
            <QueryClientProvider client={new QueryClient()}>
                <AiHubRuntimeProvider>child</AiHubRuntimeProvider>
            </QueryClientProvider>
        );

        await act(async () => {
            await getRuntimeOptions().onNew({
                content: [{text: 'Build me a workflow', type: 'text'}],
            } as unknown as AppendMessage);
        });

        expect(runErrorParts(aiHubStore.getState().messages)).toEqual([
            {
                data: {
                    code: 'ACCESS_DENIED',
                    message:
                        "You don't have permission to do this in this workspace. Ask a workspace administrator for access.",
                },
                type: 'data-run-error',
            },
        ]);
        expect(toast.error).not.toHaveBeenCalled();
        expect(aiChatRetryableErrorStore.getState().currentError).toBeUndefined();
        expect(getRuntimeOptions().isRunning).toBe(false);
        expect(aiHubChatsStore.getState().chatActivity['thread-1']).toBeUndefined();
        expect(patchChat).not.toHaveBeenCalled();
        expect(generateAiHubChatTitle).not.toHaveBeenCalled();
    });
});
