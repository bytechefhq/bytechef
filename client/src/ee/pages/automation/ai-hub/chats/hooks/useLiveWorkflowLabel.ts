import {AiHubChatI} from '@/ee/pages/automation/ai-hub/chats/api/chats.api';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useHasWorkspaceScope} from '@/shared/hooks/useHasWorkspaceScope';
import {useLoadWorkspaceScopes} from '@/shared/hooks/useLoadWorkspaceScopes';
import {useWorkspaceChatWorkflowsQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useMemo} from 'react';

/**
 * Resolves the live display label ({@code "${projectName} — ${workflowLabel}"}) for the workflow a workflow chat is
 * bound to. Returns {@code null} for standard chats and when the workflow is not in the workspace's chat-enabled
 * list (deleted, no longer chat-enabled, or in a different environment).
 *
 * <p>The chat's stored title is set on creation and preserved when the user renames the chat, so it goes stale if
 * the workflow is renamed later. This hook exposes the live label without overwriting the user's rename.</p>
 *
 * <p>The chat workflow listing requires the WORKFLOW_VIEW workspace scope, so without it the hook returns null and
 * the sidebar shows the stored title. The query only runs for workflow chats and is workspace-cached, so many
 * workflow chat rows share one fetch.</p>
 */
export function useLiveWorkflowLabel(chat: AiHubChatI): string | null {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    useLoadWorkspaceScopes(currentWorkspaceId);

    const canViewWorkflows = useHasWorkspaceScope(currentWorkspaceId, 'WORKFLOW_VIEW');

    const isWorkflowChat = chat.kind === 'WORKFLOW_CHAT' && chat.workflowExecutionId != null;

    const {data} = useWorkspaceChatWorkflowsQuery(
        {
            environmentId: String(currentEnvironmentId),
            workspaceId: String(currentWorkspaceId ?? 0),
        },
        {enabled: isWorkflowChat && canViewWorkflows && currentWorkspaceId != null}
    );

    return useMemo(() => {
        if (!isWorkflowChat || !canViewWorkflows || !data?.workspaceChatWorkflows) {
            return null;
        }

        const matching = data.workspaceChatWorkflows.find(
            (chatWorkflow) => chatWorkflow.workflowExecutionId === chat.workflowExecutionId
        );

        if (!matching) {
            return null;
        }

        return `${matching.projectName} — ${matching.workflowLabel}`;
    }, [canViewWorkflows, chat.workflowExecutionId, data, isWorkflowChat]);
}

export default useLiveWorkflowLabel;
