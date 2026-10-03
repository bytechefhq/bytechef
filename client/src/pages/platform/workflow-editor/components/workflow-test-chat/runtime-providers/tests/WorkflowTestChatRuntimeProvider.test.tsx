import useWorkflowTestChatStore from '@/pages/platform/workflow-editor/stores/useWorkflowTestChatStore';
import {UseWorkflowTestStreamProps, useWorkflowTestStream} from '@/shared/hooks/useWorkflowTestStream';
import {getTestWorkflowStreamPostRequest} from '@/shared/util/testWorkflow-utils';
import {AppendMessage, useExternalStoreRuntime} from '@assistant-ui/react';
import {act, render} from '@testing-library/react';
import {ReactNode} from 'react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {WorkflowTestChatRuntimeProvider} from '../WorkflowTestChatRuntimeProvider';

const hoisted = vi.hoisted(() => ({
    setStreamRequest: vi.fn(),
    setWorkflowIsRunning: vi.fn(),
    streamError: {current: null as string | null},
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowDataStore', () => ({
    default: vi.fn((selector) => selector({workflow: {id: 'workflow-1'}})),
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowEditorStore', () => ({
    default: vi.fn((selector) => selector({setWorkflowIsRunning: hoisted.setWorkflowIsRunning})),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: vi.fn((selector) => selector({currentEnvironmentId: 1})),
}));

vi.mock('@/shared/hooks/useWorkflowTestStream', () => ({
    useWorkflowTestStream: vi.fn(() => ({
        close: vi.fn(),
        error: hoisted.streamError.current,
        getPersistedJobId: vi.fn(),
        persistJobId: vi.fn(),
        setStreamRequest: hoisted.setStreamRequest,
    })),
}));

vi.mock('@/shared/util/testWorkflow-utils', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/util/testWorkflow-utils')>();

    return {
        ...actual,
        getTestWorkflowStreamPostRequest: vi.fn(actual.getTestWorkflowStreamPostRequest),
    };
});

vi.mock('@assistant-ui/react', () => ({
    AssistantRuntimeProvider: ({children}: {children: ReactNode}) => <div>{children}</div>,
    CompositeAttachmentAdapter: vi.fn(),
    SimpleImageAttachmentAdapter: vi.fn(),
    SimpleTextAttachmentAdapter: vi.fn(),
    Suggestions: vi.fn(),
    useAui: vi.fn(() => ({})),
    useExternalStoreRuntime: vi.fn(() => ({})),
}));

function getRuntimeOptions() {
    return vi.mocked(useExternalStoreRuntime).mock.lastCall![0] as unknown as {
        isRunning: boolean;
        onNew: (message: AppendMessage) => Promise<void>;
    };
}

function getStreamProps(): UseWorkflowTestStreamProps {
    return vi.mocked(useWorkflowTestStream).mock.lastCall![0];
}

function createTextMessage(text: string) {
    return {content: [{text, type: 'text'}]} as unknown as AppendMessage;
}

describe('WorkflowTestChatRuntimeProvider', () => {
    beforeEach(() => {
        hoisted.streamError.current = null;

        useWorkflowTestChatStore.setState({messages: []});
    });

    afterEach(() => {
        vi.clearAllMocks();
    });

    it('starts a test run for each message', async () => {
        render(<WorkflowTestChatRuntimeProvider>child</WorkflowTestChatRuntimeProvider>);

        await act(async () => {
            await getRuntimeOptions().onNew(createTextMessage('Blue'));
        });

        expect(hoisted.setStreamRequest).toHaveBeenCalledTimes(1);
        expect(hoisted.setWorkflowIsRunning).toHaveBeenCalledWith(true);
        expect(getRuntimeOptions().isRunning).toBe(true);

        const {messages} = useWorkflowTestChatStore.getState();

        expect(messages[messages.length - 1]).toEqual({content: '', role: 'assistant'});
    });

    it('shows an error and stops running when the test request cannot be built', async () => {
        const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

        vi.mocked(getTestWorkflowStreamPostRequest).mockImplementationOnce(() => {
            throw new Error('Invalid request');
        });

        render(<WorkflowTestChatRuntimeProvider>child</WorkflowTestChatRuntimeProvider>);

        await act(async () => {
            await getRuntimeOptions().onNew(createTextMessage('Blue'));
        });

        const {messages} = useWorkflowTestChatStore.getState();

        expect(hoisted.setStreamRequest).not.toHaveBeenCalled();
        expect(getRuntimeOptions().isRunning).toBe(false);
        expect(hoisted.setWorkflowIsRunning).toHaveBeenLastCalledWith(false);
        expect(messages[messages.length - 1]).toEqual({
            content: 'Failed to send your message. Please try again.',
            role: 'assistant',
        });

        consoleErrorSpy.mockRestore();
    });

    it('stops running when the stream ends', async () => {
        render(<WorkflowTestChatRuntimeProvider>child</WorkflowTestChatRuntimeProvider>);

        await act(async () => {
            await getRuntimeOptions().onNew(createTextMessage('Blue'));
        });

        act(() => {
            getStreamProps().onStreamEnd!();
        });

        expect(getRuntimeOptions().isRunning).toBe(false);
        expect(hoisted.setWorkflowIsRunning).toHaveBeenLastCalledWith(false);
    });

    it('tells the user the response ended unexpectedly when the stream ends without any event', async () => {
        render(<WorkflowTestChatRuntimeProvider>child</WorkflowTestChatRuntimeProvider>);

        await act(async () => {
            await getRuntimeOptions().onNew(createTextMessage('Blue'));
        });

        act(() => {
            getStreamProps().onStreamEnd!();
        });

        const {messages} = useWorkflowTestChatStore.getState();

        expect(messages[messages.length - 1]).toEqual({
            content: 'The response ended unexpectedly.',
            role: 'assistant',
        });
    });

    it('shows the message of an error event in the assistant bubble', async () => {
        render(<WorkflowTestChatRuntimeProvider>child</WorkflowTestChatRuntimeProvider>);

        await act(async () => {
            await getRuntimeOptions().onNew(createTextMessage('Blue'));
        });

        act(() => {
            getStreamProps().onError!('The model call failed');
            getStreamProps().onStreamEnd!();
        });

        const {messages} = useWorkflowTestChatStore.getState();

        expect(messages[messages.length - 1]).toEqual({content: 'The model call failed', role: 'assistant'});
        expect(getRuntimeOptions().isRunning).toBe(false);
    });

    it('keeps the streamed text when the stream fails part way', async () => {
        const {rerender} = render(<WorkflowTestChatRuntimeProvider>child</WorkflowTestChatRuntimeProvider>);

        await act(async () => {
            await getRuntimeOptions().onNew(createTextMessage('Hi'));
        });

        act(() => {
            useWorkflowTestChatStore.getState().appendToLastAssistantMessage('Partial answer');
        });

        hoisted.streamError.current = 'Connection error occurred';

        rerender(<WorkflowTestChatRuntimeProvider>child</WorkflowTestChatRuntimeProvider>);

        const {messages} = useWorkflowTestChatStore.getState();

        expect(messages[messages.length - 1]).toEqual({
            content: 'Partial answer\n\nThe request failed: Connection error occurred',
            role: 'assistant',
        });
        expect(getRuntimeOptions().isRunning).toBe(false);
    });

    it('stops running after each of two consecutive identical stream failures', async () => {
        const {rerender} = render(<WorkflowTestChatRuntimeProvider>child</WorkflowTestChatRuntimeProvider>);

        for (const text of ['Hi', 'Hi again']) {
            await act(async () => {
                await getRuntimeOptions().onNew(createTextMessage(text));
            });

            hoisted.streamError.current = null;

            rerender(<WorkflowTestChatRuntimeProvider>child</WorkflowTestChatRuntimeProvider>);

            expect(getRuntimeOptions().isRunning).toBe(true);
            expect(hoisted.setWorkflowIsRunning).toHaveBeenLastCalledWith(true);

            hoisted.streamError.current = 'HTTP 500';

            rerender(<WorkflowTestChatRuntimeProvider>child</WorkflowTestChatRuntimeProvider>);

            expect(getRuntimeOptions().isRunning).toBe(false);
            expect(hoisted.setWorkflowIsRunning).toHaveBeenLastCalledWith(false);

            const {messages} = useWorkflowTestChatStore.getState();

            expect(messages[messages.length - 1]).toMatchObject({
                content: 'The request failed: HTTP 500',
                role: 'assistant',
            });
        }
    });
});
