import {AiHubChatI, toChat} from '@/ee/pages/automation/ai-hub/chats/api/chats.api';
import {AiHubChatsKeys} from '@/ee/pages/automation/ai-hub/chats/hooks/useChats';
import {aiHubChatsStore} from '@/ee/pages/automation/ai-hub/chats/stores/useAiHubChatsStore';
import {aiHubStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {ModelPickerAgentChatI, ModelPickerWorkflowChatI} from '@/shared/components/ai/model-picker/ModelPicker';
import {useHasWorkspaceScope} from '@/shared/hooks/useHasWorkspaceScope';
import {useLoadWorkspaceScopes} from '@/shared/hooks/useLoadWorkspaceScopes';
import {
    AiHubChatsQuery,
    useCreateAgentChatAiHubChatMutation,
    useCreateWorkflowChatAiHubChatMutation,
    useWorkspaceChatAgentsQuery,
    useWorkspaceChatWorkflowsQuery,
} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {useCallback, useMemo} from 'react';
import {useNavigate} from 'react-router-dom';

export interface AiHubChatLaunchersI {
    agentChats: ModelPickerAgentChatI[];
    onSelectAgentChat: (workflowExecutionId: string, projectDeploymentId: string, label: string) => void;
    onSelectWorkflowChat: (workflowExecutionId: string, projectDeploymentId: string, label: string) => void;
    workflowChats: ModelPickerWorkflowChatI[];
}

export const useAiHubChatLaunchers = (): AiHubChatLaunchersI => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const navigate = useNavigate();
    const queryClient = useQueryClient();

    useLoadWorkspaceScopes(currentWorkspaceId);

    const canViewWorkflows = useHasWorkspaceScope(currentWorkspaceId, 'WORKFLOW_VIEW');

    const {data: chatWorkflowsData} = useWorkspaceChatWorkflowsQuery(
        {environmentId: String(currentEnvironmentId), workspaceId: String(currentWorkspaceId ?? 0)},
        {enabled: currentWorkspaceId != null && canViewWorkflows}
    );

    const {data: chatAgentsData} = useWorkspaceChatAgentsQuery(
        {environmentId: String(currentEnvironmentId), workspaceId: String(currentWorkspaceId ?? 0)},
        {enabled: currentWorkspaceId != null && canViewWorkflows}
    );

    const createAgentChatMutation = useCreateAgentChatAiHubChatMutation();
    const createWorkflowChatMutation = useCreateWorkflowChatAiHubChatMutation();

    const pickerAgentChats = useMemo(
        () =>
            (chatAgentsData?.workspaceChatAgents ?? []).map((chatAgent) => ({
                label: chatAgent.agentTitle,
                projectDeploymentId: chatAgent.projectDeploymentId,
                workflowExecutionId: chatAgent.workflowExecutionId,
            })),
        [chatAgentsData]
    );

    const pickerWorkflowChats = useMemo(() => {
        const agentWorkflowExecutionIds = new Set(pickerAgentChats.map((chatAgent) => chatAgent.workflowExecutionId));

        return (chatWorkflowsData?.workspaceChatWorkflows ?? [])
            .filter((chat) => !agentWorkflowExecutionIds.has(chat.workflowExecutionId))
            .map((chat) => ({
                label: `${chat.projectName} — ${chat.workflowLabel}`,
                projectDeploymentId: chat.projectDeploymentId,
                workflowExecutionId: chat.workflowExecutionId,
            }));
    }, [chatWorkflowsData, pickerAgentChats]);

    const openCreatedChat = useCallback(
        (createdChat: AiHubChatsQuery['aiHubChats'][number]) => {
            if (currentWorkspaceId == null) {
                return;
            }

            const chat = toChat(createdChat);

            queryClient.setQueryData<AiHubChatI[]>(
                AiHubChatsKeys.list(currentWorkspaceId, currentEnvironmentId, 'ACTIVE'),
                (cachedChats) => [chat, ...(cachedChats ?? []).filter((cachedChat) => cachedChat.id !== chat.id)]
            );

            aiHubStore.setState({
                chatId: chat.threadId,
                messages: [],
            });

            aiHubChatsStore.getState().setCurrentChatId(chat.id);

            queryClient.invalidateQueries({
                queryKey: [...AiHubChatsKeys.all, 'list', currentWorkspaceId],
            });

            navigate(`/automation/ai-hub/chats/${chat.id}`);
        },
        [currentEnvironmentId, currentWorkspaceId, navigate, queryClient]
    );

    const handleSelectWorkflowChat = useCallback(
        async (workflowExecutionId: string, projectDeploymentId: string, label: string) => {
            if (currentWorkspaceId == null) {
                return;
            }

            let result;

            try {
                result = await createWorkflowChatMutation.mutateAsync({
                    environment: currentEnvironmentId,
                    projectDeploymentId,
                    title: label,
                    workflowExecutionId,
                    workspaceId: String(currentWorkspaceId),
                });
            } catch {
                return;
            }

            openCreatedChat(result.createWorkflowChatAiHubChat);
        },
        [createWorkflowChatMutation, currentEnvironmentId, currentWorkspaceId, openCreatedChat]
    );

    const handleSelectAgentChat = useCallback(
        async (workflowExecutionId: string, projectDeploymentId: string, label: string) => {
            if (currentWorkspaceId == null) {
                return;
            }

            let result;

            try {
                result = await createAgentChatMutation.mutateAsync({
                    environment: currentEnvironmentId,
                    projectDeploymentId,
                    title: label,
                    workflowExecutionId,
                    workspaceId: String(currentWorkspaceId),
                });
            } catch {
                return;
            }

            openCreatedChat(result.createAgentChatAiHubChat);
        },
        [createAgentChatMutation, currentEnvironmentId, currentWorkspaceId, openCreatedChat]
    );

    return {
        agentChats: pickerAgentChats,
        onSelectAgentChat: handleSelectAgentChat,
        onSelectWorkflowChat: handleSelectWorkflowChat,
        workflowChats: pickerWorkflowChats,
    };
};

export default useAiHubChatLaunchers;
