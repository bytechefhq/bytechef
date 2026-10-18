import {TooltipProvider} from '@/components/ui/tooltip';
import {hasPendingToolApproval} from '@/ee/pages/automation/ai-hub/chats/pendingToolApproval';
import {aiHubChatsStore} from '@/ee/pages/automation/ai-hub/chats/stores/useAiHubChatsStore';
import AiHubChatComposer from '@/ee/pages/automation/ai-hub/composer/AiHubChatComposer';
import {aiHubRunStateStore} from '@/ee/pages/automation/ai-hub/runtime-providers/stores/useAiHubRunStateStore';
import {MODE, aiHubStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {
    type ApprovalResolutionContextI,
    useApprovalResolution,
} from '@/shared/components/ai-chat/approvalResolutionContext';
import {ThreadMessageLike} from '@assistant-ui/react';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {act, render, screen} from '@testing-library/react';
import {ReactNode} from 'react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {AiHubRuntimeProvider} from '../AiHubRuntimeProvider';

const hoisted = vi.hoisted(() => ({
    approvalResolutionRef: {current: null as ApprovalResolutionContextI | null},
    resolveMutateAsync: vi.fn(),
}));

vi.mock('@assistant-ui/react', () => ({
    AssistantRuntimeProvider: ({children}: {children: ReactNode}) => <div>{children}</div>,
    ComposerPrimitive: {
        Input: (props: Record<string, unknown>) => <textarea {...props} />,
        Root: ({children}: {children: ReactNode}) => <form>{children}</form>,
        Send: ({children}: {children: ReactNode}) => <>{children}</>,
    },
    useAui: () => ({
        composer: {getState: () => ({text: ''}), setText: vi.fn()},
        thread: {cancelRun: vi.fn()},
    }),
    useExternalStoreRuntime: vi.fn(() => ({thread: {composer: {setText: vi.fn()}}})),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useCancelAiHubRunMutation: () => ({mutate: vi.fn()}),
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

const renderProviderWithComposer = () =>
    render(
        <QueryClientProvider client={new QueryClient()}>
            <TooltipProvider>
                <AiHubRuntimeProvider>
                    <ApprovalResolutionCapture />

                    <AiHubChatComposer />
                </AiHubRuntimeProvider>
            </TooltipProvider>
        </QueryClientProvider>
    );

const approvalPartData = () => {
    const message = aiHubStore.getState().messages[1];
    const parts = message.content as unknown as Array<{data: Record<string, unknown>}>;

    return parts[0].data;
};

describe('AiHubRuntimeProvider resolving a tool approval live', () => {
    beforeEach(() => {
        useWorkspaceStore.setState({currentWorkspaceId: 1});

        aiHubRunStateStore.getState().reset();
        aiHubChatsStore.getState().reset();
        aiHubChatsStore.getState().setCurrentChatId(5);
        aiHubStore.setState({
            chatId: 'thread-1',
            messages: [{content: 'delete the old workflow', role: 'user'}, pendingApprovalMessage],
            mode: MODE.BUILD,
        });

        hoisted.approvalResolutionRef.current = null;
    });

    afterEach(() => {
        vi.clearAllMocks();
    });

    it.each([
        {approved: false, status: 'REJECTED'},
        {approved: true, status: 'APPROVED'},
    ])('stamps $status onto the approval part and clears the composer prompt', async ({approved, status}) => {
        hoisted.resolveMutateAsync.mockResolvedValue({
            resolveAiHubToolApproval: {
                approval: {executionError: null, status},
                continuationStarted: false,
            },
        });

        renderProviderWithComposer();

        expect(screen.getByRole('textbox', {name: 'Message input'})).toHaveAttribute(
            'placeholder',
            'Approve or reject the tool call above, or send a message to withdraw it...'
        );

        await act(async () => {
            await hoisted.approvalResolutionRef.current!.resolveToolApproval!(APPROVAL_ID, approved);
        });

        expect(approvalPartData().resolvedStatus).toBe(status);
        expect(hasPendingToolApproval(aiHubStore.getState().messages)).toBe(false);
        expect(screen.getByRole('textbox', {name: 'Message input'})).toHaveAttribute(
            'placeholder',
            'Send a message...'
        );
    });

    it('keeps the approval pending when the resolve mutation fails', async () => {
        hoisted.resolveMutateAsync.mockRejectedValue(new Error('Approval already resolved'));

        renderProviderWithComposer();

        await act(async () => {
            await expect(
                hoisted.approvalResolutionRef.current!.resolveToolApproval!(APPROVAL_ID, false)
            ).rejects.toThrow('Approval already resolved');
        });

        expect(approvalPartData().resolvedStatus).toBeUndefined();
        expect(hasPendingToolApproval(aiHubStore.getState().messages)).toBe(true);
        expect(screen.getByRole('textbox', {name: 'Message input'})).toHaveAttribute(
            'placeholder',
            'Approve or reject the tool call above, or send a message to withdraw it...'
        );
    });
});
