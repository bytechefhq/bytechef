import {createAiHubChat} from '@/ee/pages/automation/ai-hub/chats/api/chats.api';
import {aiHubChatsStore} from '@/ee/pages/automation/ai-hub/chats/stores/useAiHubChatsStore';
import {aiHubRunStateStore} from '@/ee/pages/automation/ai-hub/runtime-providers/stores/useAiHubRunStateStore';
import {aiHubStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {AppendMessage, useExternalStoreRuntime} from '@assistant-ui/react';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {act, render} from '@testing-library/react';
import {ReactNode} from 'react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {AiHubRuntimeProvider} from '../AiHubRuntimeProvider';

const hoisted = vi.hoisted(() => ({setComposerText: vi.fn()}));

vi.mock('@assistant-ui/react', () => ({
    AssistantRuntimeProvider: ({children}: {children: ReactNode}) => <div>{children}</div>,
    useExternalStoreRuntime: vi.fn(() => ({thread: {composer: {setText: hoisted.setComposerText}}})),
}));

vi.mock('@/ee/pages/automation/ai-hub/chats/api/chats.api', () => ({
    createAiHubChat: vi.fn(),
    generateAiHubChatTitle: vi.fn(() => Promise.resolve()),
    patchChat: vi.fn(() => Promise.resolve()),
}));

vi.mock('sonner', () => ({
    toast: {error: vi.fn(), info: vi.fn()},
}));

function getOnNew() {
    return (
        vi.mocked(useExternalStoreRuntime).mock.lastCall![0] as unknown as {
            onNew: (message: AppendMessage) => Promise<void>;
        }
    ).onNew;
}

describe('AiHubRuntimeProvider before the workspace id is known', () => {
    beforeEach(() => {
        aiHubRunStateStore.getState().reset();
        aiHubChatsStore.getState().reset();
        aiHubStore.setState({chatId: 'thread-1', messages: []});

        vi.spyOn(globalThis, 'fetch').mockImplementation(async () => new Response('{}', {status: 200}));
    });

    afterEach(() => {
        useWorkspaceStore.setState({currentWorkspaceId: 1});

        vi.restoreAllMocks();
        vi.clearAllMocks();
    });

    it.each([undefined, 0])(
        'sends nothing and hands the text back to the composer for workspace id %s',
        async (workspaceId) => {
            useWorkspaceStore.setState({currentWorkspaceId: workspaceId as unknown as number});

            render(
                <QueryClientProvider client={new QueryClient()}>
                    <AiHubRuntimeProvider>child</AiHubRuntimeProvider>
                </QueryClientProvider>
            );

            await act(async () => {
                await getOnNew()({content: [{text: 'first message', type: 'text'}]} as unknown as AppendMessage);
            });

            expect(createAiHubChat).not.toHaveBeenCalled();
            expect(
                vi.mocked(globalThis.fetch).mock.calls.filter(([url]) => String(url).includes('/ai/chat/ai_hub'))
            ).toEqual([]);
            expect(aiHubStore.getState().messages).toEqual([]);
            expect(hoisted.setComposerText).toHaveBeenCalledWith('first message');
        }
    );
});
