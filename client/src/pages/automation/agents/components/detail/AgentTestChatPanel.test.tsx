import {useSSE} from '@/shared/hooks/useSSE';
import {AppendMessage} from '@assistant-ui/react';
import {act, renderHook} from '@testing-library/react';
import {afterEach, describe, expect, it, vi} from 'vitest';

/**
 * AgentTestChatPanel itself is a thin wrapper (header + suggestion list) around AgentTestChatRuntimeProvider and
 * assistant-ui's <Thread>. All the behavior worth pinning — which request a send builds, and that the
 * conversation stays coherent across turns — lives in the runtime provider's `useAgentTestChatRuntime` hook,
 * so these tests exercise that hook directly via `renderHook` rather than mounting the full assistant-ui
 * Thread tree (composer, message list, etc.), which would add rendering weight without covering anything
 * this hook doesn't already prove.
 */

vi.mock('@/shared/hooks/useSSE', () => ({
    useSSE: vi.fn(() => ({close: vi.fn(), connectionState: 'CLOSED', data: null, error: null})),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: vi.fn((selector) => selector({currentEnvironmentId: 42})),
}));

import {useAgentTestChatRuntime} from './runtime-providers/AgentTestChatRuntimeProvider';

const mockUseSSE = vi.mocked(useSSE);

/** Minimal AppendMessage fixture: onNew only reads `content[0]` (text) and `attachments`. */
const buildAppendMessage = (text: string): AppendMessage =>
    ({
        attachments: [],
        content: [{text, type: 'text'}],
        role: 'user',
    }) as unknown as AppendMessage;

/** The streamRequest passed to the most recent useSSE call, or undefined if it was never invoked with one. */
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
});
