import {aiHubChatsStore} from '@/ee/pages/automation/ai-hub/chats/stores/useAiHubChatsStore';
import {
    aiHubRunStateStore,
    isChatRunning,
} from '@/ee/pages/automation/ai-hub/runtime-providers/stores/useAiHubRunStateStore';
import {MODE, aiHubStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {
    type ApprovalResolutionContextI,
    useApprovalResolution,
} from '@/shared/components/ai-chat/approvalResolutionContext';
import {aiChatRetryableErrorStore} from '@/shared/components/ai-chat/stores/useAiChatRetryableErrorStore';
import {EventType} from '@ag-ui/core';
import {ThreadMessageLike, useExternalStoreRuntime} from '@assistant-ui/react';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {act, render} from '@testing-library/react';
import {ReactNode} from 'react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {AiHubRuntimeProvider} from '../AiHubRuntimeProvider';

const hoisted = vi.hoisted(() => ({
    approvalResolutionRef: {current: null as ApprovalResolutionContextI | null},
    resolveMutateAsync: vi.fn(),
}));

vi.mock('@assistant-ui/react', () => ({
    AssistantRuntimeProvider: ({children}: {children: ReactNode}) => <div>{children}</div>,
    useExternalStoreRuntime: vi.fn(() => ({thread: {composer: {setText: vi.fn()}}})),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useResolveAiHubToolApprovalMutation: () => ({mutateAsync: hoisted.resolveMutateAsync}),
}));

vi.mock('@/ee/pages/automation/ai-hub/chats/api/chats.api', () => ({
    createAiHubChat: vi.fn(),
    generateAiHubChatTitle: vi.fn(() => Promise.resolve()),
    patchChat: vi.fn(() => Promise.resolve()),
}));

vi.mock('sonner', () => ({
    toast: {error: vi.fn(), info: vi.fn()},
}));

class FakeEventSource {
    static instances: FakeEventSource[] = [];

    closed = false;
    onerror: ((event: Event) => void) | null = null;
    onmessage: ((event: MessageEvent) => void) | null = null;
    url: string;

    constructor(url: string) {
        this.url = url;

        FakeEventSource.instances.push(this);
    }

    close() {
        this.closed = true;
    }

    emit(payload: unknown) {
        if (!this.closed) {
            this.onmessage?.({data: JSON.stringify(payload)} as MessageEvent);
        }
    }
}

const APPROVAL_ID = 4;

const pendingApprovalMessage = {
    content: [
        {
            data: {
                approvalId: APPROVAL_ID,
                arguments: {},
                awaitingApproval: true,
                kind: 'tool-approval-request',
                toolName: 'deleteWorkflow',
            },
            type: 'data-tool-approval-request',
        },
    ],
    role: 'assistant',
} as ThreadMessageLike;

const ApprovalResolutionCapture = () => {
    hoisted.approvalResolutionRef.current = useApprovalResolution();

    return null;
};

function getRuntimeOptions() {
    return vi.mocked(useExternalStoreRuntime).mock.lastCall![0] as unknown as {onCancel: () => Promise<void>};
}

async function flushHandlers() {
    await act(async () => {
        await new Promise((resolve) => setTimeout(resolve, 0));
    });
}

describe('AiHubRuntimeProvider stopping an approval continuation', () => {
    beforeEach(() => {
        FakeEventSource.instances = [];

        vi.stubGlobal('EventSource', FakeEventSource);

        useWorkspaceStore.setState({currentWorkspaceId: 1});

        aiHubRunStateStore.getState().reset();
        aiHubChatsStore.getState().reset();
        aiHubChatsStore.getState().setCurrentChatId(5);
        aiHubStore.setState({
            chatId: 'thread-1',
            messages: [{content: 'delete the old workflow', role: 'user'}, pendingApprovalMessage],
            mode: MODE.BUILD,
        });
        aiChatRetryableErrorStore.setState({currentError: undefined});

        hoisted.approvalResolutionRef.current = null;
        hoisted.resolveMutateAsync.mockResolvedValue({
            resolveAiHubToolApproval: {
                approval: {executionError: null, status: 'APPROVED'},
                continuationStarted: true,
            },
        });
    });

    afterEach(() => {
        vi.unstubAllGlobals();
        vi.clearAllMocks();
    });

    it('closes the continuation stream, so a run end after Stop raises no retry banner', async () => {
        render(
            <QueryClientProvider client={new QueryClient()}>
                <AiHubRuntimeProvider>
                    <ApprovalResolutionCapture />
                </AiHubRuntimeProvider>
            </QueryClientProvider>
        );

        await act(async () => {
            await hoisted.approvalResolutionRef.current!.resolveToolApproval!(APPROVAL_ID, true);
        });

        const continuationEventSource = FakeEventSource.instances[0]!;

        expect(continuationEventSource.url).toBe('/api/platform/internal/ai/chat/ai_hub/thread-1/attach');
        expect(isChatRunning(aiHubRunStateStore.getState(), 'thread-1')).toBe(true);

        continuationEventSource.emit({runId: 'run-2', threadId: 'thread-1', type: EventType.RUN_STARTED});
        continuationEventSource.emit({
            toolCallId: 'call-1',
            toolCallName: 'selectConnection',
            type: EventType.TOOL_CALL_START,
        });
        continuationEventSource.emit({
            content: JSON.stringify({error: 'Connection lookup failed'}),
            messageId: 'tool-message-1',
            role: 'tool',
            toolCallId: 'call-1',
            type: EventType.TOOL_CALL_RESULT,
        });

        await flushHandlers();

        await act(async () => {
            await getRuntimeOptions().onCancel();
        });

        expect(continuationEventSource.closed).toBe(true);
        expect(isChatRunning(aiHubRunStateStore.getState(), 'thread-1')).toBe(false);

        continuationEventSource.emit({runId: 'run-2', threadId: 'thread-1', type: EventType.RUN_FINISHED});

        await flushHandlers();

        expect(aiChatRetryableErrorStore.getState().currentError).toBeUndefined();
    });

    it("records the continuation's run id, so Stop names the continuation rather than the previous turn", async () => {
        aiHubRunStateStore.getState().setChatRunId('thread-1', 'run-1');

        hoisted.resolveMutateAsync.mockResolvedValue({
            resolveAiHubToolApproval: {
                approval: {executionError: null, status: 'APPROVED'},
                continuationStarted: true,
                runId: 'run-2',
            },
        });

        render(
            <QueryClientProvider client={new QueryClient()}>
                <AiHubRuntimeProvider>
                    <ApprovalResolutionCapture />
                </AiHubRuntimeProvider>
            </QueryClientProvider>
        );

        await act(async () => {
            await hoisted.approvalResolutionRef.current!.resolveToolApproval!(APPROVAL_ID, true);
        });

        expect(aiHubRunStateStore.getState().runIdByChat['thread-1']).toBe('run-2');
    });
});
