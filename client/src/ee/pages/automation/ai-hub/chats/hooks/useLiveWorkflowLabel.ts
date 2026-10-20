import {AiHubChatI} from '@/ee/pages/automation/ai-hub/chats/api/chats.api';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useHasWorkspaceScope} from '@/shared/hooks/useHasWorkspaceScope';
import {useLoadWorkspaceScopes} from '@/shared/hooks/useLoadWorkspaceScopes';
import {useWorkspaceChatWorkflowsQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useMemo} from 'react';

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
