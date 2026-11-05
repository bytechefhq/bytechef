import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import AgentChannelChips from '@/pages/automation/agents/components/AgentChannelChips';
import DeleteAgentAlertDialog from '@/pages/automation/agents/components/DeleteAgentAlertDialog';
import exportAgent from '@/pages/automation/agents/utils/agentImportExport';
import {describeCadence, fromCadenceParameters} from '@/pages/automation/agents/utils/agentScheduleCron';
import getAgentPath from '@/pages/automation/agents/utils/getAgentPath';
import invalidateAgentQueries from '@/pages/automation/agents/utils/invalidateAgentQueries';
import isScheduledAgent from '@/pages/automation/agents/utils/isScheduledAgent';
import {AiAgent, useDeleteAiAgentMutation} from '@/shared/middleware/graphql';
import {usePublishProjectMutation} from '@/shared/mutations/automation/projects.mutations';
import {ProjectKeys} from '@/shared/queries/automation/projects.queries';
import isInteractiveElementClick from '@/shared/util/interactive-element-utils';
import {useQueryClient} from '@tanstack/react-query';
import {
    BotIcon,
    CalendarClockIcon,
    DownloadIcon,
    EllipsisVerticalIcon,
    PencilIcon,
    SendIcon,
    Trash2Icon,
} from 'lucide-react';
import {useMemo, useState} from 'react';
import {Link, useNavigate} from 'react-router-dom';
import {toast} from 'sonner';

interface AgentListItemProps {
    agent: AiAgent;
}

const AgentListItem = ({agent}: AgentListItemProps) => {
    const [showDeleteConfirmDialog, setShowDeleteConfirmDialog] = useState(false);

    const navigate = useNavigate();
    const queryClient = useQueryClient();

    const deleteAgentMutation = useDeleteAiAgentMutation({
        onError: (error) => {
            toast.error(error instanceof Error ? error.message : 'Failed to delete the agent.');
        },
        onSuccess: () => queryClient.invalidateQueries({queryKey: ['aiAgents']}),
    });

    const publishProjectMutation = usePublishProjectMutation({
        onSuccess: () => {
            invalidateAgentQueries(queryClient);

            queryClient.invalidateQueries({queryKey: ProjectKeys.project(+agent.projectId)});

            toast.success('The project has been published.');
        },
    });

    const scheduleSummary = useMemo(
        () =>
            (agent.channels ?? [])
                .filter((channel) => channel?.channelType === 'schedule')
                .map((channel) => {
                    const parameters = (channel?.parameters ?? {}) as Record<string, unknown>;

                    return describeCadence(fromCadenceParameters(parameters)) || String(parameters.expression ?? '');
                })
                .filter(Boolean)
                .join(', '),
        [agent.channels]
    );

    const handleConfirmDelete = () => {
        setShowDeleteConfirmDialog(false);

        deleteAgentMutation.mutate({id: agent.id});
    };

    const handleExportClick = async () => {
        try {
            await exportAgent(agent.id, agent.title);
        } catch (error) {
            toast.error(error instanceof Error ? error.message : 'Failed to export the agent.');
        }
    };

    const handleClick = (event: React.MouseEvent) => {
        if (isInteractiveElementClick(event.target)) {
            return;
        }

        navigate(getAgentPath(agent));
    };

    return (
        <li
            className="flex cursor-pointer items-center justify-between rounded-md px-3 py-1 hover:bg-surface-neutral-primary-hover"
            onClick={handleClick}
        >
            <Link
                aria-label={`Link to agent ${agent.title}`}
                className="flex min-w-0 flex-1 items-center gap-2"
                to={getAgentPath(agent)}
            >
                <div className="flex w-80 min-w-0 shrink-0 items-center gap-2 pr-1 text-sm font-semibold">
                    <BotIcon className="size-4 shrink-0 text-content-neutral-secondary" />

                    <Tooltip>
                        <TooltipTrigger className="line-clamp-1 min-w-0 flex-1 truncate text-start">
                            {agent.title}
                        </TooltipTrigger>

                        <TooltipContent align="start" className="max-w-md break-all">
                            {agent.title}
                        </TooltipContent>
                    </Tooltip>
                </div>

                <AgentChannelChips channels={agent.channels} className="ml-6 hidden sm:flex" />
            </Link>

            <div className="flex justify-end gap-x-6">
                {isScheduledAgent(agent) && (
                    <span className="mr-6 hidden min-w-0 items-center gap-1 truncate text-sm font-normal text-content-neutral-secondary sm:flex">
                        <CalendarClockIcon className="size-4 shrink-0" />

                        <span className="truncate">{scheduleSummary || 'Scheduled'}</span>
                    </span>
                )}

                <Tooltip>
                    <TooltipTrigger className="hidden items-center text-sm text-content-neutral-secondary xl:block">
                        <span className="text-xs">
                            {agent.lastModifiedDate &&
                                `Modified at ${new Date(agent.lastModifiedDate).toLocaleDateString()} ${new Date(agent.lastModifiedDate).toLocaleTimeString()}`}
                        </span>
                    </TooltipTrigger>

                    <TooltipContent>Last Modified Date</TooltipContent>
                </Tooltip>

                <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                        <Button icon={<EllipsisVerticalIcon />} size="icon" variant="ghost" />
                    </DropdownMenuTrigger>

                    <DropdownMenuContent align="end">
                        <DropdownMenuItem
                            disabled={publishProjectMutation.isPending}
                            onClick={() =>
                                publishProjectMutation.mutate({id: +agent.projectId, publishProjectRequest: {}})
                            }
                        >
                            <SendIcon /> Publish Project
                        </DropdownMenuItem>

                        <DropdownMenuItem onClick={() => navigate(getAgentPath(agent))}>
                            <PencilIcon /> Edit
                        </DropdownMenuItem>

                        <DropdownMenuItem onClick={handleExportClick}>
                            <DownloadIcon /> Export
                        </DropdownMenuItem>

                        <DropdownMenuSeparator />

                        <DropdownMenuItem
                            disabled={deleteAgentMutation.isPending}
                            onClick={() => setShowDeleteConfirmDialog(true)}
                            variant="destructive"
                        >
                            <Trash2Icon /> Delete
                        </DropdownMenuItem>
                    </DropdownMenuContent>
                </DropdownMenu>
            </div>

            {showDeleteConfirmDialog && (
                <DeleteAgentAlertDialog
                    agentTitle={agent.title}
                    onClose={() => setShowDeleteConfirmDialog(false)}
                    onDelete={handleConfirmDelete}
                />
            )}
        </li>
    );
};

export default AgentListItem;
