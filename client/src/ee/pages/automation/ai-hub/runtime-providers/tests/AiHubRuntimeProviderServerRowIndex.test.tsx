import {aiHubChatsStore} from '@/ee/pages/automation/ai-hub/chats/stores/useAiHubChatsStore';
import {aiHubRunStateStore} from '@/ee/pages/automation/ai-hub/runtime-providers/stores/useAiHubRunStateStore';
import {getServerRowIndex, withServerRowIndex} from '@/ee/pages/automation/ai-hub/stores/serverRowIndex';
import {aiHubStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {type AgentSubscriber} from '@ag-ui/client';
import {EventType} from '@ag-ui/core';
import {AppendMessage, ThreadMessageLike, useExternalStoreRuntime} from '@assistant-ui/react';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {act, render} from '@testing-library/react';
import {ReactNode} from 'react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {AiHubRuntimeProvider, buildAiHubSubscriber} from '../AiHubRuntimeProvider';

const hoisted = vi.hoisted(() => ({setComposerText: vi.fn(), truncateMock: vi.fn()}));

vi.mock('@assistant-ui/react', () => ({
    AssistantRuntimeProvider: ({children}: {children: ReactNode}) => <div>{children}</div>,
    useExternalStoreRuntime: vi.fn(() => ({thread: {composer: {setText: hoisted.setComposerText}}})),
}));

vi.mock('@/ee/pages/automation/ai-hub/chats/api/chats.api', () => ({
    createAiHubChat: vi.fn(),
    generateAiHubChatTitle: vi.fn(() => Promise.resolve()),
    patchChat: vi.fn(() => Promise.resolve()),
}));

vi.mock('@/ee/pages/automation/ai-hub/chats/hooks/useTruncateChatMessages', () => ({
    useTruncateAiHubChatMessagesMutation: () => ({mutateAsync: hoisted.truncateMock}),
}));

vi.mock('sonner', () => ({
    toast: {error: vi.fn(), info: vi.fn()},
}));

function getRuntimeOptions() {
    return vi.mocked(useExternalStoreRuntime).mock.lastCall![0] as unknown as {
        onEdit: (message: AppendMessage) => Promise<void>;
        onReload: (parentId: string | null) => Promise<void>;
    };
}

const SELECT_CONNECTION_PART = {
    data: {componentLabel: 'Slack', componentName: 'slack', kind: 'select-connection'},
    type: 'data-select-connection',
};

const RELOADED_TRANSCRIPT: ThreadMessageLike[] = [
    withServerRowIndex({content: 'Bind a Slack connection', role: 'user'}, 0),
    {
        content: [{args: {}, argsText: '{}', toolCallId: 'call-list', toolName: 'listConnections', type: 'tool-call'}],
        role: 'assistant',
    } as ThreadMessageLike,
    withServerRowIndex({content: 'Pick one:', role: 'assistant'}, 1),
    {content: [SELECT_CONNECTION_PART], role: 'assistant'} as ThreadMessageLike,
    withServerRowIndex({content: 'User picked: Slack Engineering (ID: 1051)', role: 'user'}, 2),
    withServerRowIndex({content: 'Bound.', role: 'assistant'}, 3),
    withServerRowIndex({content: 'Now rename it', role: 'user'}, 4),
    withServerRowIndex({content: 'Renamed.', role: 'assistant'}, 5),
];

const renderProvider = () =>
    render(
        <QueryClientProvider client={new QueryClient()}>
            <AiHubRuntimeProvider>child</AiHubRuntimeProvider>
        </QueryClientProvider>
    );

describe('AiHubRuntimeProvider server row index', () => {
    beforeEach(() => {
        useWorkspaceStore.setState({currentWorkspaceId: 1});

        aiHubRunStateStore.getState().reset();
        aiHubChatsStore.getState().reset();
        aiHubChatsStore.getState().setCurrentChatId(9);
        aiHubStore.setState({chatId: 'thread-9', messages: RELOADED_TRANSCRIPT, nextServerRowIndex: 6});

        hoisted.truncateMock.mockReset().mockResolvedValue({});

        vi.spyOn(globalThis, 'fetch').mockRejectedValue(new Error('offline'));
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('truncates at the edited user message server row, not its client row', async () => {
        renderProvider();

        await act(async () => {
            await getRuntimeOptions().onEdit({
                content: [{text: 'Now rename it to Leads', type: 'text'}],
                sourceId: '6',
            } as unknown as AppendMessage);
        });

        expect(hoisted.truncateMock).toHaveBeenCalledWith(expect.objectContaining({fromMessageIndex: 4}));
        expect(getServerRowIndex(aiHubStore.getState().messages[6])).toBe(4);
        expect(aiHubStore.getState().nextServerRowIndex).toBe(5);
    });

    it('truncates from the reloaded reply parent user row, which the re-run persists again', async () => {
        renderProvider();

        await act(async () => {
            await getRuntimeOptions().onReload('6');
        });

        expect(hoisted.truncateMock).toHaveBeenCalledWith(expect.objectContaining({fromMessageIndex: 4}));
        expect(aiHubStore.getState().nextServerRowIndex).toBe(5);
    });
});

describe('live server row tagging', () => {
    beforeEach(() => {
        aiHubStore.setState({
            chatId: 'thread-9',
            messages: [
                withServerRowIndex({content: 'Bind a Slack connection', role: 'user'}, 0),
                {content: '', role: 'assistant'},
            ],
            nextServerRowIndex: 1,
        });
    });

    it('tags each assistant bubble with text, and no tool-only bubble', () => {
        const subscriber = buildAiHubSubscriber({
            addMessage: aiHubStore.getState().addMessage,
            appendToLastAssistantMessage: aiHubStore.getState().appendToLastAssistantMessage,
            assistantMessageIndex: 1,
            chatId: 'thread-9',
            getLastUserMessage: () => 'Bind a Slack connection',
        });

        const baseParams = {agent: {}, input: {}, messages: [], state: {}};

        subscriber.onToolCallStartEvent!({
            ...baseParams,
            event: {toolCallId: 'call-list', toolCallName: 'listConnections', type: EventType.TOOL_CALL_START},
        } as unknown as Parameters<NonNullable<AgentSubscriber['onToolCallStartEvent']>>[0]);

        subscriber.onTextMessageStartEvent!({
            ...baseParams,
            event: {messageId: 'm', type: EventType.TEXT_MESSAGE_START},
        } as never);
        subscriber.onTextMessageContentEvent!({
            ...baseParams,
            event: {delta: 'Pick one:', messageId: 'm', type: EventType.TEXT_MESSAGE_CONTENT},
            textMessageBuffer: '',
        } as never);

        const messages = aiHubStore.getState().messages;

        expect(getServerRowIndex(messages[1])).toBeUndefined();
        expect(getServerRowIndex(messages[2])).toBe(1);
        expect(aiHubStore.getState().nextServerRowIndex).toBe(2);
    });
});
