import {generateAiHubChatTitle, patchChat} from '@/ee/pages/automation/ai-hub/chats/api/chats.api';
import {AiHubChatsKeys} from '@/ee/pages/automation/ai-hub/chats/hooks/useChats';
import {aiHubChatsStore} from '@/ee/pages/automation/ai-hub/chats/stores/useAiHubChatsStore';
import {aiHubRunStateStore} from '@/ee/pages/automation/ai-hub/runtime-providers/stores/useAiHubRunStateStore';
import {aiHubStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {EventType} from '@ag-ui/core';
import {AppendMessage, useExternalStoreRuntime} from '@assistant-ui/react';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {act, render} from '@testing-library/react';
import {ReactNode} from 'react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {AiHubRuntimeProvider} from '../AiHubRuntimeProvider';

vi.mock('@assistant-ui/react', () => ({
    AssistantRuntimeProvider: ({children}: {children: ReactNode}) => <div>{children}</div>,
    useExternalStoreRuntime: vi.fn(() => ({thread: {composer: {setText: vi.fn()}}})),
}));

vi.mock('@/ee/pages/automation/ai-hub/chats/api/chats.api', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/ee/pages/automation/ai-hub/chats/api/chats.api')>()),
    createAiHubChat: vi.fn(),
    generateAiHubChatTitle: vi.fn(() => Promise.resolve()),
    patchChat: vi.fn(() => Promise.resolve()),
}));

vi.mock('sonner', () => ({
    toast: {error: vi.fn(), info: vi.fn()},
}));

const streamResponse = (events: Array<Record<string, unknown>>) => {
    const body = events.map((event) => `data: ${JSON.stringify(event)}\n\n`).join('');

    return new Response(
        new ReadableStream<Uint8Array>({
            start(controller) {
                controller.enqueue(new TextEncoder().encode(body));
                controller.close();
            },
        }),
        {headers: {'Content-Type': 'text/event-stream'}, status: 200}
    );
};

const RUN_STARTED = {runId: 'run-1', threadId: 'thread-1', type: EventType.RUN_STARTED};

const RUN_FINISHED = {runId: 'run-1', threadId: 'thread-1', type: EventType.RUN_FINISHED};

const sendTurn = async (queryClient: QueryClient, text: string) => {
    render(
        <QueryClientProvider client={queryClient}>
            <AiHubRuntimeProvider>child</AiHubRuntimeProvider>
        </QueryClientProvider>
    );

    const runtimeOptions = vi.mocked(useExternalStoreRuntime).mock.lastCall![0] as unknown as {
        onNew: (message: AppendMessage) => Promise<void>;
    };

    await act(async () => {
        await runtimeOptions.onNew({content: [{text, type: 'text'}]} as unknown as AppendMessage);
    });
};

describe('AiHubRuntimeProvider turns around workflow chats', () => {
    beforeEach(() => {
        useWorkspaceStore.setState({currentWorkspaceId: 1});
        environmentStore.setState({currentEnvironmentId: 2});

        aiHubRunStateStore.getState().reset();
        aiHubChatsStore.getState().reset();
        aiHubChatsStore.getState().setCurrentChatId(5);
        aiHubStore.setState({chatId: 'thread-1', messages: [{content: 'earlier', role: 'user'}]});
    });

    afterEach(() => {
        vi.restoreAllMocks();
        vi.clearAllMocks();
    });

    it('patches the preview but does not generate a title for a workflow chat', async () => {
        const queryClient = new QueryClient();

        queryClient.setQueryData(AiHubChatsKeys.list(1, 2, 'ACTIVE'), [{id: 5, kind: 'WORKFLOW_CHAT'}]);

        vi.spyOn(globalThis, 'fetch').mockImplementation(async () =>
            streamResponse([
                RUN_STARTED,
                {messageId: 'm-1', role: 'assistant', type: EventType.TEXT_MESSAGE_START},
                {delta: 'Hello from the workflow', messageId: 'm-1', type: EventType.TEXT_MESSAGE_CONTENT},
                {messageId: 'm-1', type: EventType.TEXT_MESSAGE_END},
                RUN_FINISHED,
            ])
        );

        await sendTurn(queryClient, 'Hi workflow');

        expect(patchChat).toHaveBeenCalledWith({
            chatId: 5,
            patch: {lastPreview: 'Hi workflow', messageCount: 3},
            workspaceId: 1,
        });
        expect(generateAiHubChatTitle).not.toHaveBeenCalled();
    });

    it('switches to the opened workflow chat without filing the turn under it', async () => {
        const queryClient = new QueryClient();

        vi.spyOn(globalThis, 'fetch').mockImplementation(async () =>
            streamResponse([
                RUN_STARTED,
                {toolCallId: 'call-1', toolCallName: 'openWorkflowChatTab', type: EventType.TOOL_CALL_START},
                {toolCallId: 'call-1', type: EventType.TOOL_CALL_END},
                {
                    content: JSON.stringify({chatId: 77, opened: true, threadId: 'thread-77', title: 'Acme — Support'}),
                    messageId: 'tool-1',
                    role: 'tool',
                    toolCallId: 'call-1',
                    type: EventType.TOOL_CALL_RESULT,
                },
                RUN_FINISHED,
            ])
        );

        await sendTurn(queryClient, 'Open the support chat');

        expect(aiHubChatsStore.getState().currentChatId).toBe(77);
        expect(aiHubStore.getState().chatId).toBe('thread-77');
        expect(patchChat).not.toHaveBeenCalled();
        expect(generateAiHubChatTitle).not.toHaveBeenCalled();
    });
});
