import {aiHubChatsStore} from '@/ee/pages/automation/ai-hub/chats/stores/useAiHubChatsStore';
import {aiHubStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubStore';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {act, renderHook} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {createMutateAsyncMock, navigateMock} = vi.hoisted(() => ({
    createMutateAsyncMock: vi.fn(),
    navigateMock: vi.fn(),
}));

vi.mock('react-router-dom', () => ({
    useNavigate: () => navigateMock,
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: {currentWorkspaceId: number}) => unknown) =>
        selector({currentWorkspaceId: 3}),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: {currentEnvironmentId: number}) => unknown) =>
        selector({currentEnvironmentId: 2}),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useCreateWorkflowChatAiHubChatMutation: () => ({mutateAsync: createMutateAsyncMock}),
    useWorkspaceChatWorkflowsQuery: () => ({
        data: {
            workspaceChatWorkflows: [
                {
                    projectDeploymentId: '5',
                    projectId: '1',
                    projectName: 'Acme',
                    projectWorkflowId: '8',
                    workflowExecutionId: 'exec-1',
                    workflowId: '9',
                    workflowLabel: 'Support',
                },
            ],
        },
    }),
}));

const {useAiHubChatLaunchers} = await import('../useAiHubChatLaunchers');

const wrapper = ({children}: {children: ReactNode}) => (
    <QueryClientProvider client={new QueryClient()}>{children}</QueryClientProvider>
);

describe('useAiHubChatLaunchers', () => {
    beforeEach(() => {
        createMutateAsyncMock.mockReset();
        navigateMock.mockReset();
        aiHubChatsStore.getState().setCurrentChatId(undefined);
    });

    it('labels workflow chats as "project - workflow" with an em dash', () => {
        const {result} = renderHook(() => useAiHubChatLaunchers(), {wrapper});

        expect(result.current.workflowChats).toEqual([
            {label: 'Acme — Support', projectDeploymentId: '5', workflowExecutionId: 'exec-1'},
        ]);
    });

    it('creates a chat titled with the label on select and navigates to it', async () => {
        createMutateAsyncMock.mockResolvedValue({createWorkflowChatAiHubChat: {id: '9', threadId: 'thread-9'}});

        const {result} = renderHook(() => useAiHubChatLaunchers(), {wrapper});

        await act(async () => {
            await result.current.onSelectWorkflowChat('exec-1', '5', 'Acme — Support');
        });

        expect(createMutateAsyncMock).toHaveBeenCalledWith({
            environment: 2,
            projectDeploymentId: '5',
            title: 'Acme — Support',
            workflowExecutionId: 'exec-1',
            workspaceId: '3',
        });
        expect(navigateMock).toHaveBeenCalledWith('/automation/ai-hub/chats/9');
        expect(aiHubChatsStore.getState().currentChatId).toBe(9);
        expect(aiHubStore.getState().chatId).toBe('thread-9');
    });

    it('settles without switching chats when creating the workflow chat fails', async () => {
        createMutateAsyncMock.mockRejectedValue(new Error('Workflow is not deployed'));

        aiHubStore.setState({chatId: 'thread-home'});

        const {result} = renderHook(() => useAiHubChatLaunchers(), {wrapper});

        await act(async () => {
            await expect(result.current.onSelectWorkflowChat('exec-1', '5', 'Acme — Support')).resolves.toBeUndefined();
        });

        expect(navigateMock).not.toHaveBeenCalled();
        expect(aiHubChatsStore.getState().currentChatId).toBeUndefined();
        expect(aiHubStore.getState().chatId).toBe('thread-home');
    });
});
