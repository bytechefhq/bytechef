import {AiHubChatI, toChat} from '@/ee/pages/automation/ai-hub/chats/api/chats.api';
import {AiHubChatsKeys} from '@/ee/pages/automation/ai-hub/chats/hooks/useChats';
import {aiHubChatsStore} from '@/ee/pages/automation/ai-hub/chats/stores/useAiHubChatsStore';
import {aiHubStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {ModelPickerWorkflowChatI} from '@/shared/components/ai/model-picker/ModelPicker';
import {useHasWorkspaceScope} from '@/shared/hooks/useHasWorkspaceScope';
import {useLoadWorkspaceScopes} from '@/shared/hooks/useLoadWorkspaceScopes';
import {useCreateWorkflowChatAiHubChatMutation, useWorkspaceChatWorkflowsQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {useCallback, useMemo} from 'react';
import {useNavigate} from 'react-router-dom';

export interface AiHubChatLaunchersI {
    onSelectWorkflowChat: (workflowExecutionId: string, projectDeploymentId: string, label: string) => void;
    workflowChats: ModelPickerWorkflowChatI[];
}

/**
 * Feeds the model picker's workflow chat cascade and owns what picking one does: create the workflow chat
 * server-side, point the AI Hub stores at it, and navigate.
 *
 * Shared by the home composer and the in-chat composer so the launcher is reachable from anywhere the picker is.
 * Every pick starts a new conversation and navigates away from the current one. The new chat goes into the cached
 * active chats list first, so the chat view knows it is a workflow chat before the list refetches. A failed create leaves the current
 * chat in place; the global fetch interceptor already shows the error toast.
 *
 * The chat workflow listing requires the WORKFLOW_VIEW workspace scope, so it only runs once the loaded workspace
 * scopes grant it. A user without the scope gets an empty cascade instead of an access-denied toast on every mount.
 */
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

    const createWorkflowChatMutation = useCreateWorkflowChatAiHubChatMutation();

    const pickerWorkflowChats = useMemo(
        () =>
            (chatWorkflowsData?.workspaceChatWorkflows ?? []).map((chat) => ({
                label: `${chat.projectName} — ${chat.workflowLabel}`,
                projectDeploymentId: chat.projectDeploymentId,
                workflowExecutionId: chat.workflowExecutionId,
            })),
        [chatWorkflowsData]
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

            const chat = toChat(result.createWorkflowChatAiHubChat);

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
        [createWorkflowChatMutation, currentEnvironmentId, currentWorkspaceId, navigate, queryClient]
    );

    return {
        onSelectWorkflowChat: handleSelectWorkflowChat,
        workflowChats: pickerWorkflowChats,
    };
};

export default useAiHubChatLaunchers;
