import {useSSE} from '@/shared/hooks/useSSE';
import {AppendMessage, useExternalStoreRuntime} from '@assistant-ui/react';
import {act, render} from '@testing-library/react';
import {ReactNode} from 'react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {useAiAgentTestingChatStore} from '../../../../stores';
import AiAgentTestRuntimeProvider from '../AiAgentTestRuntimeProvider';

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowDataStore', () => ({
    default: vi.fn((selector) => selector({workflow: {id: 'workflow-1'}})),
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowEditorStore', () => ({
    default: vi.fn((selector) => selector({rootClusterElementNodeData: {workflowNodeName: 'aiAgent_1'}})),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: vi.fn((selector) => selector({currentEnvironmentId: 1})),
}));

vi.mock('@/shared/hooks/useSSE', () => ({
    useSSE: vi.fn(() => ({close: vi.fn(), connectionState: 'CLOSED', data: null, error: null, errorStatus: null})),
}));

vi.mock('@assistant-ui/react', () => ({
    AssistantRuntimeProvider: ({children}: {children: ReactNode}) => <div>{children}</div>,
    CompositeAttachmentAdapter: vi.fn(),
    SimpleImageAttachmentAdapter: vi.fn(),
    SimpleTextAttachmentAdapter: vi.fn(),
    useExternalStoreRuntime: vi.fn(() => ({})),
}));

const AI_AGENT_TESTS_URL = '/api/platform/internal/ai-agent-tests';

function getRuntimeOptions() {
    return vi.mocked(useExternalStoreRuntime).mock.lastCall![0] as unknown as {
        isRunning: boolean;
        onNew: (message: AppendMessage) => Promise<void>;
    };
}

function getSSEOptions() {
    return vi.mocked(useSSE).mock.lastCall![1]!;
}

function createTextMessage(text: string) {
    return {content: [{text, type: 'text'}]} as unknown as AppendMessage;
}

describe('AiAgentTestRuntimeProvider', () => {
    beforeEach(() => {
        vi.mocked(useSSE).mockReturnValue({
            close: vi.fn(),
            connectionState: 'CLOSED',
            data: null,
            error: null,
            errorStatus: null,
        });

        useAiAgentTestingChatStore.setState({messages: []});
    });

    afterEach(() => {
        vi.clearAllMocks();
    });

    it('shows the questions and keeps the test running until the agent finishes its turn', async () => {
        render(<AiAgentTestRuntimeProvider>child</AiAgentTestRuntimeProvider>);

        await act(async () => {
            await getRuntimeOptions().onNew(createTextMessage('Pick a color'));
        });

        act(() => {
            getSSEOptions().eventHandlers!.ask_user_question({
                questions: [{header: 'Color', multiSelect: false, options: [], question: 'Which color?'}],
            });
        });

        expect(getRuntimeOptions().isRunning).toBe(true);
        expect(vi.mocked(useSSE).mock.lastCall![0]).not.toBeNull();

        act(() => {
            getSSEOptions().eventHandlers!.stream('Answer in your next message.');
            getSSEOptions().eventHandlers!.result('Answer in your next message.');
        });

        const {messages} = useAiAgentTestingChatStore.getState();
        const lastMessageContent = JSON.stringify(messages[messages.length - 1].content);

        expect(lastMessageContent).toContain('**Color**: Which color?');
        expect(lastMessageContent).toContain('Answer in your next message.');
        expect(getRuntimeOptions().isRunning).toBe(false);

        await act(async () => {
            await getRuntimeOptions().onNew(createTextMessage('Blue'));
        });

        const streamRequest = vi.mocked(useSSE).mock.lastCall![0]!;

        expect(streamRequest.url).toBe(AI_AGENT_TESTS_URL);
        expect(JSON.parse(streamRequest.init!.body as string)).toMatchObject({message: 'Blue'});
        expect(getRuntimeOptions().isRunning).toBe(true);
    });

    it('shows the final result of the next turn when the stream asking a question closed without a result', async () => {
        render(<AiAgentTestRuntimeProvider>child</AiAgentTestRuntimeProvider>);

        await act(async () => {
            await getRuntimeOptions().onNew(createTextMessage('Pick a color'));
        });

        act(() => {
            getSSEOptions().eventHandlers!.ask_user_question({
                questions: [{header: 'Color', multiSelect: false, options: [], question: 'Which color?'}],
            });
        });

        act(() => {
            getSSEOptions().onClose!();
        });

        await act(async () => {
            await getRuntimeOptions().onNew(createTextMessage('answer'));
        });

        act(() => {
            getSSEOptions().eventHandlers!.result('Final');
        });

        const {messages} = useAiAgentTestingChatStore.getState();

        expect(JSON.stringify(messages[messages.length - 1].content)).toContain('Final');
        expect(getRuntimeOptions().isRunning).toBe(false);
    });

    it('replaces the streamed text with the final result when no question was asked', async () => {
        render(<AiAgentTestRuntimeProvider>child</AiAgentTestRuntimeProvider>);

        await act(async () => {
            await getRuntimeOptions().onNew(createTextMessage('Hi'));
        });

        act(() => {
            getSSEOptions().eventHandlers!.stream('Hel');
            getSSEOptions().eventHandlers!.result('Hello there.');
        });

        const {messages} = useAiAgentTestingChatStore.getState();

        expect(JSON.stringify(messages[messages.length - 1].content)).toContain('Hello there.');
    });

    it('stops running when the stream ends', async () => {
        render(<AiAgentTestRuntimeProvider>child</AiAgentTestRuntimeProvider>);

        await act(async () => {
            await getRuntimeOptions().onNew(createTextMessage('Hi'));
        });

        act(() => {
            getSSEOptions().onClose!();
        });

        expect(getRuntimeOptions().isRunning).toBe(false);
        expect(vi.mocked(useSSE).mock.lastCall![0]).toBeNull();
    });

    it('tells the user the response ended unexpectedly when the stream ends without any event', async () => {
        render(<AiAgentTestRuntimeProvider>child</AiAgentTestRuntimeProvider>);

        await act(async () => {
            await getRuntimeOptions().onNew(createTextMessage('Hi'));
        });

        act(() => {
            getSSEOptions().onClose!();
        });

        const {messages} = useAiAgentTestingChatStore.getState();

        expect(messages[messages.length - 1]).toMatchObject({
            role: 'assistant',
            status: {error: 'The response ended unexpectedly.'},
        });
    });

    it('keeps the streamed answer when the stream ends after it', async () => {
        render(<AiAgentTestRuntimeProvider>child</AiAgentTestRuntimeProvider>);

        await act(async () => {
            await getRuntimeOptions().onNew(createTextMessage('Hi'));
        });

        act(() => {
            getSSEOptions().eventHandlers!.stream('Hello there.');
        });

        act(() => {
            getSSEOptions().onClose!();
        });

        const {messages} = useAiAgentTestingChatStore.getState();

        expect(messages[messages.length - 1]).not.toHaveProperty('status');
    });

    it('shows an error and stops running when the ask_user_question event is malformed', async () => {
        render(<AiAgentTestRuntimeProvider>child</AiAgentTestRuntimeProvider>);

        await act(async () => {
            await getRuntimeOptions().onNew(createTextMessage('Pick a color'));
        });

        act(() => {
            getSSEOptions().eventHandlers!.ask_user_question({questions: 'not a list'});
        });

        const {messages} = useAiAgentTestingChatStore.getState();

        expect(messages[messages.length - 1]).toMatchObject({
            role: 'assistant',
            status: {error: 'The agent asked a question in an unexpected format.'},
        });
        expect(getRuntimeOptions().isRunning).toBe(false);
    });

    it('shows the connection failure and stops running when the stream fails', async () => {
        const {rerender} = render(<AiAgentTestRuntimeProvider>child</AiAgentTestRuntimeProvider>);

        await act(async () => {
            await getRuntimeOptions().onNew(createTextMessage('Hi'));
        });

        vi.mocked(useSSE).mockReturnValue({
            close: vi.fn(),
            connectionState: 'ERROR',
            data: null,
            error: 'HTTP 500',
            errorStatus: 500,
        });

        rerender(<AiAgentTestRuntimeProvider>child</AiAgentTestRuntimeProvider>);

        const {messages} = useAiAgentTestingChatStore.getState();

        expect(messages[messages.length - 1]).toMatchObject({
            role: 'assistant',
            status: {error: 'Connection to the AI agent test failed: HTTP 500'},
        });
        expect(getRuntimeOptions().isRunning).toBe(false);
    });
});
