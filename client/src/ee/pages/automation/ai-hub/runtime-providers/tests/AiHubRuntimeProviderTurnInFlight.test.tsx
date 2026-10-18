import {aiHubChatsStore} from '@/ee/pages/automation/ai-hub/chats/stores/useAiHubChatsStore';
import {aiHubRunStateStore} from '@/ee/pages/automation/ai-hub/runtime-providers/stores/useAiHubRunStateStore';
import {aiHubStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {AppendMessage, ThreadMessageLike, useExternalStoreRuntime} from '@assistant-ui/react';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {act, render} from '@testing-library/react';
import {ReactNode} from 'react';
import {toast} from 'sonner';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {AiHubRuntimeProvider, TURN_IN_FLIGHT_MESSAGE} from '../AiHubRuntimeProvider';

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

function getRuntimeOptions() {
    return vi.mocked(useExternalStoreRuntime).mock.lastCall![0] as unknown as {
        isRunning: boolean;
        messages: ThreadMessageLike[];
        onNew: (message: AppendMessage) => Promise<void>;
    };
}

describe('AiHubRuntimeProvider when the server answers 409 TURN_IN_FLIGHT', () => {
    beforeEach(() => {
        useWorkspaceStore.setState({currentWorkspaceId: 1});

        aiHubRunStateStore.getState().reset();
        aiHubChatsStore.getState().reset();
        aiHubChatsStore.getState().setCurrentChatId(5);
        aiHubStore.setState({chatId: 'thread-1', messages: [{content: 'earlier', role: 'user'}]});

        vi.spyOn(globalThis, 'fetch').mockImplementation(
            async () =>
                new Response(JSON.stringify({error: 'TURN_IN_FLIGHT'}), {
                    headers: {'Content-Type': 'application/json'},
                    status: 409,
                })
        );
    });

    afterEach(() => {
        vi.restoreAllMocks();
        vi.clearAllMocks();
    });

    it('rolls back the optimistic turn, hands the text back to the composer and says a turn is already running', async () => {
        render(
            <QueryClientProvider client={new QueryClient()}>
                <AiHubRuntimeProvider>child</AiHubRuntimeProvider>
            </QueryClientProvider>
        );

        await act(async () => {
            await getRuntimeOptions().onNew({
                content: [{text: 'second turn', type: 'text'}],
            } as unknown as AppendMessage);
        });

        expect(aiHubStore.getState().messages).toEqual([{content: 'earlier', role: 'user'}]);
        expect(getRuntimeOptions().isRunning).toBe(false);
        expect(hoisted.setComposerText).toHaveBeenCalledWith('second turn');
        expect(toast.info).toHaveBeenCalledWith(TURN_IN_FLIGHT_MESSAGE);
        expect(toast.error).not.toHaveBeenCalled();
        expect(aiHubChatsStore.getState().chatActivity['thread-1']).toBeUndefined();
    });
});
