import {aiHubStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubStore';
import {aiChatRetryableErrorStore} from '@/shared/components/ai-chat/stores/useAiChatRetryableErrorStore';
import {aiChatToolCallStore} from '@/shared/components/ai-chat/stores/useAiChatToolCallStore';
import {type AgentSubscriber} from '@ag-ui/client';
import {EventType} from '@ag-ui/core';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {buildAiHubSubscriber, validateRunChatWorkflowResult} from '../AiHubRuntimeProvider';
import {fetchWorkflowResponse, openWorkflowSseStream} from '../workflowStreamHandler';

vi.mock('@/ee/pages/automation/ai-hub/chats/api/chats.api', () => ({
    createAiHubChat: vi.fn(),
    generateAiHubChatTitle: vi.fn(),
    patchChat: vi.fn(),
}));

vi.mock('../workflowStreamHandler', () => ({
    fetchWorkflowResponse: vi.fn(),
    openWorkflowSseStream: vi.fn(),
}));

type ToolCallResultParamsType = Parameters<NonNullable<AgentSubscriber['onToolCallResultEvent']>>[0];
type ToolCallStartParamsType = Parameters<NonNullable<AgentSubscriber['onToolCallStartEvent']>>[0];
type RunFinishedParamsType = Parameters<NonNullable<AgentSubscriber['onRunFinishedEvent']>>[0];

type WorkflowRunRequestType = Parameters<typeof openWorkflowSseStream>[0];

const baseParams = {agent: {}, input: {}, messages: [], state: {}};

const STREAMING_RESULT = JSON.stringify({
    streamUrl: '/webhooks/exec-1/sse',
    streaming: true,
    workflowExecutionId: 'exec-1',
});

const SYNC_RESULT = JSON.stringify({
    responseUrl: '/webhooks/exec-1',
    streaming: false,
    workflowExecutionId: 'exec-1',
});

const createSubscriber = (turnAbortSignal?: AbortSignal) =>
    buildAiHubSubscriber({
        addMessage: aiHubStore.getState().addMessage,
        appendToLastAssistantMessage: aiHubStore.getState().appendToLastAssistantMessage,
        assistantMessageIndex: 1,
        chatId: 'thread-1',
        getLastUserMessage: () => 'Run the support workflow',
        turnAbortSignal,
    });

const runToolCall = (subscriber: AgentSubscriber, content: string) => {
    subscriber.onToolCallStartEvent!({
        ...baseParams,
        event: {toolCallId: 'call-run', toolCallName: 'runChatWorkflow', type: EventType.TOOL_CALL_START},
    } as unknown as ToolCallStartParamsType);

    subscriber.onToolCallResultEvent!({
        ...baseParams,
        event: {content, messageId: 'm-2', role: 'tool', toolCallId: 'call-run', type: EventType.TOOL_CALL_RESULT},
    } as unknown as ToolCallResultParamsType);
};

const finishRun = (subscriber: AgentSubscriber) =>
    subscriber.onRunFinishedEvent!({
        ...baseParams,
        event: {runId: 'run-1', threadId: 'thread-1', type: EventType.RUN_FINISHED},
    } as unknown as RunFinishedParamsType);

const lastStreamRequest = (): WorkflowRunRequestType => {
    const calls = vi.mocked(openWorkflowSseStream).mock.calls;

    return calls[calls.length - 1][0];
};

const toolCallStatus = () => aiChatToolCallStore.getState().toolCalls['call-run']?.status;

const assistantTexts = () =>
    aiHubStore
        .getState()
        .messages.filter((message) => message.role === 'assistant' && typeof message.content === 'string')
        .map((message) => message.content);

describe('runChatWorkflow in an AI Hub turn', () => {
    beforeEach(() => {
        vi.mocked(openWorkflowSseStream).mockReset();
        vi.mocked(fetchWorkflowResponse).mockReset();

        aiChatToolCallStore.setState({order: [], toolCalls: {}});
        aiChatRetryableErrorStore.getState().clearError();

        aiHubStore.setState({
            chatId: 'thread-1',
            messages: [
                {content: 'Run the support workflow', id: 'user-1', role: 'user'},
                {content: '', id: 'assistant-1', role: 'assistant'},
            ],
        });
    });

    it('opens the stream with the turn signal and the user message', () => {
        const turnController = new AbortController();

        runToolCall(createSubscriber(turnController.signal), STREAMING_RESULT);

        expect(openWorkflowSseStream).toHaveBeenCalledOnce();
        expect(lastStreamRequest()).toMatchObject({
            message: 'Run the support workflow',
            signal: turnController.signal,
            streamUrl: '/webhooks/exec-1/sse',
        });
        expect(fetchWorkflowResponse).not.toHaveBeenCalled();
    });

    it('fetches the single response of a workflow that does not stream', () => {
        runToolCall(createSubscriber(), SYNC_RESULT);

        expect(fetchWorkflowResponse).toHaveBeenCalledOnce();
        expect(vi.mocked(fetchWorkflowResponse).mock.calls[0][0]).toMatchObject({responseUrl: '/webhooks/exec-1'});
        expect(openWorkflowSseStream).not.toHaveBeenCalled();
    });

    it('keeps the card running past the end of the agent run and streams the reply into a new assistant message', () => {
        const subscriber = createSubscriber();

        runToolCall(subscriber, STREAMING_RESULT);

        expect(toolCallStatus()).toBe('running');

        finishRun(subscriber);

        expect(toolCallStatus()).toBe('running');

        const request = lastStreamRequest();

        request.onChunk('Hello');
        request.onChunk(' there');

        expect(assistantTexts()).toEqual(['', 'Hello there']);

        request.onComplete({chunkCount: 2, kind: 'completed'});

        expect(toolCallStatus()).toBe('success');
    });

    it('still terminates the other running tool calls when the run finishes', () => {
        const subscriber = createSubscriber();

        subscriber.onToolCallStartEvent!({
            ...baseParams,
            event: {toolCallId: 'call-orphan', toolCallName: 'research', type: EventType.TOOL_CALL_START},
        } as unknown as ToolCallStartParamsType);

        runToolCall(subscriber, STREAMING_RESULT);

        finishRun(subscriber);

        expect(aiChatToolCallStore.getState().toolCalls['call-orphan'].status).toBe('aborted');
        expect(toolCallStatus()).toBe('running');
    });

    it('releases the card to the orphan sweep once the stream settled', () => {
        const subscriber = createSubscriber();

        runToolCall(subscriber, STREAMING_RESULT);

        lastStreamRequest().onComplete({chunkCount: 1, kind: 'completed'});

        finishRun(subscriber);

        expect(toolCallStatus()).toBe('success');
    });

    it('marks the card failed and raises the retry banner when the stream fails', () => {
        runToolCall(createSubscriber(), STREAMING_RESULT);

        lastStreamRequest().onError!({message: 'Workflow stream failed: HTTP 500'});

        expect(toolCallStatus()).toBe('error');
        expect(aiChatRetryableErrorStore.getState().currentError?.errorMessage).toBe(
            'Workflow stream failed: HTTP 500'
        );
    });

    it('treats a workflow that produced no output as a failure', () => {
        runToolCall(createSubscriber(), STREAMING_RESULT);

        lastStreamRequest().onComplete({kind: 'empty'});

        expect(toolCallStatus()).toBe('error');
        expect(assistantTexts()).toEqual(['']);
    });

    it('drops reply chunks that arrive after the user switched to another chat', () => {
        runToolCall(createSubscriber(), STREAMING_RESULT);

        aiHubStore.setState({chatId: 'thread-2', messages: []});

        lastStreamRequest().onChunk('late');

        expect(aiHubStore.getState().messages).toEqual([]);
    });

    it('does not run a workflow whose result carries no webhook url', () => {
        runToolCall(
            createSubscriber(),
            JSON.stringify({streamUrl: 'https://evil.example/sse', streaming: true, workflowExecutionId: 'exec-1'})
        );

        expect(openWorkflowSseStream).not.toHaveBeenCalled();
        expect(fetchWorkflowResponse).not.toHaveBeenCalled();
        expect(toolCallStatus()).toBe('error');
    });

    it('does not run a workflow when the tool reported an error', () => {
        runToolCall(createSubscriber(), JSON.stringify({error: 'exec-9 is not an enabled chat workflow'}));

        expect(openWorkflowSseStream).not.toHaveBeenCalled();
        expect(toolCallStatus()).toBe('error');
    });
});

describe('validateRunChatWorkflowResult', () => {
    it('accepts only webhook urls', () => {
        expect(validateRunChatWorkflowResult(JSON.parse(STREAMING_RESULT))).toEqual({
            streaming: true,
            url: '/webhooks/exec-1/sse',
        });
        expect(validateRunChatWorkflowResult(JSON.parse(SYNC_RESULT))).toEqual({
            streaming: false,
            url: '/webhooks/exec-1',
        });
        expect(
            validateRunChatWorkflowResult({
                responseUrl: '/webhooks/../api/x',
                streaming: false,
                workflowExecutionId: 'e',
            })
        ).toBeNull();
        expect(validateRunChatWorkflowResult({streaming: true, workflowExecutionId: 'e'})).toBeNull();
        expect(validateRunChatWorkflowResult(null)).toBeNull();
    });
});
