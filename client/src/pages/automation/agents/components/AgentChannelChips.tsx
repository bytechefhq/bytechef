import AgentChannelIcon from '@/pages/automation/agents/components/AgentChannelIcon';
import {useAiAgentChannelDefinitions} from '@/pages/automation/agents/hooks/useAiAgentChannelDefinitions';
import {useMemo} from 'react';
import {twMerge} from 'tailwind-merge';

const OMITTED_CHANNEL_TYPES = ['schedule', 'workflowCall'];

interface AgentChannelChipsProps {
    channels?: ({channelType?: string | null} | null)[] | null;
    className?: string;
}

const AgentChannelChips = ({channels, className}: AgentChannelChipsProps) => {
    const {definitionsByType} = useAiAgentChannelDefinitions();

    const chips = useMemo(
        () =>
            (channels ?? [])
                .map((channel) => channel?.channelType)
                .filter(
                    (channelType): channelType is string =>
                        !!channelType && !OMITTED_CHANNEL_TYPES.includes(channelType)
                )
                .map((channelType) => ({
                    channelType,
                    icon: definitionsByType[channelType]?.icon,
                    label: definitionsByType[channelType]?.title || channelType,
                })),
        [channels, definitionsByType]
    );

    if (!chips.length) {
        return null;
    }

    return (
        <div className={twMerge('flex min-w-0 flex-wrap items-center gap-2', className)}>
            {chips.map(({channelType, icon, label}, index) => (
                <span
                    className="flex shrink-0 items-center gap-1.5 rounded-full border border-stroke-neutral-primary bg-surface-neutral-primary px-2 py-0.5 text-xs font-normal"
                    key={`${channelType}-${index}`}
                >
                    <AgentChannelIcon channelType={channelType} icon={icon} />

                    {label}
                </span>
            ))}
        </div>
    );
};

export default AgentChannelChips;
