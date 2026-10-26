import {useSSE} from '@/shared/hooks/useSSE';
import {AppendMessage, ThreadMessageLike} from '@assistant-ui/react';
import {act, renderHook} from '@testing-library/react';
import {afterEach, describe, expect, it, vi} from 'vitest';

vi.mock('@/shared/hooks/useSSE', () => ({
    useSSE: vi.fn(() => ({close: vi.fn(), connectionState: 'CLOSED', data: null, error: null})),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: vi.fn((selector) => selector({currentEnvironmentId: 42})),
}));

import {useAgentTestChatRuntime} from './runtime-providers/AgentTestChatRuntimeProvider';

const mockUseSSE = vi.mocked(useSSE);

const buildAppendMessage = (text: string): AppendMessage =>
    ({
        attachments: [],
        content: [{text, type: 'text'}],
        role: 'user',
    }) as unknown as AppendMessage;

const getLastStreamRequest = () => {
    const lastCall = mockUseSSE.mock.calls.at(-1);

    return lastCall?.[0] as {init?: RequestInit; url: string} | null | undefined;
};

describe('useAgentTestChatRuntime', () => {
    afterEach(() => {
        mockUseSSE.mockClear();
    });

    it('posts to /workflows/<draftWorkflowId>/tests with inputs.chat_1.message set to the typed text', async () => {
        const {result} = renderHook(() => useAgentTestChatRuntime('draft-workflow-1'));

        await act(async () => {
            await result.current.onNew(buildAppendMessage('Hello there'));
        });

        const request = getLastStreamRequest();

        expect(request?.url).toContain('/workflows/draft-workflow-1/tests');

        const body = JSON.parse(request?.init?.body as string);

        expect(body.inputs.chat_1.message).toBe('Hello there');
    });

    it('keeps conversationId stable across two sends', async () => {
        const {result} = renderHook(() => useAgentTestChatRuntime('draft-workflow-1'));

        await act(async () => {
            await result.current.onNew(buildAppendMessage('First message'));
        });

        const firstBody = JSON.parse(getLastStreamRequest()?.init?.body as string);

        await act(async () => {
            await result.current.onNew(buildAppendMessage('Second message'));
        });

        const secondBody = JSON.parse(getLastStreamRequest()?.init?.body as string);

        expect(firstBody.inputs.chat_1.conversationId).toBeTruthy();
        expect(secondBody.inputs.chat_1.conversationId).toBe(firstBody.inputs.chat_1.conversationId);
        expect(secondBody.inputs.chat_1.message).toBe('Second message');
    });

    it('appends the user message to local state immediately, before the stream resolves', async () => {
        const {result} = renderHook(() => useAgentTestChatRuntime('draft-workflow-1'));

        await act(async () => {
            await result.current.onNew(buildAppendMessage('Hi'));
        });

        const userMessage = result.current.messages.find((message) => message.role === 'user');

        expect(userMessage?.content).toBe('Hi');
        expect(result.current.isRunning).toBe(true);
    });

    describe('when the run fails', () => {
        const getEventHandlers = () => {
            const lastCall = mockUseSSE.mock.calls.at(-1);

            return (lastCall?.[1] as {eventHandlers: Record<string, (data: unknown) => void>}).eventHandlers;
        };

        const getOnRequestError = () => {
            const lastCall = mockUseSSE.mock.calls.at(-1);

            return (lastCall?.[1] as {onRequestError: (status: number | null) => void}).onRequestError;
        };

        const getLastAssistantMessage = (messages: ThreadMessageLike[]) =>
            messages.findLast((message) => message.role === 'assistant');

        it('shows the error event message on the assistant reply', async () => {
            const {result} = renderHook(() => useAgentTestChatRuntime('draft-workflow-1'));

            await act(async () => {
                await result.current.onNew(buildAppendMessage('Hi'));
            });

            act(() => {
                getEventHandlers().error('Aborted');
            });

            expect(getLastAssistantMessage(result.current.messages)?.status).toEqual({
                error: 'Aborted',
                reason: 'error',
                type: 'incomplete',
            });
            expect(result.current.isRunning).toBe(false);
        });

        it('shows the failed task error when the result carries a failed job', async () => {
            const {result} = renderHook(() => useAgentTestChatRuntime('draft-workflow-1'));

            await act(async () => {
                await result.current.onNew(buildAppendMessage('Hi'));
            });

            act(() => {
                getEventHandlers().result({
                    job: {
                        error: null,
                        outputs: null,
                        status: 'FAILED',
                        taskExecutions: [{error: null}, {error: {message: 'modelConnection is null'}}],
                    },
                });
            });

            expect(getLastAssistantMessage(result.current.messages)?.status).toEqual({
                error: 'modelConnection is null',
                reason: 'error',
                type: 'incomplete',
            });
        });

        it('shows an error on the assistant reply when the test request fails', async () => {
            const {result} = renderHook(() => useAgentTestChatRuntime('draft-workflow-1'));

            await act(async () => {
                await result.current.onNew(buildAppendMessage('Hi'));
            });

            act(() => {
                getOnRequestError()(500);
            });

            expect(getLastAssistantMessage(result.current.messages)?.status).toMatchObject({
                error: 'The test request failed with status 500.',
            });
            expect(result.current.isRunning).toBe(false);
            expect(getLastStreamRequest()).toBeNull();

            await act(async () => {
                await result.current.onNew(buildAppendMessage('Again'));
            });

            act(() => {
                getOnRequestError()(null);
            });

            expect(getLastAssistantMessage(result.current.messages)?.status).toMatchObject({
                error: 'The connection to the agent was lost.',
            });
        });

        it('prefers the job error and falls back to a generic message', async () => {
            const {result} = renderHook(() => useAgentTestChatRuntime('draft-workflow-1'));

            await act(async () => {
                await result.current.onNew(buildAppendMessage('Hi'));
            });

            act(() => {
                getEventHandlers().result({job: {error: {message: 'Job failed'}, status: 'FAILED'}});
            });

            expect(getLastAssistantMessage(result.current.messages)?.status).toMatchObject({error: 'Job failed'});

            await act(async () => {
                await result.current.onNew(buildAppendMessage('Again'));
            });

            act(() => {
                getEventHandlers().result({job: {status: 'FAILED'}});
            });

            expect(getLastAssistantMessage(result.current.messages)?.status).toMatchObject({
                error: 'The agent run failed.',
            });
        });
    });
});
