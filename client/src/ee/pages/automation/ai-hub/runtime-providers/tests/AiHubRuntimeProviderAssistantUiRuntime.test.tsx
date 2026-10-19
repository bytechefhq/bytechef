import {AiHubChatMessageI, getChatMessages} from '@/ee/pages/automation/ai-hub/chats/api/chats.api';
import {useSwitchChat} from '@/ee/pages/automation/ai-hub/chats/hooks/useSwitchChat';
import {aiHubChatsStore} from '@/ee/pages/automation/ai-hub/chats/stores/useAiHubChatsStore';
import {aiHubRunStateStore} from '@/ee/pages/automation/ai-hub/runtime-providers/stores/useAiHubRunStateStore';
import {aiHubStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {aiChatToolCallStore} from '@/shared/components/ai-chat/stores/useAiChatToolCallStore';
import {EventType} from '@ag-ui/core';
import {type AssistantRuntime} from '@assistant-ui/react';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {act, render, renderHook, waitFor} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {AiHubRuntimeProvider} from '../AiHubRuntimeProvider';

const hoisted = vi.hoisted(() => ({
    runtimeRef: {current: undefined as AssistantRuntime | undefined},
    truncateMock: vi.fn(),
}));

vi.mock('@assistant-ui/react', async () => {
    const actual = await vi.importActual<typeof import('@assistant-ui/react')>('@assistant-ui/react');

    return {
        ...actual,
        useExternalStoreRuntime: (...args: Parameters<typeof actual.useExternalStoreRuntime>) => {
            const runtime = actual.useExternalStoreRuntime(...args);

            hoisted.runtimeRef.current = runtime;

            return runtime;
        },
    };
});

vi.mock('@/ee/pages/automation/ai-hub/chats/api/chats.api', async () => {
    const actual = await vi.importActual<typeof import('@/ee/pages/automation/ai-hub/chats/api/chats.api')>(
        '@/ee/pages/automation/ai-hub/chats/api/chats.api'
    );

    return {
        ...actual,
        createAiHubChat: vi.fn(),
        generateAiHubChatTitle: vi.fn(() => Promise.resolve()),
        getChatArtifacts: vi.fn(() => Promise.resolve([])),
        getChatMessages: vi.fn(),
        getToolApprovals: vi.fn(() => Promise.resolve([])),
        patchChat: vi.fn(() => Promise.resolve()),
    };
});

vi.mock('@/ee/pages/automation/ai-hub/chats/hooks/useTruncateChatMessages', () => ({
    useTruncateAiHubChatMessagesMutation: () => ({mutateAsync: hoisted.truncateMock}),
}));

vi.mock('react-router-dom', () => ({
    useNavigate: () => vi.fn(),
}));

vi.mock('sonner', () => ({
    toast: {error: vi.fn(), info: vi.fn()},
}));

const toolEvents = (toolName: string, response: unknown) =>
    JSON.stringify([
        {arguments: '{}', id: `call-${toolName}`, kind: 'call', name: toolName},
        {id: `call-${toolName}`, kind: 'result', name: toolName, response: JSON.stringify(response)},
    ]);

const SERVER_ROWS: AiHubChatMessageI[] = [
    {
        content: 'Bind a Slack connection',
        id: '6f1c2a3b-4d5e-4f60-8a71-b2c3d4e5f601',
        role: 'USER',
        timestamp: '2026-10-19T08:00:00Z',
        toolEventsJson: toolEvents('listConnections', []),
    },
    {
        content: 'Pick one:',
        id: '6f1c2a3b-4d5e-4f60-8a71-b2c3d4e5f602',
        role: 'ASSISTANT',
        timestamp: '2026-10-19T08:00:01Z',
        toolEventsJson: toolEvents('selectConnection', {
            awaitingAnswer: true,
            componentLabel: 'Slack',
            componentName: 'slack',
            kind: 'select-connection',
        }),
    },
    {
        content: 'User picked: Slack Engineering (ID: 1051)',
        id: '6f1c2a3b-4d5e-4f60-8a71-b2c3d4e5f603',
        role: 'USER',
        timestamp: '2026-10-19T08:00:02Z',
        toolEventsJson: null,
    },
    {
        content: 'Bound.',
        id: '6f1c2a3b-4d5e-4f60-8a71-b2c3d4e5f604',
        role: 'ASSISTANT',
        timestamp: '2026-10-19T08:00:03Z',
        toolEventsJson: null,
    },
    {
        content: 'Now rename it',
        id: '6f1c2a3b-4d5e-4f60-8a71-b2c3d4e5f605',
        role: 'USER',
        timestamp: '2026-10-19T08:00:04Z',
        toolEventsJson: null,
    },
    {
        content: 'Renamed.',
        id: '6f1c2a3b-4d5e-4f60-8a71-b2c3d4e5f606',
        role: 'ASSISTANT',
        timestamp: '2026-10-19T08:00:05Z',
        toolEventsJson: null,
    },
];

let sentRunBodies: Array<{messages: Array<{content: string; id: string; role: string}>}> = [];

const finishedRunResponse = () => {
    const body = [
        {runId: 'run-1', threadId: 'thread-9', type: EventType.RUN_STARTED},
        {runId: 'run-1', threadId: 'thread-9', type: EventType.RUN_FINISHED},
    ]
        .map((event) => `data: ${JSON.stringify(event)}\n\n`)
        .join('');

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

const getRuntime = () => hoisted.runtimeRef.current!;

const loadChatAndRenderProvider = async () => {
    const {result} = renderHook(() => useSwitchChat());

    await act(async () => {
        await result.current({
            autoTitled: false,
            createdAt: '2026-10-19T08:00:00Z',
            environmentId: 0,
            id: 9,
            isOwner: true,
            lastPreview: null,
            messageCount: SERVER_ROWS.length,
            ownerUserId: 1,
            status: 'ACTIVE',
            threadId: 'thread-9',
            title: 'Slack',
            updatedAt: '2026-10-19T08:00:05Z',
            userId: 1,
            workspaceId: 1,
        });
    });

    render(
        <QueryClientProvider client={new QueryClient()}>
            <AiHubRuntimeProvider>child</AiHubRuntimeProvider>
        </QueryClientProvider>
    );
};

describe('AiHubRuntimeProvider driven through the assistant-ui runtime', () => {
    beforeEach(() => {
        sentRunBodies = [];

        useWorkspaceStore.setState({currentWorkspaceId: 1});

        aiHubRunStateStore.getState().reset();
        aiHubChatsStore.getState().reset();
        aiChatToolCallStore.setState({order: [], toolCalls: {}});
        aiHubStore.setState({chatId: 'thread-source', messages: []});

        vi.mocked(getChatMessages).mockResolvedValue(SERVER_ROWS);

        hoisted.truncateMock.mockReset().mockResolvedValue({truncateAiHubChatMessages: 4});

        vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
            if (!String(input).endsWith('/internal/ai/chat/ai_hub')) {
                throw new Error('offline');
            }

            sentRunBodies.push(JSON.parse(String(init?.body)));

            return finishedRunResponse();
        });
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('gives every runtime message the store id, never a fallback id', async () => {
        await loadChatAndRenderProvider();

        const runtimeMessageIds = getRuntime()
            .thread.getState()
            .messages.map((message) => message.id);

        expect(runtimeMessageIds).toEqual(aiHubStore.getState().messages.map((message) => message.id));
        expect(runtimeMessageIds.some((id) => id.startsWith('__external_store_fallback_'))).toBe(false);
    });

    it('truncates at the server row of the second user message when the user edits it', async () => {
        await loadChatAndRenderProvider();

        const secondUserMessage = getRuntime()
            .thread.getState()
            .messages.filter((message) => message.role === 'user')[1];

        const messageRuntime = getRuntime().thread.getMessageById(secondUserMessage.id);

        await act(async () => {
            messageRuntime.composer.beginEdit();
            messageRuntime.composer.setText('User picked: Slack Sales (ID: 1052)');
            messageRuntime.composer.send();
        });

        await waitFor(() => expect(sentRunBodies).toHaveLength(1));

        expect(hoisted.truncateMock).toHaveBeenCalledWith({
            id: '9',
            keepMessage: false,
            messageId: '6f1c2a3b-4d5e-4f60-8a71-b2c3d4e5f603',
            workspaceId: '1',
        });

        const editedMessage = aiHubStore
            .getState()
            .messages.find(
                (message) => message.role === 'user' && message.content === 'User picked: Slack Sales (ID: 1052)'
            );

        expect(editedMessage?.id).not.toBe('6f1c2a3b-4d5e-4f60-8a71-b2c3d4e5f603');
        expect(sentRunBodies[0].messages).toEqual([
            {content: 'User picked: Slack Sales (ID: 1052)', id: editedMessage?.id, role: 'user'},
        ]);
    });

    it('keeps the user row and re-sends it under its server id when the user reloads the last reply', async () => {
        await loadChatAndRenderProvider();

        await act(async () => {
            getRuntime().thread.getMessageById('6f1c2a3b-4d5e-4f60-8a71-b2c3d4e5f606').reload();
        });

        await waitFor(() => expect(sentRunBodies).toHaveLength(1));

        expect(hoisted.truncateMock).toHaveBeenCalledWith({
            id: '9',
            keepMessage: true,
            messageId: '6f1c2a3b-4d5e-4f60-8a71-b2c3d4e5f605',
            workspaceId: '1',
        });
        expect(sentRunBodies[0].messages).toEqual([
            {content: 'Now rename it', id: '6f1c2a3b-4d5e-4f60-8a71-b2c3d4e5f605', role: 'user'},
        ]);
    });
});
