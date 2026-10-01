import {useSSE} from '@/shared/hooks/useSSE';
import {AppendMessage} from '@assistant-ui/react';
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
});
