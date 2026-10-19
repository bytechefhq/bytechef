import {aiHubChatsStore} from '@/ee/pages/automation/ai-hub/chats/stores/useAiHubChatsStore';
import {aiHubRunStateStore} from '@/ee/pages/automation/ai-hub/runtime-providers/stores/useAiHubRunStateStore';
import {aiHubStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {aiChatToolCallStore} from '@/shared/components/ai-chat/stores/useAiChatToolCallStore';
import {type AgentSubscriber} from '@ag-ui/client';
import {EventType} from '@ag-ui/core';
import {AppendMessage, ThreadMessageLike, useExternalStoreRuntime} from '@assistant-ui/react';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {act, render} from '@testing-library/react';
import {ReactNode} from 'react';
import {toast} from 'sonner';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {
    AiHubRuntimeProvider,
    EDIT_NOT_SENT_MESSAGE,
    buildAiHubSubscriber,
    findReloadedUserMessageIndex,
} from '../AiHubRuntimeProvider';

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

const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/;

const LOADED_TRANSCRIPT: ThreadMessageLike[] = [
    {content: 'Bind a Slack connection', id: 'event-1', role: 'user'},
    {
        content: [{args: {}, argsText: '{}', toolCallId: 'call-list', toolName: 'listConnections', type: 'tool-call'}],
        id: 'event-1-tools',
        role: 'assistant',
    } as ThreadMessageLike,
    {
        content: [
            {text: 'Pick one:', type: 'text'},
            {
                data: {componentName: 'slack', kind: 'select-connection', pickedLabel: 'Slack Engineering'},
                type: 'data-select-connection',
            },
        ],
        id: 'event-2',
        role: 'assistant',
    } as ThreadMessageLike,
    {content: 'User picked: Slack Engineering (ID: 1051)', id: 'event-3', role: 'user'},
    {content: 'Bound.', id: 'event-4', role: 'assistant'},
    {content: 'Now rename it', id: 'event-5', role: 'user'},
    {content: 'Renamed.', id: 'event-6', role: 'assistant'},
    {
        content: [
            {
                args: {name: 'Leads', projectId: '5', projectWorkflowId: 12, workflowId: 'wf-1'},
                argsText: '{}',
                result: {opened: true},
                toolCallId: 'rehydrated-artifact-1',
                toolName: 'openWorkflowTab',
                type: 'tool-call',
            },
        ],
        id: 'artifact-links',
        role: 'assistant',
    } as ThreadMessageLike,
];

const LIVE_SPLIT_TURN: ThreadMessageLike[] = [
    {content: 'Build a lead workflow', id: 'live-user', role: 'user'},
    {content: "I'll build it.", id: 'live-text-1', role: 'assistant'},
    {content: 'Built it.', id: 'live-text-2', role: 'assistant'},
];

interface SentMessageI {
    content: string;
    id: string;
    role: string;
}

let sentRunBodies: Array<{messages: SentMessageI[]}> = [];

function toEventStream(events: Array<Record<string, unknown>>) {
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
}

function respondToRuns(respond: () => Promise<Response>) {
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
        if (!String(input).endsWith('/internal/ai/chat/ai_hub')) {
            throw new Error('offline');
        }

        sentRunBodies.push(JSON.parse(String(init?.body)));

        return respond();
    });
}

const finishedRun = async () =>
    toEventStream([
        {runId: 'run-1', threadId: 'thread-9', type: EventType.RUN_STARTED},
        {runId: 'run-1', threadId: 'thread-9', type: EventType.RUN_FINISHED},
    ]);

const failedRun = async () =>
    toEventStream([
        {runId: 'run-1', threadId: 'thread-9', type: EventType.RUN_STARTED},
        {message: 'The selected model is unavailable', type: EventType.RUN_ERROR},
    ]);

function getRuntimeOptions() {
    return vi.mocked(useExternalStoreRuntime).mock.lastCall![0] as unknown as {
        onEdit: (message: AppendMessage) => Promise<void>;
        onNew: (message: AppendMessage) => Promise<void>;
        onReload: (parentId: string | null) => Promise<void>;
    };
}

const editMessage = (sourceId: string, text: string) =>
    act(async () => {
        await getRuntimeOptions().onEdit({
            content: [{text, type: 'text'}],
            sourceId,
        } as unknown as AppendMessage);
    });

const reloadReply = (parentId: string | null) =>
    act(async () => {
        await getRuntimeOptions().onReload(parentId);
    });

const renderProvider = () =>
    render(
        <QueryClientProvider client={new QueryClient()}>
            <AiHubRuntimeProvider>child</AiHubRuntimeProvider>
        </QueryClientProvider>
    );

const threadIds = () => aiHubStore.getState().messages.map((message) => message.id);

describe('AiHubRuntimeProvider edit and reload', () => {
    beforeEach(() => {
        sentRunBodies = [];

        useWorkspaceStore.setState({currentWorkspaceId: 1});

        aiHubRunStateStore.getState().reset();
        aiHubChatsStore.getState().reset();
        aiHubChatsStore.getState().setCurrentChatId(9);
        aiChatToolCallStore.setState({order: [], toolCalls: {}});
        aiHubStore.setState({chatId: 'thread-9', messages: LOADED_TRANSCRIPT});

        hoisted.truncateMock.mockReset().mockResolvedValue({truncateAiHubChatMessages: 2});
        hoisted.setComposerText.mockReset();

        respondToRuns(finishedRun);
    });

    afterEach(() => {
        vi.restoreAllMocks();
        vi.mocked(toast.error).mockClear();
        vi.mocked(toast.info).mockClear();
    });

    describe('edit', () => {
        it('truncates the server from the edited user row and sends the new text under a new id', async () => {
            renderProvider();

            await editMessage('event-5', 'Now rename it to Leads');

            expect(hoisted.truncateMock).toHaveBeenCalledWith({
                id: '9',
                keepMessage: false,
                messageId: 'event-5',
                workspaceId: '1',
            });

            const editedMessage = aiHubStore.getState().messages[5];

            expect(threadIds().slice(0, 5)).toEqual(['event-1', 'event-1-tools', 'event-2', 'event-3', 'event-4']);
            expect(editedMessage).toMatchObject({content: 'Now rename it to Leads', role: 'user'});
            expect(editedMessage.id).toMatch(UUID_PATTERN);
            expect(sentRunBodies).toHaveLength(1);
            expect(sentRunBodies[0].messages).toEqual([
                {content: 'Now rename it to Leads', id: editedMessage.id, role: 'user'},
            ]);
        });

        it('leaves the thread and the server alone when the server reports a turn in flight', async () => {
            hoisted.truncateMock.mockRejectedValue(new Error('A turn is already running in this chat.'));

            renderProvider();

            await editMessage('event-5', 'Now rename it to Leads');

            expect(aiHubStore.getState().messages).toBe(LOADED_TRANSCRIPT);
            expect(sentRunBodies).toEqual([]);
            expect(toast.error).toHaveBeenCalledWith(expect.stringContaining('A turn is already running'));
        });

        it('refuses an earlier user message the server never saved, without running', async () => {
            hoisted.truncateMock.mockResolvedValue({truncateAiHubChatMessages: null});

            renderProvider();

            await editMessage('event-3', 'User picked: Slack Sales (ID: 1052)');

            expect(aiHubStore.getState().messages).toBe(LOADED_TRANSCRIPT);
            expect(sentRunBodies).toEqual([]);
            expect(toast.error).toHaveBeenCalledWith('This message was not saved, so it cannot be edited.');
        });

        it('refuses a message id that is not a user message in the thread', async () => {
            renderProvider();

            await editMessage('__external_store_fallback_5', 'Now rename it to Leads');

            expect(hoisted.truncateMock).not.toHaveBeenCalled();
            expect(toast.error).toHaveBeenCalledWith('Could not resolve edited message');
        });

        it('keeps the edited message in the thread, out of the composer, when the re-run meets a turn in flight', async () => {
            respondToRuns(
                async () =>
                    new Response(JSON.stringify({error: 'TURN_IN_FLIGHT'}), {
                        headers: {'Content-Type': 'application/json'},
                        status: 409,
                    })
            );

            renderProvider();

            await editMessage('event-5', 'Now rename it to Leads');

            expect(aiHubStore.getState().messages.at(-1)).toMatchObject({
                content: 'Now rename it to Leads',
                role: 'user',
            });
            expect(hoisted.setComposerText).not.toHaveBeenCalled();
        });
    });

    describe('reload', () => {
        it('regenerates the reply to the nearest user message when the parent is an assistant bubble', async () => {
            aiHubStore.setState({messages: [...LOADED_TRANSCRIPT, ...LIVE_SPLIT_TURN]});

            renderProvider();

            await reloadReply('live-text-1');

            expect(hoisted.truncateMock).toHaveBeenCalledWith({
                id: '9',
                keepMessage: true,
                messageId: 'live-user',
                workspaceId: '1',
            });
            expect(threadIds().slice(0, LOADED_TRANSCRIPT.length + 1)).toEqual([
                ...LOADED_TRANSCRIPT.map((message) => message.id),
                'live-user',
            ]);
            expect(threadIds()).not.toContain('live-text-1');
            expect(sentRunBodies[0].messages).toEqual([
                {content: 'Build a lead workflow', id: 'live-user', role: 'user'},
            ]);
        });

        it('walks back over a tool-only row to the user message', async () => {
            renderProvider();

            await reloadReply('event-1-tools');

            expect(hoisted.truncateMock).toHaveBeenCalledWith(
                expect.objectContaining({keepMessage: true, messageId: 'event-1'})
            );
            expect(threadIds().slice(0, 1)).toEqual(['event-1']);
            expect(sentRunBodies[0].messages).toEqual([
                {content: 'Bind a Slack connection', id: 'event-1', role: 'user'},
            ]);
        });

        it('walks back over a data row to the user message', async () => {
            aiHubStore.setState({
                messages: [
                    ...LOADED_TRANSCRIPT,
                    {content: 'Rename it back', id: 'live-user', role: 'user'},
                    {
                        content: [{data: {message: 'Provider overloaded'}, type: 'data-run-error'}],
                        id: 'live-error',
                        role: 'assistant',
                    } as ThreadMessageLike,
                ],
            });

            renderProvider();

            await reloadReply('live-error');

            expect(hoisted.truncateMock).toHaveBeenCalledWith(
                expect.objectContaining({keepMessage: true, messageId: 'live-user'})
            );
        });

        it('refuses a reload with no parent instead of truncating the whole chat', async () => {
            renderProvider();

            await reloadReply(null);

            expect(hoisted.truncateMock).not.toHaveBeenCalled();
            expect(sentRunBodies).toEqual([]);
            expect(aiHubStore.getState().messages).toBe(LOADED_TRANSCRIPT);
            expect(toast.error).toHaveBeenCalledWith('Could not resolve message to refresh');
        });

        it('re-sends the same user message after a failed re-run, which kept it', async () => {
            respondToRuns(failedRun);

            renderProvider();

            await reloadReply('event-5');

            expect(threadIds()).toContain('event-5');

            respondToRuns(finishedRun);

            await reloadReply('event-5');

            expect(hoisted.truncateMock.mock.calls.map(([variables]) => variables)).toEqual([
                {id: '9', keepMessage: true, messageId: 'event-5', workspaceId: '1'},
                {id: '9', keepMessage: true, messageId: 'event-5', workspaceId: '1'},
            ]);
            expect(sentRunBodies.map((body) => body.messages)).toEqual([
                [{content: 'Now rename it', id: 'event-5', role: 'user'}],
                [{content: 'Now rename it', id: 'event-5', role: 'user'}],
            ]);
        });

        it('re-sends the last user message the server never saved, without truncating anything', async () => {
            hoisted.truncateMock.mockResolvedValue({truncateAiHubChatMessages: null});

            renderProvider();

            await reloadReply('event-5');

            expect(sentRunBodies[0].messages).toEqual([{content: 'Now rename it', id: 'event-5', role: 'user'}]);
        });

        it('drops the tool calls the removed bubbles carried', async () => {
            aiHubStore.setState({messages: [...LOADED_TRANSCRIPT, ...LIVE_SPLIT_TURN]});

            aiChatToolCallStore
                .getState()
                .startToolCall('call-build', 'buildWorkflow', LOADED_TRANSCRIPT.length + 1, 'thread-9');

            renderProvider();

            await reloadReply('live-text-1');

            expect(aiChatToolCallStore.getState().toolCalls['call-build']).toBeUndefined();
        });
    });

    it('leaves a chat switched to during the edit truncation alone', async () => {
        const switchedTranscript: ThreadMessageLike[] = [{content: 'Other chat', id: 'other-1', role: 'user'}];

        hoisted.truncateMock.mockImplementation(async () => {
            aiHubStore.setState((state) => ({
                chatId: 'thread-9',
                messages: switchedTranscript,
                transcriptGeneration: state.transcriptGeneration + 1,
            }));

            return {truncateAiHubChatMessages: 2};
        });

        renderProvider();

        await editMessage('event-5', 'Now rename it to Leads');

        expect(aiHubStore.getState().messages).toBe(switchedTranscript);
        expect(sentRunBodies).toEqual([]);
        expect(toast.error).toHaveBeenCalledWith(EDIT_NOT_SENT_MESSAGE);
    });

    it('leaves a chat switched to during the reload truncation alone', async () => {
        const switchedTranscript: ThreadMessageLike[] = [{content: 'Other chat', id: 'other-1', role: 'user'}];

        hoisted.truncateMock.mockImplementation(async () => {
            aiHubStore.setState((state) => ({
                chatId: 'thread-9',
                messages: switchedTranscript,
                transcriptGeneration: state.transcriptGeneration + 1,
            }));

            return {truncateAiHubChatMessages: 2};
        });

        renderProvider();

        await reloadReply('event-6');

        expect(aiHubStore.getState().messages).toBe(switchedTranscript);
        expect(sentRunBodies).toEqual([]);
    });

    it('sends a new user message under the id the thread shows it with', async () => {
        renderProvider();

        await act(async () => {
            await getRuntimeOptions().onNew({
                content: [{text: 'And publish it', type: 'text'}],
            } as unknown as AppendMessage);
        });

        const sentMessage = aiHubStore.getState().messages.find((message) => message.content === 'And publish it');

        expect(sentMessage?.id).toMatch(UUID_PATTERN);
        expect(sentRunBodies[0].messages.at(-1)).toEqual({
            content: 'And publish it',
            id: sentMessage?.id,
            role: 'user',
        });
    });
});

describe('findReloadedUserMessageIndex', () => {
    it('is the parent itself when the parent is a user message', () => {
        expect(findReloadedUserMessageIndex(LOADED_TRANSCRIPT, 'event-5')).toBe(5);
    });

    it('is -1 for an unknown parent', () => {
        expect(findReloadedUserMessageIndex(LOADED_TRANSCRIPT, 'missing')).toBe(-1);
    });
});

describe('a turn subscriber after its chat is switched away and back', () => {
    beforeEach(() => {
        aiHubStore.setState({
            chatId: 'thread-9',
            messages: [
                {content: 'Build a lead workflow', id: 'live-user', role: 'user'},
                {content: '', id: 'live-text-1', role: 'assistant'},
            ],
        });
    });

    it('stops writing into the reloaded transcript', () => {
        const subscriber = buildAiHubSubscriber({
            addMessage: aiHubStore.getState().addMessage,
            appendToLastAssistantMessage: aiHubStore.getState().appendToLastAssistantMessage,
            assistantMessageIndex: 1,
            chatId: 'thread-9',
            getLastUserMessage: () => 'Build a lead workflow',
        });

        aiHubStore.setState((state) => ({
            chatId: 'thread-9',
            messages: LOADED_TRANSCRIPT,
            transcriptGeneration: state.transcriptGeneration + 1,
        }));

        subscriber.onTextMessageContentEvent!({
            agent: {},
            event: {delta: 'Late text', messageId: 'm', type: EventType.TEXT_MESSAGE_CONTENT},
            input: {},
            messages: [],
            state: {},
            textMessageBuffer: '',
        } as unknown as Parameters<NonNullable<AgentSubscriber['onTextMessageContentEvent']>>[0]);

        expect(aiHubStore.getState().messages).toBe(LOADED_TRANSCRIPT);
    });
});
