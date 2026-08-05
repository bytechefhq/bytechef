import {AiHubTaskI} from '@/ee/pages/automation/ai-hub/tasks/api/tasks.api';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useWorkspaceChatWorkflowsQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useMemo} from 'react';

/**
 * Resolves the LIVE display label ({@code "${projectName} — ${workflowLabel}"}) for the workflow a workflow-chat
 * task is bound to. Returns {@code null} for non-workflow-chat tasks and when the workflow can't
 * be found in the workspace's chat-enabled list (deleted, no longer chat-enabled, or in a different environment).
 *
 * <p>The task's stored {@code title} is set on first creation and is preserved across the
 * idempotent-restore path so a user-renamed task keeps its name. The trade-off: if the workflow is
 * renamed AFTER the task was created, the stored title goes stale. This hook exposes the live label so
 * a stale title can surface as supplementary info (e.g. in a tooltip) without overwriting the user's rename.</p>
 *
 * <p>Only fires the underlying query for workflow-chat tasks; standard tasks short-circuit and
 * the query stays disabled. The query is workspace-cached so 30 workflow-chat rows share one fetch.</p>
 */
export function useLiveWorkflowLabel(task: AiHubTaskI): string | null {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const isWorkflowChat = task.kind === 'WORKFLOW_CHAT' && task.workflowExecutionId != null;

    const {data} = useWorkspaceChatWorkflowsQuery(
        {
            environmentId: String(currentEnvironmentId),
            workspaceId: String(currentWorkspaceId ?? 0),
        },
        {enabled: isWorkflowChat && currentWorkspaceId != null}
    );

    return useMemo(() => {
        if (!isWorkflowChat || !data?.workspaceChatWorkflows) {
            return null;
        }

        const matching = data.workspaceChatWorkflows.find(
            (chatWorkflow) => chatWorkflow.workflowExecutionId === task.workflowExecutionId
        );

        if (!matching) {
            // Workflow no longer chat-enabled or doesn't exist in this workspace+environment. Returning null
            // means the tooltip won't show a "current label" line, which is the right behaviour: the
            // deleted-workflow message in the task thread already covers this case.
            return null;
        }

        return `${matching.projectName} — ${matching.workflowLabel}`;
    }, [task.workflowExecutionId, data, isWorkflowChat]);
}
