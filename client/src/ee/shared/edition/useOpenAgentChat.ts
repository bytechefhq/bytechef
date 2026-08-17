import {AiHubChatsKeys} from '@/ee/pages/automation/ai-hub/chats/hooks/useChats';
import {aiHubChatsStore} from '@/ee/pages/automation/ai-hub/chats/stores/useAiHubChatsStore';
import {aiHubStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {OpenAgentChatParamsI} from '@/shared/edition/agent-chat/agentChatApi';
import {useCreateWorkflowChatAiHubChatMutation} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {useNavigate} from 'react-router-dom';

/**
 * EE implementation of the agent-chat seam: opens the deployment's chat as an AI Hub chat rather than
 * the standalone hosted chat page.
 *
 * Mirrors AiHubHomePanel's provider-popup handler, which is the other way into a workflow chat — same mutation,
 * same store priming, same invalidation. The mutation is idempotent on the server, so re-opening a deployment's
 * chat returns the existing chat instead of piling up duplicates.
 */
const useOpenAgentChat = () => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const createMutation = useCreateWorkflowChatAiHubChatMutation();

    const navigate = useNavigate();
    const queryClient = useQueryClient();

    return async ({projectDeploymentId, title, workflowExecutionId}: OpenAgentChatParamsI) => {
        if (currentWorkspaceId == null) {
            return;
        }

        const result = await createMutation.mutateAsync({
            environment: currentEnvironmentId,
            projectDeploymentId,
            title,
            workflowExecutionId,
            workspaceId: String(currentWorkspaceId),
        });

        const chat = result.createWorkflowChatAiHubChat;

        // Primed BEFORE navigating so AiHub's URL→store sync effect does not see a mismatch and switch back.
        aiHubStore.setState({
            chatId: chat.threadId,
            messages: [],
        });

        aiHubChatsStore.getState().setCurrentChatId(Number(chat.id));

        queryClient.invalidateQueries({
            queryKey: [...AiHubChatsKeys.all, 'list', currentWorkspaceId],
        });

        navigate(`/automation/ai-hub/chats/${chat.id}`);
    };
};

export default useOpenAgentChat;
