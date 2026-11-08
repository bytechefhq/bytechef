import {AiHubChatI} from '@/ee/pages/automation/ai-hub/chats/api/chats.api';
import {AiHubChatsKeys} from '@/ee/pages/automation/ai-hub/chats/hooks/useChats';
import {aiHubChatsStore} from '@/ee/pages/automation/ai-hub/chats/stores/useAiHubChatsStore';
import {aiHubStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubStore';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {act, renderHook} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {
    chatAgentsQueryMock,
    chatWorkflowsQueryMock,
    createAgentChatMutateAsyncMock,
    createMutateAsyncMock,
    hasWorkspaceScopeMock,
    navigateMock,
    workspaceRef,
} = vi.hoisted(() => ({
    chatAgentsQueryMock: vi.fn(),
    chatWorkflowsQueryMock: vi.fn(),
    createAgentChatMutateAsyncMock: vi.fn(),
    createMutateAsyncMock: vi.fn(),
    hasWorkspaceScopeMock: vi.fn(),
    navigateMock: vi.fn(),
    workspaceRef: {currentWorkspaceId: 3 as number | null},
}));

const chatAgentsData = {
    workspaceChatAgents: [
        {
            agentName: 'support-agent',
            agentTitle: 'Support Agent',
            aiAgentId: '11',
            projectDeploymentId: '6',
            workflowExecutionId: 'exec-agent-1',
            workflowLabel: 'Agent1',
        },
    ],
};

const chatWorkflowsData = {
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
        {
            projectDeploymentId: '6',
            projectId: '2',
            projectName: 'Agents',
            projectWorkflowId: '10',
            workflowExecutionId: 'exec-agent-1',
            workflowId: '12',
            workflowLabel: 'Agent1',
        },
    ],
};

vi.mock('react-router-dom', () => ({
    useNavigate: () => navigateMock,
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: {currentWorkspaceId: number | null}) => unknown) =>
        selector({currentWorkspaceId: workspaceRef.currentWorkspaceId}),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: {currentEnvironmentId: number}) => unknown) =>
        selector({currentEnvironmentId: 2}),
}));

vi.mock('@/shared/hooks/useHasWorkspaceScope', () => ({
    useHasWorkspaceScope: hasWorkspaceScopeMock,
}));

vi.mock('@/shared/hooks/useLoadWorkspaceScopes', () => ({
    useLoadWorkspaceScopes: () => ({error: false, loading: false}),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useCreateAgentChatAiHubChatMutation: () => ({mutateAsync: createAgentChatMutateAsyncMock}),
    useCreateWorkflowChatAiHubChatMutation: () => ({mutateAsync: createMutateAsyncMock}),
    useWorkspaceChatAgentsQuery: chatAgentsQueryMock,
    useWorkspaceChatWorkflowsQuery: chatWorkflowsQueryMock,
}));

const {useAiHubChatLaunchers} = await import('../useAiHubChatLaunchers');

let queryClient = new QueryClient();

const wrapper = ({children}: {children: ReactNode}) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
);

describe('useAiHubChatLaunchers', () => {
    beforeEach(() => {
        chatWorkflowsQueryMock.mockReset();
        chatWorkflowsQueryMock.mockImplementation((_variables: unknown, options: {enabled?: boolean}) => ({
            data: options.enabled ? chatWorkflowsData : undefined,
        }));
        chatAgentsQueryMock.mockReset();
        chatAgentsQueryMock.mockImplementation((_variables: unknown, options: {enabled?: boolean}) => ({
            data: options.enabled ? chatAgentsData : undefined,
        }));
        createAgentChatMutateAsyncMock.mockReset();
        hasWorkspaceScopeMock.mockReset();
        hasWorkspaceScopeMock.mockReturnValue(true);
        createMutateAsyncMock.mockReset();
        navigateMock.mockReset();
        workspaceRef.currentWorkspaceId = 3;
        queryClient = new QueryClient();
        aiHubChatsStore.getState().setCurrentChatId(undefined);
    });

    it('labels workflow chats as "project - workflow" with an em dash', () => {
        const {result} = renderHook(() => useAiHubChatLaunchers(), {wrapper});

        expect(result.current.workflowChats).toEqual([
            {label: 'Acme — Support', projectDeploymentId: '5', workflowExecutionId: 'exec-1'},
        ]);
    });

    it('lists no workflow chats and never runs the listing for a user without WORKFLOW_VIEW', () => {
        hasWorkspaceScopeMock.mockReturnValue(false);

        const {result} = renderHook(() => useAiHubChatLaunchers(), {wrapper});

        expect(hasWorkspaceScopeMock).toHaveBeenCalledWith(3, 'WORKFLOW_VIEW');
        expect(chatWorkflowsQueryMock).toHaveBeenCalledWith(expect.anything(), {enabled: false});
        expect(chatAgentsQueryMock).toHaveBeenCalledWith(expect.anything(), {enabled: false});
        expect(result.current.workflowChats).toEqual([]);
        expect(result.current.agentChats).toEqual([]);
    });

    it('labels agent chats with the agent title and keeps their workflows out of the workflow chats', () => {
        const {result} = renderHook(() => useAiHubChatLaunchers(), {wrapper});

        expect(result.current.agentChats).toEqual([
            {label: 'Support Agent', projectDeploymentId: '6', workflowExecutionId: 'exec-agent-1'},
        ]);
        expect(result.current.workflowChats.map((workflowChat) => workflowChat.workflowExecutionId)).toEqual([
            'exec-1',
        ]);
    });

    it('creates an agent chat through the agent chat mutation and navigates to it', async () => {
        createAgentChatMutateAsyncMock.mockResolvedValue({
            createAgentChatAiHubChat: {id: '12', kind: 'AGENT_CHAT', threadId: 'thread-12'},
        });

        const {result} = renderHook(() => useAiHubChatLaunchers(), {wrapper});

        await act(async () => {
            await result.current.onSelectAgentChat('exec-agent-1', '6', 'Support Agent');
        });

        expect(createAgentChatMutateAsyncMock).toHaveBeenCalledWith({
            environment: 2,
            projectDeploymentId: '6',
            title: 'Support Agent',
            workflowExecutionId: 'exec-agent-1',
            workspaceId: '3',
        });
        expect(createMutateAsyncMock).not.toHaveBeenCalled();
        expect(navigateMock).toHaveBeenCalledWith('/automation/ai-hub/chats/12');
        expect(aiHubChatsStore.getState().currentChatId).toBe(12);
        expect(aiHubStore.getState().chatId).toBe('thread-12');
    });

    it('settles without switching chats when creating the agent chat fails', async () => {
        createAgentChatMutateAsyncMock.mockRejectedValue(new Error('Agent is not deployed'));

        aiHubStore.setState({chatId: 'thread-home'});

        const {result} = renderHook(() => useAiHubChatLaunchers(), {wrapper});

        await act(async () => {
            await expect(
                result.current.onSelectAgentChat('exec-agent-1', '6', 'Support Agent')
            ).resolves.toBeUndefined();
        });

        expect(navigateMock).not.toHaveBeenCalled();
        expect(aiHubStore.getState().chatId).toBe('thread-home');
    });

    it('creates a chat titled with the label on select and navigates to it', async () => {
        createMutateAsyncMock.mockResolvedValue({createWorkflowChatAiHubChat: {id: '9', threadId: 'thread-9'}});

        const invalidateQueriesSpy = vi.spyOn(queryClient, 'invalidateQueries');

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
        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: [...AiHubChatsKeys.all, 'list', 3]});
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

    it('does nothing on select while no workspace is chosen', async () => {
        workspaceRef.currentWorkspaceId = null;

        aiHubStore.setState({chatId: 'thread-home'});

        const invalidateQueriesSpy = vi.spyOn(queryClient, 'invalidateQueries');

        const {result} = renderHook(() => useAiHubChatLaunchers(), {wrapper});

        await act(async () => {
            await result.current.onSelectWorkflowChat('exec-1', '5', 'Acme — Support');
        });

        expect(createMutateAsyncMock).not.toHaveBeenCalled();
        expect(invalidateQueriesSpy).not.toHaveBeenCalled();
        expect(navigateMock).not.toHaveBeenCalled();
        expect(aiHubStore.getState().chatId).toBe('thread-home');
    });

    it('seeds the active chats list with the new workflow chat before navigating', async () => {
        const existingChat = {id: 4, kind: 'STANDARD'} as AiHubChatI;

        queryClient.setQueryData(AiHubChatsKeys.list(3, 2, 'ACTIVE'), [existingChat]);

        createMutateAsyncMock.mockResolvedValue({
            createWorkflowChatAiHubChat: {
                autoTitled: true,
                createdAt: 1760947200000,
                environmentId: '2',
                id: '9',
                isOwner: true,
                kind: 'WORKFLOW_CHAT',
                lastPreview: null,
                messageCount: 0,
                ownerUserId: '1',
                projectDeploymentId: '5',
                status: 'ACTIVE',
                threadId: 'thread-9',
                title: 'Acme — Support',
                updatedAt: 1760947200000,
                userId: '1',
                workflowExecutionId: 'exec-1',
                workspaceId: '3',
            },
        });

        let seededChatsAtNavigation: AiHubChatI[] | undefined;

        navigateMock.mockImplementation(() => {
            seededChatsAtNavigation = queryClient.getQueryData<AiHubChatI[]>(AiHubChatsKeys.list(3, 2, 'ACTIVE'));
        });

        const {result} = renderHook(() => useAiHubChatLaunchers(), {wrapper});

        await act(async () => {
            await result.current.onSelectWorkflowChat('exec-1', '5', 'Acme — Support');
        });

        expect(seededChatsAtNavigation).toHaveLength(2);
        expect(seededChatsAtNavigation![0]).toMatchObject({
            id: 9,
            kind: 'WORKFLOW_CHAT',
            threadId: 'thread-9',
            title: 'Acme — Support',
            workflowExecutionId: 'exec-1',
        });
        expect(seededChatsAtNavigation![1]).toBe(existingChat);
    });
});
