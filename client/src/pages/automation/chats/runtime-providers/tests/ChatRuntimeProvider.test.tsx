import {useChatsStore} from '@/pages/automation/chats/stores/useChatsStore';
import {
    ApprovalResolutionContextI,
    ResumeError,
    useApprovalResolution,
} from '@/shared/components/ai-chat/approvalResolutionContext';
import {SSERequestType, UseSSEOptionsType, useSSE} from '@/shared/hooks/useSSE';
import {
    AskUserQuestionEventI,
    formatAskUserQuestionMessage,
    getResumeStreamRequest,
} from '@/shared/util/assistant-message-utils';
import {AppendMessage, useExternalStoreRuntime} from '@assistant-ui/react';
import {act, render, renderHook} from '@testing-library/react';
import {ReactNode} from 'react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {ChatRuntimeProvider} from '../ChatRuntimeProvider';

vi.mock('@/shared/hooks/useSSE', () => ({
    useSSE: vi.fn(() => ({
        close: vi.fn(),
        connectionState: 'CLOSED',
        data: null,
        error: null,
        errorStatus: null,
    })),
}));

vi.mock('@assistant-ui/react', () => ({
    AssistantRuntimeProvider: ({children}: {children: ReactNode}) => <div>{children}</div>,
    CompositeAttachmentAdapter: vi.fn(),
    SimpleImageAttachmentAdapter: vi.fn(),
    SimpleTextAttachmentAdapter: vi.fn(),
    useExternalStoreRuntime: vi.fn(() => ({})),
}));

describe('ChatRuntimeProvider', () => {
    it('renders children', () => {
        const {result} = renderHook(() => null, {
            wrapper: ({children}) => (
                <ChatRuntimeProvider environmentName="test" workflowExecutionId="workflow-123">
                    {children}
                </ChatRuntimeProvider>
            ),
        });

        expect(result).toBeDefined();
    });

    it('accepts sseStream prop', () => {
        const {result} = renderHook(() => null, {
            wrapper: ({children}) => (
                <ChatRuntimeProvider environmentName="test" sseStreamResponse={true} workflowExecutionId="workflow-123">
                    {children}
                </ChatRuntimeProvider>
            ),
        });

        expect(result).toBeDefined();
    });

    it('initializes with correct environment', () => {
        const {result} = renderHook(() => null, {
            wrapper: ({children}) => (
                <ChatRuntimeProvider environmentName="production" workflowExecutionId="workflow-123">
                    {children}
                </ChatRuntimeProvider>
            ),
        });

        expect(result).toBeDefined();
    });

    it('handles different workflow execution IDs', () => {
        const {result} = renderHook(() => null, {
            wrapper: ({children}) => (
                <ChatRuntimeProvider environmentName="test" workflowExecutionId="different-workflow-456">
                    {children}
                </ChatRuntimeProvider>
            ),
        });

        expect(result).toBeDefined();
    });

    it('resets isRunning when the stream closes', () => {
        useChatsStore.setState({isRunning: true, messages: [], resumeUrl: null});

        vi.mocked(useSSE).mockReturnValue({
            close: vi.fn(),
            connectionState: 'CLOSED',
            data: null,
            error: null,
            errorStatus: null,
        });

        render(
            <ChatRuntimeProvider environmentName="test" workflowExecutionId="workflow-123">
                child
            </ChatRuntimeProvider>
        );

        expect(useChatsStore.getState().isRunning).toBe(false);
    });

    it('resets isRunning and shows the failure when the stream fails', () => {
        useChatsStore.setState({
            isRunning: true,
            messages: [{content: '', role: 'assistant'}],
            resumeUrl: null,
        });

        vi.mocked(useSSE).mockReturnValue({
            close: vi.fn(),
            connectionState: 'ERROR',
            data: null,
            error: 'HTTP 500',
            errorStatus: 500,
        });

        render(
            <ChatRuntimeProvider environmentName="test" workflowExecutionId="workflow-123">
                child
            </ChatRuntimeProvider>
        );

        const {isRunning, messages} = useChatsStore.getState();

        expect(isRunning).toBe(false);
        expect(messages[messages.length - 1]).toEqual({content: 'The request failed: HTTP 500', role: 'assistant'});
    });
});

describe('ChatRuntimeProvider resume', () => {
    const RESUME_URL = 'https://example.com/job/resume/abc';

    const QUESTION_EVENT: AskUserQuestionEventI = {
        questions: [
            {
                header: 'Color',
                multiSelect: false,
                options: [{description: 'A calm color', label: 'Blue'}],
                question: 'Which color do you prefer?',
            },
        ],
        resumeUrl: RESUME_URL,
    };

    const QUESTION_TEXT = formatAskUserQuestionMessage(QUESTION_EVENT);

    const fetchSpy = vi.fn<typeof fetch>();
    const originalFetch = global.fetch;

    function getOnNew() {
        const options = vi.mocked(useExternalStoreRuntime).mock.lastCall![0] as unknown as {
            onNew: (message: AppendMessage) => Promise<void>;
        };

        return options.onNew;
    }

    function createTextMessage(text: string) {
        return {content: [{text, type: 'text'}]} as unknown as AppendMessage;
    }

    function mockFailingStream(error: string, errorStatus: number | null) {
        vi.mocked(useSSE).mockImplementation((request) => ({
            close: vi.fn(),
            connectionState: request ? 'ERROR' : 'CLOSED',
            data: null,
            error: request ? error : null,
            errorStatus: request ? errorStatus : null,
        }));
    }

    function mockSwitchableStream() {
        const stream: {connectionState: 'CLOSED' | 'CONNECTED'} = {connectionState: 'CLOSED'};

        vi.mocked(useSSE).mockImplementation(() => ({
            close: vi.fn(),
            connectionState: stream.connectionState,
            data: null,
            error: null,
            errorStatus: null,
        }));

        return stream;
    }

    async function renderAndAskQuestion() {
        const stream = mockSwitchableStream();

        useChatsStore.setState({resumeUrl: null});

        render(
            <ChatRuntimeProvider environmentName="test" sseStreamResponse={true} workflowExecutionId="workflow-123">
                child
            </ChatRuntimeProvider>
        );

        await act(async () => {
            await getOnNew()(createTextMessage('Pick a color'));
        });

        stream.connectionState = 'CONNECTED';

        const sseOptions = vi.mocked(useSSE).mock.lastCall![1]!;

        act(() => {
            sseOptions.eventHandlers!.ask_user_question(QUESTION_EVENT);
        });

        return {sseOptions, stream};
    }

    beforeEach(() => {
        global.fetch = fetchSpy;

        vi.mocked(useSSE).mockReturnValue({
            close: vi.fn(),
            connectionState: 'CLOSED',
            data: null,
            error: null,
            errorStatus: null,
        });

        useChatsStore.setState({isRunning: false, messages: [], resumeUrl: RESUME_URL});
    });

    afterEach(() => {
        global.fetch = originalFetch;

        vi.clearAllMocks();
    });

    it('streams the resumed agent turn when the chat streams its responses', async () => {
        render(
            <ChatRuntimeProvider environmentName="test" sseStreamResponse={true} workflowExecutionId="workflow-123">
                child
            </ChatRuntimeProvider>
        );

        await act(async () => {
            await getOnNew()(createTextMessage('Blue'));
        });

        expect(fetchSpy).not.toHaveBeenCalled();
        expect(vi.mocked(useSSE).mock.lastCall![0]).toEqual(getResumeStreamRequest(RESUME_URL, 'Blue'));

        const {isRunning, messages, resumeUrl} = useChatsStore.getState();

        expect(isRunning).toBe(true);
        expect(resumeUrl).toBeNull();
        expect(messages[messages.length - 1]).toEqual({content: '', role: 'assistant'});
    });

    it('posts the answer without streaming when the chat does not stream its responses', async () => {
        fetchSpy.mockResolvedValue(new Response(null, {status: 204}));

        render(
            <ChatRuntimeProvider environmentName="test" workflowExecutionId="workflow-123">
                child
            </ChatRuntimeProvider>
        );

        await act(async () => {
            await getOnNew()(createTextMessage('Blue'));
        });

        expect(fetchSpy).toHaveBeenCalledWith(RESUME_URL, expect.objectContaining({method: 'POST'}));
        expect(vi.mocked(useSSE).mock.lastCall![0]).toBeNull();
        expect(useChatsStore.getState().isRunning).toBe(false);
    });

    it('shows the expiry in the assistant bubble and keeps the resume URL cleared when the streamed resume is gone', async () => {
        mockFailingStream('HTTP 410', 410);

        render(
            <ChatRuntimeProvider environmentName="test" sseStreamResponse={true} workflowExecutionId="workflow-123">
                child
            </ChatRuntimeProvider>
        );

        await act(async () => {
            await getOnNew()(createTextMessage('Blue'));
        });

        const {isRunning, messages, resumeUrl} = useChatsStore.getState();

        expect(isRunning).toBe(false);
        expect(resumeUrl).toBeNull();
        expect(messages[messages.length - 1]).toEqual({
            content: 'This question has expired or was already answered.',
            role: 'assistant',
        });
    });

    it('restores the resume URL when the streamed answer arrived before the workflow finished suspending', async () => {
        mockFailingStream('HTTP 409', 409);

        render(
            <ChatRuntimeProvider environmentName="test" sseStreamResponse={true} workflowExecutionId="workflow-123">
                child
            </ChatRuntimeProvider>
        );

        await act(async () => {
            await getOnNew()(createTextMessage('Blue'));
        });

        const {isRunning, messages, resumeUrl} = useChatsStore.getState();

        expect(isRunning).toBe(false);
        expect(resumeUrl).toBe(RESUME_URL);
        expect(messages[messages.length - 1]).toEqual({
            content: "The workflow wasn't ready for your answer yet. Please send it again.",
            role: 'assistant',
        });
    });

    it('restores the resume URL when the streamed resume hits a connection error', async () => {
        mockFailingStream('Connection error occurred', null);

        render(
            <ChatRuntimeProvider environmentName="test" sseStreamResponse={true} workflowExecutionId="workflow-123">
                child
            </ChatRuntimeProvider>
        );

        await act(async () => {
            await getOnNew()(createTextMessage('Blue'));
        });

        const {messages, resumeUrl} = useChatsStore.getState();

        expect(resumeUrl).toBe(RESUME_URL);
        expect(messages[messages.length - 1]).toEqual({
            content: 'Could not reach the workflow. Please send your answer again.',
            role: 'assistant',
        });
    });

    it('explains how to fix a non-streaming chat whose workflow would pause', async () => {
        fetchSpy.mockResolvedValue(
            Response.json(
                {
                    detail: "Workflows that return a synchronous response can't contain steps that pause the run",
                    entityClass: 'WebhookErrorType',
                    errorKey: 100,
                },
                {status: 400}
            )
        );

        useChatsStore.setState({resumeUrl: null});

        render(
            <ChatRuntimeProvider environmentName="test" workflowExecutionId="workflow-123">
                child
            </ChatRuntimeProvider>
        );

        await act(async () => {
            await getOnNew()(createTextMessage('Hello'));
        });

        const {isRunning, messages} = useChatsStore.getState();

        expect(isRunning).toBe(false);
        expect(messages[messages.length - 1]).toEqual({
            content:
                "This chat can't pause the run for an approval or wait step. A chat can only wait on one when it " +
                'streams its reply, so add a step that streams, such as an AI Agent, after it.',
            role: 'assistant',
        });
    });

    it('shows the server detail for any other failed non-streaming message', async () => {
        fetchSpy.mockResolvedValue(
            Response.json({detail: 'Workflow not found', entityClass: 'Workflow', errorKey: 100}, {status: 400})
        );

        useChatsStore.setState({resumeUrl: null});

        render(
            <ChatRuntimeProvider environmentName="test" workflowExecutionId="workflow-123">
                child
            </ChatRuntimeProvider>
        );

        await act(async () => {
            await getOnNew()(createTextMessage('Hello'));
        });

        const {messages} = useChatsStore.getState();

        expect(messages[messages.length - 1]).toEqual({
            content: 'An error occurred\nWorkflow not found',
            role: 'assistant',
        });
    });

    it('restores the resume URL when the non-streamed answer arrived before the workflow finished suspending', async () => {
        fetchSpy.mockResolvedValue(new Response(null, {status: 409}));

        render(
            <ChatRuntimeProvider environmentName="test" workflowExecutionId="workflow-123">
                child
            </ChatRuntimeProvider>
        );

        await act(async () => {
            await getOnNew()(createTextMessage('Blue'));
        });

        const {isRunning, messages, resumeUrl} = useChatsStore.getState();

        expect(isRunning).toBe(false);
        expect(resumeUrl).toBe(RESUME_URL);
        expect(messages[messages.length - 1]).toEqual({
            content: "The workflow wasn't ready for your answer yet. Please send it again.",
            role: 'assistant',
        });
    });

    it('does not restore the resume URL when the non-streamed answer is gone', async () => {
        fetchSpy.mockResolvedValue(new Response(null, {status: 410}));

        render(
            <ChatRuntimeProvider environmentName="test" workflowExecutionId="workflow-123">
                child
            </ChatRuntimeProvider>
        );

        await act(async () => {
            await getOnNew()(createTextMessage('Blue'));
        });

        const {messages, resumeUrl} = useChatsStore.getState();

        expect(resumeUrl).toBeNull();
        expect(messages[messages.length - 1]).toEqual({
            content: 'This question has expired or was already answered.',
            role: 'assistant',
        });
    });

    it('restores the resume URL when the streamed resume hits a transient server error', async () => {
        mockFailingStream('HTTP 503', 503);

        render(
            <ChatRuntimeProvider environmentName="test" sseStreamResponse={true} workflowExecutionId="workflow-123">
                child
            </ChatRuntimeProvider>
        );

        await act(async () => {
            await getOnNew()(createTextMessage('Blue'));
        });

        expect(useChatsStore.getState().resumeUrl).toBe(RESUME_URL);
    });

    it('tells the user the workflow failed when the streamed resume reaches a failed job', async () => {
        mockFailingStream('HTTP 422', 422);

        render(
            <ChatRuntimeProvider environmentName="test" sseStreamResponse={true} workflowExecutionId="workflow-123">
                child
            </ChatRuntimeProvider>
        );

        await act(async () => {
            await getOnNew()(createTextMessage('Blue'));
        });

        const {messages, resumeUrl} = useChatsStore.getState();

        expect(resumeUrl).toBeNull();
        expect(messages[messages.length - 1]).toEqual({
            content: 'The workflow failed, so your answer could not be delivered.',
            role: 'assistant',
        });
    });

    it('keeps the streamed text, shows the failure and does not offer the answer again when an accepted resume drops', async () => {
        let connectionState: 'CLOSED' | 'CONNECTED' | 'ERROR' = 'CLOSED';

        vi.mocked(useSSE).mockImplementation(() => ({
            close: vi.fn(),
            connectionState,
            data: null,
            error: connectionState === 'ERROR' ? 'Connection error occurred' : null,
            errorStatus: null,
        }));

        render(
            <ChatRuntimeProvider environmentName="test" sseStreamResponse={true} workflowExecutionId="workflow-123">
                child
            </ChatRuntimeProvider>
        );

        await act(async () => {
            await getOnNew()(createTextMessage('Blue'));
        });

        connectionState = 'CONNECTED';

        act(() => {
            useChatsStore.getState().appendToLastAssistantMessage('Partial answer');
        });

        connectionState = 'ERROR';

        act(() => {
            useChatsStore.getState().appendToLastAssistantMessage('');
        });

        const {isRunning, messages, resumeUrl} = useChatsStore.getState();

        expect(isRunning).toBe(false);
        expect(resumeUrl).toBeNull();
        expect(messages[messages.length - 1]).toEqual({
            content: 'Partial answer\n\nThe request failed: Connection error occurred',
            role: 'assistant',
        });
    });

    it('tells the user the response ended unexpectedly when a resumed stream closes without any event', async () => {
        let connectionState: 'CLOSED' | 'CONNECTED' = 'CLOSED';

        vi.mocked(useSSE).mockImplementation(() => ({
            close: vi.fn(),
            connectionState,
            data: null,
            error: null,
            errorStatus: null,
        }));

        render(
            <ChatRuntimeProvider environmentName="test" sseStreamResponse={true} workflowExecutionId="workflow-123">
                child
            </ChatRuntimeProvider>
        );

        await act(async () => {
            await getOnNew()(createTextMessage('Blue'));
        });

        connectionState = 'CONNECTED';

        act(() => {
            useChatsStore.getState().appendToLastAssistantMessage('');
        });

        connectionState = 'CLOSED';

        act(() => {
            useChatsStore.getState().appendToLastAssistantMessage('');
        });

        const {isRunning, messages, resumeUrl} = useChatsStore.getState();

        expect(isRunning).toBe(false);
        expect(resumeUrl).toBeNull();
        expect(messages[messages.length - 1]).toEqual({
            content: 'The response ended unexpectedly.',
            role: 'assistant',
        });
    });

    it('keeps the streamed answer when the resumed stream closes normally', async () => {
        let connectionState: 'CLOSED' | 'CONNECTED' = 'CLOSED';

        vi.mocked(useSSE).mockImplementation(() => ({
            close: vi.fn(),
            connectionState,
            data: null,
            error: null,
            errorStatus: null,
        }));

        render(
            <ChatRuntimeProvider environmentName="test" sseStreamResponse={true} workflowExecutionId="workflow-123">
                child
            </ChatRuntimeProvider>
        );

        await act(async () => {
            await getOnNew()(createTextMessage('Blue'));
        });

        connectionState = 'CONNECTED';

        const sseOptions = vi.mocked(useSSE).mock.lastCall![1]!;

        act(() => {
            sseOptions.eventHandlers!.stream('Blue is a good choice.');
        });

        connectionState = 'CLOSED';

        act(() => {
            useChatsStore.getState().appendToLastAssistantMessage('');
        });

        const {messages} = useChatsStore.getState();

        expect(messages[messages.length - 1]).toEqual({content: 'Blue is a good choice.', role: 'assistant'});
    });

    it('tells the user the workflow is waiting when the stream suspends before any text', async () => {
        let connectionState: 'CLOSED' | 'CONNECTED' = 'CLOSED';

        vi.mocked(useSSE).mockImplementation(() => ({
            close: vi.fn(),
            connectionState,
            data: null,
            error: null,
            errorStatus: null,
        }));

        useChatsStore.setState({resumeUrl: null});

        render(
            <ChatRuntimeProvider environmentName="test" sseStreamResponse={true} workflowExecutionId="workflow-123">
                child
            </ChatRuntimeProvider>
        );

        await act(async () => {
            await getOnNew()(createTextMessage('Delete the customer record'));
        });

        connectionState = 'CONNECTED';

        const sseOptions = vi.mocked(useSSE).mock.lastCall![1]!;

        act(() => {
            sseOptions.eventHandlers!.suspended?.('');

            useChatsStore.getState().appendToLastAssistantMessage('');
        });

        connectionState = 'CLOSED';

        act(() => {
            useChatsStore.getState().appendToLastAssistantMessage('');
        });

        const {isRunning, messages} = useChatsStore.getState();

        expect(isRunning).toBe(false);
        expect(messages[messages.length - 1]).toEqual({
            content: 'The workflow is waiting for a response before it can continue.',
            role: 'assistant',
        });
    });

    it('keeps the streamed text when the stream suspends after it', async () => {
        let connectionState: 'CLOSED' | 'CONNECTED' = 'CLOSED';

        vi.mocked(useSSE).mockImplementation(() => ({
            close: vi.fn(),
            connectionState,
            data: null,
            error: null,
            errorStatus: null,
        }));

        useChatsStore.setState({resumeUrl: null});

        render(
            <ChatRuntimeProvider environmentName="test" sseStreamResponse={true} workflowExecutionId="workflow-123">
                child
            </ChatRuntimeProvider>
        );

        await act(async () => {
            await getOnNew()(createTextMessage('Delete the customer record'));
        });

        connectionState = 'CONNECTED';

        const sseOptions = vi.mocked(useSSE).mock.lastCall![1]!;

        act(() => {
            sseOptions.eventHandlers!.stream('I asked a manager to approve the deletion.');
            sseOptions.eventHandlers!.suspended?.('');
        });

        connectionState = 'CLOSED';

        act(() => {
            useChatsStore.getState().appendToLastAssistantMessage('');
        });

        const {messages} = useChatsStore.getState();

        expect(messages[messages.length - 1]).toEqual({
            content: 'I asked a manager to approve the deletion.',
            role: 'assistant',
        });
    });

    it('shows only the approval card when the approval request arrives before any text and the stream closes and suspends', async () => {
        let connectionState: 'CLOSED' | 'CONNECTED' = 'CLOSED';

        vi.mocked(useSSE).mockImplementation(() => ({
            close: vi.fn(),
            connectionState,
            data: null,
            error: null,
            errorStatus: null,
        }));

        useChatsStore.setState({resumeUrl: null});

        render(
            <ChatRuntimeProvider environmentName="test" sseStreamResponse={true} workflowExecutionId="workflow-123">
                child
            </ChatRuntimeProvider>
        );

        await act(async () => {
            await getOnNew()(createTextMessage('Delete the customer record'));
        });

        connectionState = 'CONNECTED';

        const sseOptions = vi.mocked(useSSE).mock.lastCall![1]!;

        act(() => {
            sseOptions.eventHandlers!.approval_request({formTitle: 'Approve deletion', resumeId: 'resume-1'});
        });

        connectionState = 'CLOSED';

        act(() => {
            useChatsStore.setState({isRunning: true});
        });

        act(() => {
            sseOptions.eventHandlers!.suspended?.('');
        });

        const {isRunning, messages} = useChatsStore.getState();

        expect(isRunning).toBe(false);
        expect(messages).toHaveLength(2);
        expect(messages[0]).toMatchObject({content: 'Delete the customer record', role: 'user'});
        expect(messages[1]).toEqual({
            content: [
                {
                    data: {
                        expiresAt: undefined,
                        formDescription: undefined,
                        formTitle: 'Approve deletion',
                        formUrl: undefined,
                        hasInputs: false,
                        kind: 'approval-request',
                        resumeId: 'resume-1',
                    },
                    type: 'data-approval-request',
                },
            ],
            role: 'assistant',
        });
    });

    it('keeps the streamed text and adds the approval card after it', async () => {
        let connectionState: 'CLOSED' | 'CONNECTED' = 'CLOSED';

        vi.mocked(useSSE).mockImplementation(() => ({
            close: vi.fn(),
            connectionState,
            data: null,
            error: null,
            errorStatus: null,
        }));

        useChatsStore.setState({resumeUrl: null});

        render(
            <ChatRuntimeProvider environmentName="test" sseStreamResponse={true} workflowExecutionId="workflow-123">
                child
            </ChatRuntimeProvider>
        );

        await act(async () => {
            await getOnNew()(createTextMessage('Delete the customer record'));
        });

        connectionState = 'CONNECTED';

        const sseOptions = vi.mocked(useSSE).mock.lastCall![1]!;

        act(() => {
            sseOptions.eventHandlers!.stream('I need approval first.');
            sseOptions.eventHandlers!.approval_request({resumeId: 'resume-1'});
            sseOptions.eventHandlers!.suspended?.('');
        });

        const {messages} = useChatsStore.getState();

        expect(messages).toHaveLength(3);
        expect(messages[1]).toEqual({content: 'I need approval first.', role: 'assistant'});
        expect(messages[2]).toMatchObject({
            content: [{data: {kind: 'approval-request', resumeId: 'resume-1'}, type: 'data-approval-request'}],
            role: 'assistant',
        });
    });

    it('shows an error when the ask_user_question event is malformed', async () => {
        useChatsStore.setState({resumeUrl: null});

        render(
            <ChatRuntimeProvider environmentName="test" sseStreamResponse={true} workflowExecutionId="workflow-123">
                child
            </ChatRuntimeProvider>
        );

        await act(async () => {
            await getOnNew()(createTextMessage('Pick a color'));
        });

        const sseOptions = vi.mocked(useSSE).mock.lastCall![1]!;

        act(() => {
            sseOptions.eventHandlers!.ask_user_question({questions: 'not a list'});
        });

        const {isRunning, messages, resumeUrl} = useChatsStore.getState();

        expect(isRunning).toBe(false);
        expect(resumeUrl).toBeNull();
        expect(messages[messages.length - 1]).toEqual({
            content: 'The agent asked a question in an unexpected format.',
            role: 'assistant',
        });
    });

    it('shows the payload of the error event in the assistant bubble', async () => {
        useChatsStore.setState({resumeUrl: null});

        render(
            <ChatRuntimeProvider environmentName="test" sseStreamResponse={true} workflowExecutionId="workflow-123">
                child
            </ChatRuntimeProvider>
        );

        await act(async () => {
            await getOnNew()(createTextMessage('Hi'));
        });

        const sseOptions = vi.mocked(useSSE).mock.lastCall![1]!;

        act(() => {
            sseOptions.eventHandlers!.error({message: 'The model rate limit was exceeded'});
        });

        const {isRunning, messages} = useChatsStore.getState();

        expect(isRunning).toBe(false);
        expect(messages[messages.length - 1]).toEqual({
            content: 'The model rate limit was exceeded',
            role: 'assistant',
        });
    });

    it('tells the user when the workflow result cannot be read', async () => {
        useChatsStore.setState({resumeUrl: null});

        render(
            <ChatRuntimeProvider environmentName="test" sseStreamResponse={true} workflowExecutionId="workflow-123">
                child
            </ChatRuntimeProvider>
        );

        await act(async () => {
            await getOnNew()(createTextMessage('Hi'));
        });

        const sseOptions = vi.mocked(useSSE).mock.lastCall![1]!;

        act(() => {
            sseOptions.eventHandlers!.result('{not json');
        });

        const {isRunning, messages} = useChatsStore.getState();

        expect(isRunning).toBe(false);
        expect(messages[messages.length - 1]).toEqual({
            content: 'Failed to read the workflow result.',
            role: 'assistant',
        });
    });

    it('keeps the stream open after a question while letting the user answer', async () => {
        await renderAndAskQuestion();

        const {isRunning, messages, resumeUrl} = useChatsStore.getState();

        expect(vi.mocked(useSSE).mock.lastCall![0]).not.toBeNull();
        expect(isRunning).toBe(false);
        expect(resumeUrl).toBe(RESUME_URL);
        expect(messages[messages.length - 1]).toEqual({content: QUESTION_TEXT, role: 'assistant'});
    });

    it('shows the failure and stops offering the answer when the job fails after asking a question', async () => {
        const {sseOptions} = await renderAndAskQuestion();

        act(() => {
            sseOptions.eventHandlers!.error('The agent failed');
        });

        const {isRunning, messages, resumeUrl} = useChatsStore.getState();

        expect(vi.mocked(useSSE).mock.lastCall![0]).toBeNull();
        expect(isRunning).toBe(false);
        expect(resumeUrl).toBeNull();
        expect(messages[messages.length - 1]).toEqual({
            content: `${QUESTION_TEXT}\n\nThe agent failed`,
            role: 'assistant',
        });
    });

    it('keeps the question and its resume URL when the job suspends after asking it', async () => {
        const {sseOptions, stream} = await renderAndAskQuestion();

        expect(vi.mocked(useSSE).mock.lastCall![0]).not.toBeNull();

        act(() => {
            sseOptions.eventHandlers!.suspended('');
        });

        stream.connectionState = 'CLOSED';

        act(() => {
            useChatsStore.getState().appendToLastAssistantMessage('');
        });

        const {isRunning, messages, resumeUrl} = useChatsStore.getState();

        expect(vi.mocked(useSSE).mock.lastCall![0]).toBeNull();
        expect(isRunning).toBe(false);
        expect(resumeUrl).toBe(RESUME_URL);
        expect(messages[messages.length - 1]).toEqual({content: QUESTION_TEXT, role: 'assistant'});
    });

    it('keeps the question and stops offering the answer when the job completes after asking it', async () => {
        const {sseOptions} = await renderAndAskQuestion();

        act(() => {
            sseOptions.eventHandlers!.result(JSON.stringify({message: 'Final answer'}));
        });

        const {isRunning, messages, resumeUrl} = useChatsStore.getState();

        expect(vi.mocked(useSSE).mock.lastCall![0]).toBeNull();
        expect(isRunning).toBe(false);
        expect(resumeUrl).toBeNull();
        expect(messages[messages.length - 1]).toEqual({content: QUESTION_TEXT, role: 'assistant'});
    });

    it('keeps offering the answer and shows the failure when the question stream loses its connection', async () => {
        await renderAndAskQuestion();

        vi.mocked(useSSE).mockImplementation(() => ({
            close: vi.fn(),
            connectionState: 'ERROR',
            data: null,
            error: 'Network down',
            errorStatus: null,
        }));

        act(() => {
            useChatsStore.getState().appendToLastAssistantMessage('');
        });

        const {isRunning, messages, resumeUrl} = useChatsStore.getState();

        expect(isRunning).toBe(false);
        expect(resumeUrl).toBe(RESUME_URL);
        expect(messages[messages.length - 1]).toEqual({
            content: `${QUESTION_TEXT}\n\nThe request failed: Network down`,
            role: 'assistant',
        });
    });

    it('does not report an unexpected end when the stream closes cleanly after a question', async () => {
        const {stream} = await renderAndAskQuestion();

        stream.connectionState = 'CLOSED';

        act(() => {
            useChatsStore.getState().appendToLastAssistantMessage('');
        });

        const {messages, resumeUrl} = useChatsStore.getState();

        expect(resumeUrl).toBe(RESUME_URL);
        expect(messages[messages.length - 1]).toEqual({content: QUESTION_TEXT, role: 'assistant'});
    });

    it('replaces the still-open question stream with the resume stream when the user answers', async () => {
        await renderAndAskQuestion();

        await act(async () => {
            await getOnNew()(createTextMessage('Blue'));
        });

        const {isRunning, messages, resumeUrl} = useChatsStore.getState();

        expect(vi.mocked(useSSE).mock.lastCall![0]).toEqual(getResumeStreamRequest(RESUME_URL, 'Blue'));
        expect(isRunning).toBe(true);
        expect(resumeUrl).toBeNull();
        expect(messages[messages.length - 1]).toEqual({content: '', role: 'assistant'});
    });
});

describe('ChatRuntimeProvider approval resolution', () => {
    const APPROVAL_CARD_MESSAGE = {
        content: [{data: {kind: 'approval-request', resumeId: 'first'}, type: 'data-approval-request'}],
        role: 'assistant',
    } as const;

    const stream: {
        connectionState: 'CLOSED' | 'CONNECTED' | 'ERROR';
        error: string | null;
        errorStatus: number | null;
    } = {connectionState: 'CLOSED', error: null, errorStatus: null};

    let approvalResolution: ApprovalResolutionContextI | null = null;

    function ApprovalResolutionCapture({label}: {label: string}) {
        approvalResolution = useApprovalResolution();

        return <span>{label}</span>;
    }

    function getLastSseCall(): [SSERequestType, UseSSEOptionsType] {
        const lastCall = vi.mocked(useSSE).mock.lastCall!;

        return [lastCall[0], lastCall[1]!];
    }

    function renderProvider() {
        const renderResult = render(
            <ChatRuntimeProvider environmentName="test" sseStreamResponse={true} workflowExecutionId="workflow-123">
                <ApprovalResolutionCapture label="first render" />
            </ChatRuntimeProvider>
        );

        return () =>
            renderResult.rerender(
                <ChatRuntimeProvider environmentName="test" sseStreamResponse={true} workflowExecutionId="workflow-123">
                    <ApprovalResolutionCapture label="next render" />
                </ChatRuntimeProvider>
            );
    }

    beforeEach(() => {
        stream.connectionState = 'CLOSED';
        stream.error = null;
        stream.errorStatus = null;

        vi.mocked(useSSE).mockImplementation(() => ({
            close: vi.fn(),
            connectionState: stream.connectionState,
            data: null,
            error: stream.error,
            errorStatus: stream.errorStatus,
        }));

        useChatsStore.setState({isRunning: false, messages: [APPROVAL_CARD_MESSAGE], resumeUrl: null});
    });

    afterEach(() => {
        approvalResolution = null;

        vi.clearAllMocks();
    });

    it('refuses a second decision while the first one is being submitted', async () => {
        renderProvider();

        let firstDecision: Promise<void> | undefined;

        act(() => {
            firstDecision = approvalResolution!.resolveApproval('first', {approved: true});
        });

        const [firstRequest] = getLastSseCall();

        let secondDecision: Promise<void> | undefined;

        act(() => {
            secondDecision = approvalResolution!.resolveApproval('second', {approved: true});
        });

        await expect(secondDecision).rejects.toThrow('Another decision is being submitted');

        const [currentRequest, sseOptions] = getLastSseCall();

        expect(currentRequest).toBe(firstRequest);
        expect(currentRequest?.url).toBe('/job/resume/first');

        act(() => {
            sseOptions.onRequestSuccess?.(firstRequest);
        });

        await expect(firstDecision).resolves.toBeUndefined();
    });

    it('settles the decision when its request is aborted', async () => {
        renderProvider();

        let decision: Promise<void> | undefined;

        act(() => {
            decision = approvalResolution!.resolveApproval('first', {approved: true});
        });

        const [request, sseOptions] = getLastSseCall();

        act(() => {
            sseOptions.onRequestAbort?.(request);
        });

        await expect(decision).rejects.toBeInstanceOf(ResumeError);
    });

    it('ignores the abort of a request other than the decision being submitted', async () => {
        renderProvider();

        let decision: Promise<void> | undefined;

        act(() => {
            decision = approvalResolution!.resolveApproval('first', {approved: true});
        });

        const [request, sseOptions] = getLastSseCall();

        act(() => {
            sseOptions.onRequestAbort?.({url: '/webhooks/workflow-123/sse'});
            sseOptions.onRequestSuccess?.(request);
        });

        await expect(decision).resolves.toBeUndefined();
    });

    it('adds the assistant bubble for the resumed run only once the request succeeds', async () => {
        renderProvider();

        let decision: Promise<void> | undefined;

        act(() => {
            decision = approvalResolution!.resolveApproval('first', {approved: true});
        });

        expect(useChatsStore.getState().messages).toEqual([APPROVAL_CARD_MESSAGE]);

        const [request, sseOptions] = getLastSseCall();

        act(() => {
            sseOptions.onRequestSuccess?.(request);
        });

        await expect(decision).resolves.toBeUndefined();

        expect(useChatsStore.getState().messages).toEqual([APPROVAL_CARD_MESSAGE, {content: '', role: 'assistant'}]);
    });

    it('leaves the chat untouched when a decision is refused, however often it is retried', async () => {
        const rerenderProvider = renderProvider();

        for (let attempt = 0; attempt < 2; attempt++) {
            stream.connectionState = 'CLOSED';
            stream.error = null;
            stream.errorStatus = null;

            let decision: Promise<void> | undefined;

            act(() => {
                decision = approvalResolution!.resolveApproval('first', {approved: true});
            });

            const [request, sseOptions] = getLastSseCall();

            act(() => {
                sseOptions.onRequestError?.(410, request);
            });

            await expect(decision).rejects.toMatchObject({status: 410});

            stream.connectionState = 'ERROR';
            stream.error = 'HTTP 410';
            stream.errorStatus = 410;

            act(() => {
                rerenderProvider();
            });
        }

        const {isRunning, messages} = useChatsStore.getState();

        expect(isRunning).toBe(false);
        expect(messages).toEqual([APPROVAL_CARD_MESSAGE]);
    });
});
