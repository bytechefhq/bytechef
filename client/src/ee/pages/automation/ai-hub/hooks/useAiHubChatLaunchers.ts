import {AiHubChatsKeys} from '@/ee/pages/automation/ai-hub/chats/hooks/useChats';
import {aiHubChatsStore} from '@/ee/pages/automation/ai-hub/chats/stores/useAiHubChatsStore';
import {aiHubStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {ModelPickerAgentChatI, ModelPickerWorkflowChatI} from '@/shared/components/ai/model-picker/ModelPicker';
import {
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

/**
 * Feeds the model picker's two chat-launcher cascades (Agents, Workflows) and owns what picking one does:
 * create the chat server-side — as kind=AGENT_CHAT or kind=WORKFLOW_CHAT respectively — point the AI Hub stores
 * at it, and navigate.
 *
 * Shared by the home composer and the in-chat composer (AiHubHomePanel / AiHubPanel) so the launcher is
 * reachable from anywhere the picker is — starting another kind of chat shouldn't require going home first.
 * Every pick starts a NEW conversation and navigates away from the current one; nothing here touches the
 * active chat's model override, which the pickers wire separately.
 */
export const useAiHubChatLaunchers = (): AiHubChatLaunchersI => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const navigate = useNavigate();
    const queryClient = useQueryClient();

    const {data: chatWorkflowsData} = useWorkspaceChatWorkflowsQuery(
        {environmentId: String(currentEnvironmentId), workspaceId: String(currentWorkspaceId ?? 0)},
        {enabled: currentWorkspaceId != null}
    );
    // Agent chats are a disjoint set from workflow chats, not a subset: agents live in hidden
    // `__AI_AGENT__` projects, which every workspace-scoped project-deployment listing (and therefore
    // `workspaceChatWorkflows`) filters out by name. So the two cascades never show the same row twice.
    const {data: chatAgentsData} = useWorkspaceChatAgentsQuery(
        {environmentId: String(currentEnvironmentId), workspaceId: String(currentWorkspaceId ?? 0)},
        {enabled: currentWorkspaceId != null}
    );

    const createWorkflowChatMutation = useCreateWorkflowChatAiHubChatMutation();
    const createAgentChatMutation = useCreateAgentChatAiHubChatMutation();

    // Pre-shape into the picker's minimal interface so the picker stays decoupled from the codegen types.
    const pickerWorkflowChats = useMemo(
        () =>
            (chatWorkflowsData?.workspaceChatWorkflows ?? []).map((chat) => ({
                label: `${chat.projectName} — ${chat.workflowLabel}`,
                projectDeploymentId: chat.projectDeploymentId,
                workflowExecutionId: chat.workflowExecutionId,
            })),
        [chatWorkflowsData]
    );

    // The agent's TITLE is the label — it becomes the chat's title on first creation, and "Agent1" reads
    // better in the chat list than the "project — workflow" form workflow chats use (the backing project is
    // an implementation detail named `__AI_AGENT__<uuid>`, which the user should never see).
    const pickerAgentChats = useMemo(
        () =>
            (chatAgentsData?.workspaceChatAgents ?? []).map((chatAgent) => ({
                label: chatAgent.agentTitle,
                projectDeploymentId: chatAgent.projectDeploymentId,
                workflowExecutionId: chatAgent.workflowExecutionId,
            })),
        [chatAgentsData]
    );

    /**
     * Lands the client on a freshly created chat. Shared by both cascades because the only thing that differs
     * between them is which mutation minted the row.
     */
    const openCreatedChat = useCallback(
        (chat: {id: string; threadId: string}) => {
            aiHubStore.setState({
                chatId: chat.threadId,
                messages: [],
            });

            aiHubChatsStore.getState().setCurrentChatId(Number(chat.id));

            queryClient.invalidateQueries({
                queryKey: [...AiHubChatsKeys.all, 'list', currentWorkspaceId],
            });

            navigate(`/automation/ai-hub/chats/${chat.id}`);
        },
        [currentWorkspaceId, navigate, queryClient]
    );

    const handleSelectWorkflowChat = useCallback(
        async (workflowExecutionId: string, projectDeploymentId: string, label: string) => {
            if (currentWorkspaceId == null) {
                return;
            }

            const result = await createWorkflowChatMutation.mutateAsync({
                environment: currentEnvironmentId,
                projectDeploymentId,
                title: label,
                workflowExecutionId,
                workspaceId: String(currentWorkspaceId),
            });

            openCreatedChat(result.createWorkflowChatAiHubChat);
        },
        [createWorkflowChatMutation, currentEnvironmentId, currentWorkspaceId, openCreatedChat]
    );

    const handleSelectAgentChat = useCallback(
        async (workflowExecutionId: string, projectDeploymentId: string, label: string) => {
            if (currentWorkspaceId == null) {
                return;
            }

            // Same (workflowExecutionId, projectDeploymentId, title) triple and the same webhook bridge serves the
            // turns — a separate mutation only so the row is stamped kind=AGENT_CHAT. That stamp is what lets the
            // sidebar and header keep calling it an agent chat after the agent is undeployed, which the earlier
            // client-side derivation could not.
            const result = await createAgentChatMutation.mutateAsync({
                environment: currentEnvironmentId,
                projectDeploymentId,
                title: label,
                workflowExecutionId,
                workspaceId: String(currentWorkspaceId),
            });

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
