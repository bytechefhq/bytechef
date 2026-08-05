import {Skeleton} from '@/components/ui/skeleton';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {aiHubStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubStore';
import {AiHubTasksKeys, useAiHubTasksQuery} from '@/ee/pages/automation/ai-hub/tasks/hooks/useTasks';
import {aiHubTasksStore} from '@/ee/pages/automation/ai-hub/tasks/stores/useAiHubTasksStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useCreateWorkflowChatAiHubTaskMutation, useWorkspaceChatWorkflowsQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {ArchiveRestoreIcon, WorkflowIcon} from 'lucide-react';
import {useMemo} from 'react';
import {useNavigate} from 'react-router-dom';

interface ProjectChatGroupI {
    projectName: string;
    workflows: Array<{
        projectDeploymentId: string;
        workflowExecutionId: string;
        workflowLabel: string;
    }>;
}

/**
 * Workflow Chats section in the AI Hub sidebar. Lists the workspace's chat-enabled workflows
 * grouped by project; clicking a row idempotently creates (or returns) a {@code kind = WORKFLOW_CHAT}
 * task for that workflow and routes to it. The task lives in the same
 * {@code ai_hub_task} table as standard tasks, so per-task features —
 * the artifact list, attached tools, the right-panel tab snapshots, the tasks sidebar entry —
 * all work uniformly across both flavours.
 *
 * <p>Server-side, the agent runtime detects {@code kind = WORKFLOW_CHAT} on the task row and
 * routes per-turn invocations through the {@code WebhookWorkflowExecutionFacade} adapter instead of
 * the LLM agent (see {@code WebhookBridgeAgent}). Both ask-user-question pause/resume and attachment
 * forwarding are wired through the bridge, so a workflow-chat task behaves like a first-class
 * CC task end to end.</p>
 *
 * <p>Empty state: when the workspace has no chat-enabled workflows yet, the section header still
 * renders with a short explanation pointing the user at the New Chat Request trigger — discovering
 * the feature requires seeing it. Hiding the section completely (the v1 behaviour) made workflow
 * chats invisible until somebody else set up a workflow with a chat trigger.</p>
 */
const WorkflowChatsList = () => {
    const navigate = useNavigate();
    const queryClient = useQueryClient();

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {data, isLoading} = useWorkspaceChatWorkflowsQuery({
        environmentId: String(currentEnvironmentId),
        workspaceId: String(currentWorkspaceId ?? 0),
    });

    const createMutation = useCreateWorkflowChatAiHubTaskMutation();

    // Load archived tasks to mark workflow rows that have an archived chat. Without this, the user
    // who archived a chat then comes back to the sidebar has no signal that re-clicking the workflow will
    // restore the previous task (vs. starting a fresh one). Cached separately from the active list
    // so toggling status filters in AiHubTasksSidebar doesn't invalidate this fetch.
    const {data: archivedTasks} = useAiHubTasksQuery(currentWorkspaceId, currentEnvironmentId, 'ARCHIVED');

    const archivedWorkflowExecutionIds = useMemo<Set<string>>(() => {
        const ids = new Set<string>();

        if (!archivedTasks) {
            return ids;
        }

        for (const task of archivedTasks) {
            if (task.kind === 'WORKFLOW_CHAT' && task.workflowExecutionId) {
                ids.add(task.workflowExecutionId);
            }
        }

        return ids;
    }, [archivedTasks]);

    const workflowsByProject = useMemo<Map<string, ProjectChatGroupI>>(() => {
        const result = new Map<string, ProjectChatGroupI>();

        if (isLoading || !data?.workspaceChatWorkflows) {
            return result;
        }

        for (const chatWorkflow of data.workspaceChatWorkflows) {
            const group = result.get(chatWorkflow.projectId) ?? {
                projectName: chatWorkflow.projectName,
                workflows: [],
            };

            group.workflows.push({
                projectDeploymentId: chatWorkflow.projectDeploymentId,
                workflowExecutionId: chatWorkflow.workflowExecutionId,
                workflowLabel: chatWorkflow.workflowLabel,
            });

            result.set(chatWorkflow.projectId, group);
        }

        return result;
    }, [isLoading, data]);

    const handleSelect = async (
        workflowExecutionId: string,
        projectDeploymentId: string,
        projectName: string,
        workflowLabel: string
    ) => {
        if (currentWorkspaceId == null) {
            return;
        }

        // Idempotent on the server: re-clicking the same workflow row returns the existing task
        // row instead of creating a duplicate. The task comes back with `kind = WORKFLOW_CHAT`
        // so the server-side bridge routes turns through the webhook executor.
        //
        // The `title` is persisted only on FIRST creation — the workflow-chat row otherwise stays
        // "New Task" forever because the bridge bypasses the LLM-driven title generation that
        // standard tasks rely on. Format mirrors the sidebar: "{projectName} — {workflowLabel}".
        const result = await createMutation.mutateAsync({
            environment: currentEnvironmentId,
            projectDeploymentId,
            title: `${projectName} — ${workflowLabel}`,
            workflowExecutionId,
            workspaceId: String(currentWorkspaceId),
        });

        const task = result.createWorkflowChatAiHubTask;

        // Sync the active-task stores BEFORE navigating so the URL→store sync effect in
        // AiHub.tsx doesn't see a mismatch and try to switch back. Mirrors the pattern in
        // `handleNewTask` for standard tasks.
        aiHubStore.setState({
            messages: [],
            taskId: task.threadId,
        });

        aiHubTasksStore.getState().setCurrentTaskId(Number(task.id));

        // Invalidate the tasks list query (any environment + status combo for this workspace) so
        // the new row shows up in the sidebar immediately. Without this, the GraphQL mutation completes,
        // we navigate to the new thread, but the sidebar query — keyed on (workspace, env, status) — keeps
        // serving its 30s-stale cache and the row only appears after a manual refresh. Prefix-keyed match
        // so we hit ACTIVE + ARCHIVED list buckets without enumerating each.
        queryClient.invalidateQueries({
            queryKey: [...AiHubTasksKeys.all, 'list', currentWorkspaceId],
        });

        navigate(`/automation/ai-hub/tasks/${task.id}`);
    };

    if (isLoading) {
        // Skeleton mirrors the loaded layout below: each project group renders a small uppercase project-name
        // heading followed by a bordered, row-divided list of workflow buttons. Two groups × three rows is
        // representative of a typical workspace and gives the eye enough structure that the placeholder reads
        // as a list-of-projects rather than a generic loading bar.
        return (
            <div className="flex w-full flex-col gap-6">
                {Array.from({length: 2}).map((_, projectIndex) => (
                    <div className="flex flex-col gap-2" key={projectIndex}>
                        <Skeleton className="h-3 w-32" />

                        <div className="w-full divide-y divide-border/50 rounded-md border">
                            {Array.from({length: 3}).map((_, workflowIndex) => (
                                <div className="flex w-full items-center gap-3 px-4 py-4" key={workflowIndex}>
                                    <Skeleton className="size-5 shrink-0 rounded" />

                                    <Skeleton className="h-5 w-48 max-w-full" />
                                </div>
                            ))}
                        </div>
                    </div>
                ))}
            </div>
        );
    }

    const isEmpty = workflowsByProject.size === 0;

    return (
        <div className="flex w-full flex-col gap-4">
            {isEmpty ? (
                <div className="flex flex-col items-center justify-center gap-3 rounded-md border border-dashed py-16 text-center">
                    <WorkflowIcon className="size-10 text-muted-foreground" />

                    <div>
                        <p className="text-base font-semibold">No chat-enabled workflows yet</p>

                        <p className="mt-1 max-w-md text-sm text-muted-foreground">
                            Add a New Chat Request trigger to a workflow to surface it here. Once deployed, it shows up
                            in this list and on the AI Hub sidebar.
                        </p>
                    </div>
                </div>
            ) : (
                <div className="flex flex-col gap-6">
                    {/*
                     * Group by project — same shape as the previous sidebar version, but at full-page scale
                     * each project gets its own card with a heading and a divide-y rows list inside. Without
                     * the grouping, a workspace with many projects becomes an undifferentiated wall of rows.
                     */}

                    {Array.from(workflowsByProject.entries()).map(([projectId, {projectName, workflows}]) => (
                        <div className="flex flex-col gap-2" key={projectId}>
                            <h3 className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">
                                {projectName}
                            </h3>

                            <div className="w-full divide-y divide-border/50 rounded-md border">
                                {workflows.map((workflow) => {
                                    const hasArchivedChat = archivedWorkflowExecutionIds.has(
                                        workflow.workflowExecutionId
                                    );

                                    return (
                                        <button
                                            className="group flex w-full cursor-pointer items-center justify-between gap-4 px-4 py-4 text-left hover:bg-muted/40 disabled:opacity-50"
                                            disabled={createMutation.isPending}
                                            key={workflow.workflowExecutionId}
                                            onClick={() =>
                                                handleSelect(
                                                    workflow.workflowExecutionId,
                                                    workflow.projectDeploymentId,
                                                    projectName,
                                                    workflow.workflowLabel
                                                )
                                            }
                                            type="button"
                                        >
                                            <div className="flex flex-1 items-center gap-3">
                                                <WorkflowIcon className="size-5 shrink-0 text-muted-foreground" />

                                                <span className="truncate text-base font-semibold">
                                                    {workflow.workflowLabel}
                                                </span>
                                            </div>

                                            {hasArchivedChat && (
                                                <Tooltip>
                                                    <TooltipTrigger asChild>
                                                        <span className="inline-flex items-center gap-1 rounded bg-muted px-2 py-0.5 text-xs text-muted-foreground">
                                                            <ArchiveRestoreIcon className="size-3" />
                                                            archived
                                                        </span>
                                                    </TooltipTrigger>

                                                    <TooltipContent side="left">
                                                        You have an archived task with this workflow.
                                                    </TooltipContent>
                                                </Tooltip>
                                            )}
                                        </button>
                                    );
                                })}
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};

export default WorkflowChatsList;
