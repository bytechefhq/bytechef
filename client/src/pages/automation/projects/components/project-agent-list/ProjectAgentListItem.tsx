import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import AgentChannelChips from '@/pages/automation/agents/components/AgentChannelChips';
import AgentsLeftSidebarDropdownMenu from '@/pages/automation/agents/components/AgentsLeftSidebarDropdownMenu';
import {describeCadence, fromCadenceParameters} from '@/pages/automation/agents/utils/agentScheduleCron';
import getAgentPath from '@/pages/automation/agents/utils/getAgentPath';
import {BotIcon, CalendarClockIcon} from 'lucide-react';
import {useMemo} from 'react';
import {Link} from 'react-router-dom';

interface ProjectAgentListItemAgentI {
    channels?: ({channelType?: string | null; parameters?: unknown} | null)[] | null;
    description?: string | null;
    id: string;
    lastModifiedDate?: string | null;
    projectId: string;
    title: string;
}

interface ProjectAgentListItemProps {
    agent: ProjectAgentListItemAgentI;
}

const ProjectAgentListItem = ({agent}: ProjectAgentListItemProps) => {
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

    return (
        <li className="flex items-center justify-between rounded-md px-3 py-1 hover:bg-surface-neutral-primary-hover">
            <Link
                aria-label={`Link to agent ${agent.title}`}
                className="flex min-w-0 flex-1 items-center gap-2"
                data-testid={`${agent.id}-link`}
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
                {scheduleSummary && (
                    <span className="mr-6 hidden min-w-0 items-center gap-1 truncate text-sm font-normal text-content-neutral-secondary sm:flex">
                        <CalendarClockIcon className="size-4 shrink-0" />

                        <span className="truncate">{scheduleSummary}</span>
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

                <AgentsLeftSidebarDropdownMenu agent={agent} alwaysVisibleTrigger current={false} />
            </div>
        </li>
    );
};

export default ProjectAgentListItem;
