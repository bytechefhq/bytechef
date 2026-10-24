import {createAiHubChat} from '@/ee/pages/automation/ai-hub/chats/api/chats.api';
import {aiHubChatsStore} from '@/ee/pages/automation/ai-hub/chats/stores/useAiHubChatsStore';
import {aiHubRunStateStore} from '@/ee/pages/automation/ai-hub/runtime-providers/stores/useAiHubRunStateStore';
import {MODE, aiHubStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {
    type ApprovalResolutionContextI,
    useApprovalResolution,
} from '@/shared/components/ai-chat/approvalResolutionContext';
import {AppendMessage, ThreadMessageLike, useExternalStoreRuntime} from '@assistant-ui/react';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {act, render} from '@testing-library/react';
import {ReactNode} from 'react';
import {type MockInstance, afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

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

vi.mock('@/ee/pages/automation/ai-hub/chats/api/chats.api', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/ee/pages/automation/ai-hub/chats/api/chats.api')>()),
    createAiHubChat: vi.fn(),
    generateAiHubChatTitle: vi.fn(() => Promise.resolve()),
    patchChat: vi.fn(() => Promise.resolve()),
}));

vi.mock('sonner', () => ({
    toast: {error: vi.fn(), info: vi.fn()},
}));

const APPROVAL_ID = 4;

const OTHER_CHAT_MESSAGES: ThreadMessageLike[] = [
    {content: 'what runs nightly?', role: 'user'},
    {content: 'The Orders sync runs nightly.', role: 'assistant'},
];

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

const renderProvider = () =>
    render(
        <QueryClientProvider client={new QueryClient()}>
            <AiHubRuntimeProvider>
                <ApprovalResolutionCapture />
            </AiHubRuntimeProvider>
        </QueryClientProvider>
    );

function createDeferred<T>() {
    let resolve!: (value: T) => void;

    const promise = new Promise<T>((resolvePromise) => {
        resolve = resolvePromise;
    });

    return {promise, resolve};
}

const switchToOtherChat = () =>
    act(() => {
        aiHubChatsStore.getState().setCurrentChatId(9);
        aiHubStore.setState({chatId: 'thread-2', messages: OTHER_CHAT_MESSAGES});
    });

describe('AiHubRuntimeProvider when a response arrives after the user switched chats', () => {
    let fetchSpy: MockInstance<typeof fetch>;

    beforeEach(() => {
        useWorkspaceStore.setState({currentWorkspaceId: 1});

        aiHubRunStateStore.getState().reset();
        aiHubChatsStore.getState().reset();

        hoisted.approvalResolutionRef.current = null;

        fetchSpy = vi.spyOn(globalThis, 'fetch').mockRejectedValue(new TypeError('offline'));
    });

    afterEach(() => {
        vi.restoreAllMocks();
        vi.clearAllMocks();
    });

    it('does not focus the created chat or start the turn when the first message create resolves late', async () => {
        aiHubStore.setState({chatId: 'thread-1', messages: [], mode: MODE.ASK});

        const createChatDeferred = createDeferred<{id: number}>();

        vi.mocked(createAiHubChat).mockReturnValue(
            createChatDeferred.promise as unknown as ReturnType<typeof createAiHubChat>
        );

        renderProvider();

        const runtimeOptions = vi.mocked(useExternalStoreRuntime).mock.lastCall![0] as unknown as {
            onNew: (message: AppendMessage) => Promise<void>;
        };

        let sendPromise: Promise<void> | undefined;

        act(() => {
            sendPromise = runtimeOptions.onNew({
                content: [{text: 'Build me a workflow', type: 'text'}],
            } as unknown as AppendMessage);
        });

        switchToOtherChat();

        await act(async () => {
            createChatDeferred.resolve({id: 5});

            await sendPromise;
        });

        expect(aiHubChatsStore.getState().currentChatId).toBe(9);
        expect(aiHubStore.getState().messages).toEqual(OTHER_CHAT_MESSAGES);
        expect(aiHubRunStateStore.getState().runningByChat['thread-1']).toBeFalsy();
        expect(fetchSpy.mock.calls.filter(([input]) => String(input).includes('/internal/ai/chat/ai_hub'))).toEqual([]);
    });

    it('leaves the chat the user switched to untouched when the first message create fails late', async () => {
        aiHubStore.setState({chatId: 'thread-1', messages: [], mode: MODE.ASK});

        let rejectCreateChat!: (error: Error) => void;

        vi.mocked(createAiHubChat).mockReturnValue(
            new Promise((_resolve, reject) => {
                rejectCreateChat = reject;
            }) as unknown as ReturnType<typeof createAiHubChat>
        );

        renderProvider();

        const runtimeOptions = vi.mocked(useExternalStoreRuntime).mock.lastCall![0] as unknown as {
            onNew: (message: AppendMessage) => Promise<void>;
        };

        let sendPromise: Promise<void> | undefined;

        act(() => {
            sendPromise = runtimeOptions.onNew({
                content: [{text: 'Build me a workflow', type: 'text'}],
            } as unknown as AppendMessage);
        });

        switchToOtherChat();

        await act(async () => {
            rejectCreateChat(new Error('database down'));

            await sendPromise;
        });

        expect(aiHubStore.getState().chatId).toBe('thread-2');
        expect(aiHubStore.getState().messages).toEqual(OTHER_CHAT_MESSAGES);
    });

    it('does not write a late approval resolution into the chat the user switched to', async () => {
        aiHubChatsStore.getState().setCurrentChatId(5);
        aiHubStore.setState({
            chatId: 'thread-1',
            messages: [{content: 'delete the old workflow', role: 'user'}, pendingApprovalMessage],
            mode: MODE.BUILD,
        });

        const resolveDeferred = createDeferred<unknown>();

        hoisted.resolveMutateAsync.mockReturnValue(resolveDeferred.promise);

        renderProvider();

        let resolutionPromise: Promise<unknown> | undefined;

        act(() => {
            resolutionPromise = hoisted.approvalResolutionRef.current!.resolveToolApproval!(APPROVAL_ID, true);
        });

        switchToOtherChat();

        let resolution: unknown;

        await act(async () => {
            resolveDeferred.resolve({
                resolveAiHubToolApproval: {
                    approval: {executionError: null, status: 'APPROVED'},
                    continuationStarted: true,
                },
            });

            resolution = await resolutionPromise;
        });

        expect(resolution).toEqual({executionError: null, status: 'APPROVED'});
        expect(aiHubStore.getState().messages).toEqual(OTHER_CHAT_MESSAGES);
        expect(aiHubChatsStore.getState().chatActivity['thread-1']).toBeUndefined();
    });
});
