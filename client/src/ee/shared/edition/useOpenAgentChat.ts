import {AiHubChatI, toChat} from '@/ee/pages/automation/ai-hub/chats/api/chats.api';
import {AiHubChatsKeys} from '@/ee/pages/automation/ai-hub/chats/hooks/useChats';
import {aiHubChatsStore} from '@/ee/pages/automation/ai-hub/chats/stores/useAiHubChatsStore';
import {aiHubStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {OpenAgentChatParamsI} from '@/shared/edition/agent-chat/agentChatApi';
import {useCreateAgentChatAiHubChatMutation} from '@/shared/middleware/graphql';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {useNavigate} from 'react-router-dom';

const useOpenAgentChat = (): ((params: OpenAgentChatParamsI) => void) | null => {
    const aiHubEnabled = useApplicationInfoStore((state) => state.ai.hub.enabled);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const createAgentChatMutation = useCreateAgentChatAiHubChatMutation();

    const navigate = useNavigate();
    const queryClient = useQueryClient();

    if (!aiHubEnabled) {
        return null;
    }

    const openAgentChat = async ({projectDeploymentId, title, workflowExecutionId}: OpenAgentChatParamsI) => {
        if (currentWorkspaceId == null) {
            return;
        }

        let result;

        try {
            result = await createAgentChatMutation.mutateAsync({
                environment: currentEnvironmentId,
                projectDeploymentId,
                title,
                workflowExecutionId,
                workspaceId: String(currentWorkspaceId),
            });
        } catch {
            return;
        }

        const chat = toChat(result.createAgentChatAiHubChat);

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
    };

    return (params: OpenAgentChatParamsI) => {
        void openAgentChat(params);
    };
};

export default useOpenAgentChat;
