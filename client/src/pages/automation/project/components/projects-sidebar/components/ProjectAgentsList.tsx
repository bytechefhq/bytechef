import Badge from '@/components/Badge/Badge';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import AgentsLeftSidebarDropdownMenu from '@/pages/automation/agents/components/AgentsLeftSidebarDropdownMenu';
import useAgents from '@/pages/automation/agents/hooks/useAgents';
import {useAiAgentChannelDefinitions} from '@/pages/automation/agents/hooks/useAiAgentChannelDefinitions';
import getAgentPath from '@/pages/automation/agents/utils/getAgentPath';
import {ComponentIcon} from 'lucide-react';
import {useMemo} from 'react';
import InlineSVG from 'react-inlinesvg';
import {Link} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';

const HIDDEN_CHANNEL_TYPES = ['workflowCall'];

interface ProjectAgentsListAgentI {
    channels?: ({channelType?: string | null; id?: string | null} | null)[] | null;
    id: string;
    lastModifiedDate?: string | null;
    projectId: string;
    title: string;
}

interface ProjectAgentsListProps {
    calculateTimeDifference: (date?: string) => string;
    currentAgentId?: string;
    emptyMessage: string;
    projectId: number;
}

interface ProjectAgentsListItemProps {
    agent: ProjectAgentsListAgentI;
    calculateTimeDifference: (date?: string) => string;
    current: boolean;
}

const ProjectAgentsListItem = ({agent, calculateTimeDifference, current}: ProjectAgentsListItemProps) => {
    const {definitionsByType} = useAiAgentChannelDefinitions();

    const visibleChannels = useMemo(
        () =>
            (agent.channels ?? []).filter(
                (channel): channel is {channelType: string; id?: string | null} =>
                    !!channel?.channelType && !HIDDEN_CHANNEL_TYPES.includes(channel.channelType)
            ),
        [agent.channels]
    );

    const [firstChannel, ...remainingChannels] = visibleChannels;

    return (
        <li
            className={twMerge(
                'group flex w-full items-center rounded-md border border-transparent pr-1 hover:bg-background',
                current && 'border-stroke-brand-primary bg-background'
            )}
        >
            <Link
                className="flex min-w-0 flex-1 cursor-pointer flex-col gap-3 overflow-hidden py-3 pl-3"
                to={getAgentPath(agent)}
            >
                {firstChannel && (
                    <div className="flex shrink-0 items-center gap-1">
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <div className="flex shrink-0 items-center justify-center rounded-full border border-stroke-neutral-primary bg-surface-neutral-primary p-1">
                                    {definitionsByType[firstChannel.channelType]?.icon ? (
                                        <InlineSVG
                                            className="size-5"
                                            loader={<ComponentIcon className="size-5 flex-none" />}
                                            src={definitionsByType[firstChannel.channelType]!.icon!}
                                            title={null}
                                        />
                                    ) : (
                                        <ComponentIcon className="size-3 flex-none text-content-neutral-primary" />
                                    )}
                                </div>
                            </TooltipTrigger>

                            <TooltipContent>
                                {definitionsByType[firstChannel.channelType]?.title || firstChannel.channelType}
                            </TooltipContent>
                        </Tooltip>

                        <Badge
                            label={definitionsByType[firstChannel.channelType]?.title || firstChannel.channelType}
                            styleType="outline-outline"
                            weight="semibold"
                        />

                        {remainingChannels.length > 0 && (
                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <div className="flex size-7 items-center justify-center self-center rounded-full border border-stroke-neutral-secondary bg-background p-1">
                                        <span className="self-center text-xs font-medium text-content-neutral-secondary">
                                            +{remainingChannels.length}
                                        </span>
                                    </div>
                                </TooltipTrigger>

                                <TooltipContent className="mt-1 text-pretty" side="bottom">
                                    {remainingChannels.map((channel, index) => (
                                        <div className="py-0.5" key={channel.id ?? `${channel.channelType}-${index}`}>
                                            {definitionsByType[channel.channelType]?.title || channel.channelType}
                                        </div>
                                    ))}
                                </TooltipContent>
                            </Tooltip>
                        )}
                    </div>
                )}

                <div className="flex min-w-0 flex-col gap-1 text-start">
                    <span className="truncate text-sm font-medium">{agent.title}</span>

                    <div className="flex gap-1 text-xs text-content-neutral-secondary">
                        <span>Edited</span>

                        {calculateTimeDifference(agent.lastModifiedDate ?? undefined)}
                    </div>
                </div>
            </Link>

            <AgentsLeftSidebarDropdownMenu agent={agent} current={current} />
        </li>
    );
};

const ProjectAgentsList = ({
    calculateTimeDifference,
    currentAgentId,
    emptyMessage,
    projectId,
}: ProjectAgentsListProps) => {
    const {agents} = useAgents();

    const projectAgents = useMemo(
        () => agents.filter((agent) => projectId === 0 || +agent.projectId === projectId),
        [agents, projectId]
    );

    if (projectAgents.length === 0) {
        return <span className="w-full py-2 text-sm text-muted-foreground">{emptyMessage}</span>;
    }

    return (
        <ul className="flex flex-col gap-2">
            {projectAgents.map((agent) => (
                <ProjectAgentsListItem
                    agent={agent}
                    calculateTimeDifference={calculateTimeDifference}
                    current={agent.id === currentAgentId}
                    key={agent.id}
                />
            ))}
        </ul>
    );
};

export default ProjectAgentsList;
