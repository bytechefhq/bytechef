import useOpenAgentChat from '@/ee/shared/edition/useOpenAgentChat';
import {renderHook, waitFor} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    aiHubEnabled: true,
    aiHubSetState: vi.fn(),
    currentWorkspaceId: 5 as number | undefined,
    invalidateQueries: vi.fn(),
    mutateAsync: vi.fn(),
    navigate: vi.fn(),
    setCurrentChatId: vi.fn(),
    setQueryData: vi.fn(),
}));

vi.mock('@tanstack/react-query', () => ({
    useQueryClient: () => ({invalidateQueries: hoisted.invalidateQueries, setQueryData: hoisted.setQueryData}),
}));

vi.mock('react-router-dom', () => ({
    useNavigate: () => hoisted.navigate,
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useCreateAgentChatAiHubChatMutation: () => ({mutateAsync: hoisted.mutateAsync}),
}));

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: (selector: (state: {ai: {hub: {enabled: boolean}}}) => unknown) =>
        selector({ai: {hub: {enabled: hoisted.aiHubEnabled}}}),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: {currentEnvironmentId: number}) => unknown) =>
        selector({currentEnvironmentId: 1}),
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: {currentWorkspaceId: number | undefined}) => unknown) =>
        selector({currentWorkspaceId: hoisted.currentWorkspaceId}),
}));

vi.mock('@/ee/pages/automation/ai-hub/stores/useAiHubStore', () => ({
    aiHubStore: {setState: hoisted.aiHubSetState},
}));

vi.mock('@/ee/pages/automation/ai-hub/chats/stores/useAiHubChatsStore', () => ({
    aiHubChatsStore: {getState: () => ({setCurrentChatId: hoisted.setCurrentChatId})},
}));

const createdChat = {
    aiAgentId: '9',
    autoTitled: false,
    createdAt: 1761955200000,
    environmentId: '1',
    id: '42',
    isOwner: true,
    kind: 'AGENT_CHAT',
    lastPreview: null,
    messageCount: 0,
    ownerUserId: '7',
    projectDeploymentId: '3',
    status: 'ACTIVE',
    threadId: 'thread-42',
    title: 'Support agent',
    updatedAt: 1761955200000,
    userId: '7',
    workflowExecutionId: 'execution-1',
    workspaceId: '5',
};

const openParams = {projectDeploymentId: '3', title: 'Support agent', workflowExecutionId: 'execution-1'};

describe('useOpenAgentChat', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        hoisted.aiHubEnabled = true;
        hoisted.currentWorkspaceId = 5;
    });

    it('returns no opener when the AI Hub is disabled', () => {
        hoisted.aiHubEnabled = false;

        const {result} = renderHook(() => useOpenAgentChat());

        expect(result.current).toBeNull();
    });

    it('creates an agent chat and navigates to it', async () => {
        hoisted.mutateAsync.mockResolvedValue({createAgentChatAiHubChat: createdChat});

        const {result} = renderHook(() => useOpenAgentChat());

        result.current?.(openParams);

        await waitFor(() => expect(hoisted.navigate).toHaveBeenCalledWith('/automation/ai-hub/chats/42'));

        expect(hoisted.mutateAsync).toHaveBeenCalledWith({
            environment: 1,
            projectDeploymentId: '3',
            title: 'Support agent',
            workflowExecutionId: 'execution-1',
            workspaceId: '5',
        });
        expect(hoisted.aiHubSetState).toHaveBeenCalledWith({chatId: 'thread-42', messages: []});
        expect(hoisted.setCurrentChatId).toHaveBeenCalledWith(42);
        expect(hoisted.setQueryData).toHaveBeenCalledTimes(1);
        expect(hoisted.invalidateQueries).toHaveBeenCalledWith({queryKey: ['aiHubChats', 'list', 5]});
    });

    it('stays put when the agent chat cannot be created', async () => {
        hoisted.mutateAsync.mockRejectedValue(new Error('forbidden'));

        const {result} = renderHook(() => useOpenAgentChat());

        result.current?.(openParams);

        await waitFor(() => expect(hoisted.mutateAsync).toHaveBeenCalledTimes(1));

        expect(hoisted.navigate).not.toHaveBeenCalled();
        expect(hoisted.setCurrentChatId).not.toHaveBeenCalled();
    });

    it('does nothing without a current workspace', () => {
        hoisted.currentWorkspaceId = undefined;

        const {result} = renderHook(() => useOpenAgentChat());

        result.current?.(openParams);

        expect(hoisted.mutateAsync).not.toHaveBeenCalled();
    });
});
