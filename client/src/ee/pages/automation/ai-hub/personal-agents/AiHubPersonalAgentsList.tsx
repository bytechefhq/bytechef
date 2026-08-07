import Button from '@/components/Button/Button';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {Skeleton} from '@/components/ui/skeleton';
import {aiHubStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubStore';
import {AiHubTasksKeys} from '@/ee/pages/automation/ai-hub/tasks/hooks/useTasks';
import {aiHubTasksStore} from '@/ee/pages/automation/ai-hub/tasks/stores/useAiHubTasksStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useCreateAiHubPersonalAgentTaskMutation} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {formatDistanceToNow} from 'date-fns';
import {BotIcon, MoreVerticalIcon, PencilIcon, PlusIcon, Trash2Icon} from 'lucide-react';
import {useState} from 'react';
import {useNavigate} from 'react-router-dom';

import AiHubPersonalAgentDeleteDialog from './dialogs/AiHubPersonalAgentDeleteDialog';
import {AiHubPersonalAgentI, useAiHubPersonalAgentsQuery} from './hooks/useAiHubPersonalAgents';

interface AiHubPersonalAgentsListProps {
    onOpenCreateDialog: () => void;
}

/**
 * Full-page Personal Agents list, rendered inside the {@code /automation/ai-hub/personal-agents}
 * route via {@link AiHubPersonalAgents}. Rows are separated cards — each one carries its own rounded
 * border with a gap between, matching the Projects list rather than a single divide-y block.
 *
 * <p>Click on a row → idempotent create-or-resolve of a {@code kind = PERSONAL_AGENT} task
 * (server-side find-or-create), then navigate to it. Mirrors the WorkflowChatsList click flow so both
 * surface entry points produce identical client state.</p>
 *
 * <p>{@code onOpenCreateDialog} is supplied by the parent because the create-dialog open state lives at
 * the page level — the New Agent CTA renders either in the page header (when agents exist) or here in
 * the empty state (when none). Both triggers funnel through the same callback so the dialog instance is
 * shared.</p>
 */
const AiHubPersonalAgentsList = ({onOpenCreateDialog}: AiHubPersonalAgentsListProps) => {
    const [agentToDelete, setAgentToDelete] = useState<AiHubPersonalAgentI | null>(null);

    const navigate = useNavigate();
    const queryClient = useQueryClient();

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {data: agents, isLoading} = useAiHubPersonalAgentsQuery(currentWorkspaceId ?? 0, currentEnvironmentId);

    const createTaskMutation = useCreateAiHubPersonalAgentTaskMutation();

    const handleSelect = async (agent: AiHubPersonalAgentI) => {
        if (currentWorkspaceId == null) {
            return;
        }

        // Always-new on the server: each click produces a fresh task row with a fresh threadId. The
        // task comes back with `kind = PERSONAL_AGENT` so the routing agent applies the agent's
        // instructions overlay on every turn.
        const result = await createTaskMutation.mutateAsync({
            input: {
                aiHubPersonalAgentId: String(agent.id),
                environment: currentEnvironmentId,
                title: agent.title ?? agent.name,
                workspaceId: String(currentWorkspaceId),
            },
        });

        const task = result.createAiHubPersonalAgentTask;

        aiHubStore.setState({
            messages: [],
            taskId: task.threadId,
        });

        aiHubTasksStore.getState().setCurrentTaskId(Number(task.id));

        // Invalidate the tasks list query so the freshly created personal-agent task row
        // shows up in the sidebar without a manual refresh. Same prefix-key trick as WorkflowChatsList:
        // matches every (env, status) bucket for the workspace so we don't have to know which one the
        // sidebar is currently viewing.
        queryClient.invalidateQueries({
            queryKey: [...AiHubTasksKeys.all, 'list', currentWorkspaceId],
        });

        navigate(`/automation/ai-hub/tasks/${task.id}`);
    };

    const formatRelative = (value: string): string => {
        try {
            return formatDistanceToNow(new Date(value), {addSuffix: true});
        } catch {
            return '';
        }
    };

    if (isLoading) {
        return (
            <div className="flex flex-col gap-2">
                <Skeleton className="h-16 w-full" />

                <Skeleton className="h-16 w-full" />

                <Skeleton className="h-16 w-full" />
            </div>
        );
    }

    const hasAgents = agents != null && agents.length > 0;

    return (
        <div className="flex w-full flex-1 flex-col gap-4">
            {/*
             * `flex-1` so that when the list is empty the empty state can grow to fill the body and
             * vertically center its contents. With `gap-4` retained, when agents exist the list keeps the
             * same top spacing it had before — the gap only matters when there are siblings, so a single-
             * child layout (just the empty state, or just the list) is unaffected.
             *
             * Description text and the New Agent CTA used to live here in a flex row. The description was
             * folded into the page header (parent), and the create CTA lives in the page header (when
             * agents exist) or in the empty state below (when none) — see {@code AiHubPersonalAgents.tsx}.
             */}

            {!hasAgents ? (
                <div className="flex flex-1 flex-col items-center justify-center gap-3 text-center">
                    {/*
                     * Border removed at the user's request — the empty state now sits directly on the
                     * page background. `flex-1 + justify-center` vertically centers within the body's
                     * remaining height so the icon + headline + CTA hover near the optical center rather
                     * than hugging the top of the page.
                     */}

                    <BotIcon className="size-10 text-muted-foreground" />

                    <div>
                        <p className="text-base font-semibold">No personal agents yet</p>

                        <p className="mt-1 text-sm text-muted-foreground">
                            Create one to give yourself a dedicated assistant for a focused task.
                        </p>
                    </div>

                    <Button icon={<PlusIcon />} label="Create your first agent" onClick={onOpenCreateDialog} />
                </div>
            ) : (
                <div className="flex w-full flex-col gap-2">
                    {agents.map((agent) => {
                        const displayTitle = agent.title ?? agent.name;
                        const updated = formatRelative(agent.updatedAt);

                        return (
                            <div
                                className="group flex w-full cursor-pointer items-center justify-between gap-4 rounded border border-border/50 px-4 hover:bg-muted/40"
                                key={agent.id}
                                onClick={() => handleSelect(agent)}
                            >
                                <div className="flex flex-1 items-center gap-3 py-4">
                                    <BotIcon className="size-5 shrink-0 text-muted-foreground" />

                                    <div className="min-w-0 flex-1">
                                        <div className="flex items-center gap-2">
                                            <span className="truncate text-base font-semibold">{displayTitle}</span>

                                            {agent.title !== null && agent.title !== agent.name && (
                                                <span className="shrink-0 rounded bg-muted px-1.5 py-0.5 font-mono text-xs text-muted-foreground">
                                                    {agent.name}
                                                </span>
                                            )}
                                        </div>

                                        {agent.description && (
                                            <p className="mt-1 line-clamp-1 text-sm text-muted-foreground">
                                                {agent.description}
                                            </p>
                                        )}
                                    </div>
                                </div>

                                <div className="flex items-center gap-3">
                                    {updated && <span className="text-xs text-muted-foreground">{updated}</span>}

                                    {/*
                                     * Dropdown menu replaces the previous hover-revealed Edit/Delete buttons.
                                     * stopPropagation on the trigger so opening the menu doesn't fire the row
                                     * click. The menu items also stopPropagation in their onClick handlers so
                                     * the row click doesn't fire when the user picks an action.
                                     */}

                                    <DropdownMenu>
                                        <DropdownMenuTrigger asChild onClick={(event) => event.stopPropagation()}>
                                            <Button
                                                aria-label={`Actions for ${displayTitle}`}
                                                icon={<MoreVerticalIcon />}
                                                size="icon"
                                                variant="ghost"
                                            />
                                        </DropdownMenuTrigger>

                                        <DropdownMenuContent align="end">
                                            <DropdownMenuItem
                                                onClick={(event) => {
                                                    event.stopPropagation();

                                                    // Edit now navigates to the standalone form route instead
                                                    // of opening a dialog. Same form component handles
                                                    // create + edit; the route's :agentId param flips it
                                                    // into edit mode.
                                                    navigate(`/automation/ai-hub/personal-agents/${agent.id}/edit`);
                                                }}
                                            >
                                                <PencilIcon className="mr-2 size-4" />
                                                Edit
                                            </DropdownMenuItem>

                                            <DropdownMenuItem
                                                className="text-destructive focus:text-destructive"
                                                onClick={(event) => {
                                                    event.stopPropagation();

                                                    setAgentToDelete(agent);
                                                }}
                                            >
                                                <Trash2Icon className="mr-2 size-4" />
                                                Delete
                                            </DropdownMenuItem>
                                        </DropdownMenuContent>
                                    </DropdownMenu>
                                </div>
                            </div>
                        );
                    })}
                </div>
            )}

            <AiHubPersonalAgentDeleteDialog
                agent={agentToDelete}
                onOpenChange={(open) => !open && setAgentToDelete(null)}
                open={agentToDelete !== null}
            />
        </div>
    );
};

export default AiHubPersonalAgentsList;
