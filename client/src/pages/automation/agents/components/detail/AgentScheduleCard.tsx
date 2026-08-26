import Button from '@/components/Button/Button';
import AgentScheduleDialog, {
    AgentSchedulePropertiesI,
} from '@/pages/automation/agents/components/detail/AgentScheduleDialog';
import AgentSection from '@/pages/automation/agents/components/detail/AgentSection';
import {
    CADENCE_PARAMETER_KEYS,
    describeCadence,
    fromCadenceParameters,
} from '@/pages/automation/agents/utils/agentScheduleCron';
import invalidateAgentQueries from '@/pages/automation/agents/utils/invalidateAgentQueries';
import {
    AiAgentChannel,
    useAddAiAgentChannelMutation,
    useDeleteAiAgentChannelMutation,
    useUpdateAiAgentChannelMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {PlusIcon, SettingsIcon, Trash2Icon} from 'lucide-react';
import {useState} from 'react';
import {toast} from 'sonner';

const CHANNEL_TYPE = 'schedule';

interface AgentScheduleCardProps {
    agentId: string;
    channels: AiAgentChannel[];
}

/**
 * Schedules get their own section rather than a row in Channels: a schedule is not an inbound conversation
 * the agent replies to, it is a self-started run carrying its own prompt, and its configuration (cron,
 * timezone, prompt) is larger than any messaging channel's.
 *
 * <p>
 * Underneath it is still an ordinary {@code agent_channel} row of type {@code schedule}, which the generator
 * turns into an additional {@code schedule/v1/cron} trigger on the agent's workflow. No component declares
 * that entry — a schedule is the one thing in the list that is not a channel, so {@code AgentChannelResolver}
 * synthesizes it from the {@code schedule} component's own trigger. Only the presentation is separate.
 * </p>
 */
const AgentScheduleCard = ({agentId, channels}: AgentScheduleCardProps) => {
    const [showAddDialog, setShowAddDialog] = useState(false);
    const [editingChannel, setEditingChannel] = useState<AiAgentChannel | null>(null);

    const queryClient = useQueryClient();

    const onError = (error: unknown) => {
        toast.error(error instanceof Error ? error.message : 'Failed to save the schedule.');
    };

    const addAgentChannelMutation = useAddAiAgentChannelMutation({
        onError,
        onSuccess: () => invalidateAgentQueries(queryClient),
    });

    const updateAgentChannelMutation = useUpdateAiAgentChannelMutation({
        onError,
        onSuccess: () => invalidateAgentQueries(queryClient),
    });

    const deleteAgentChannelMutation = useDeleteAiAgentChannelMutation({
        onError,
        onSuccess: () => invalidateAgentQueries(queryClient),
    });

    const scheduleChannels = channels.filter((channel) => channel.channelType === CHANNEL_TYPE);

    const closeDialog = () => {
        setShowAddDialog(false);
        setEditingChannel(null);
    };

    const handleSubmit = (values: AgentSchedulePropertiesI) => {
        if (editingChannel) {
            updateAgentChannelMutation.mutate({input: {id: editingChannel.id, parameters: values}});
        } else {
            addAgentChannelMutation.mutate({
                input: {agentId, channelType: CHANNEL_TYPE, parameters: values},
            });
        }

        closeDialog();
    };

    const toScheduleProperties = (channel: AiAgentChannel): AgentSchedulePropertiesI => {
        const parameters = (channel.parameters ?? {}) as Record<string, unknown>;

        const properties: AgentSchedulePropertiesI = {
            expression: String(parameters.expression ?? ''),
            name: String(parameters.name ?? ''),
            prompt: String(parameters.prompt ?? ''),
            timezone: String(parameters.timezone || 'UTC'),
        };

        // The cadence keys are carried through verbatim so the dialog can restore the picker; the dialog,
        // not this card, is what interprets them.
        for (const key of CADENCE_PARAMETER_KEYS) {
            if (parameters[key] != null) {
                properties[key] = String(parameters[key]);
            }
        }

        return properties;
    };

    /**
     * What the row shows instead of the stored cron: "Daily at 09:00" reads, "0 9 * * ?" does not. A row written by
     * hand or before the picker existed carries no cadence fields, and describeCadence falls back to the expression
     * itself — the only thing certainly true about when such a row runs. The timezone is appended only when it is not
     * UTC, since a bare time is ambiguous the moment it is not.
     */
    const describeSchedule = (channel: AiAgentChannel): string => {
        const parameters = (channel.parameters ?? {}) as Record<string, unknown>;

        const summary = describeCadence(fromCadenceParameters(parameters));

        if (!summary) {
            return 'No schedule set';
        }

        const timezone = String(parameters.timezone || 'UTC');

        return timezone === 'UTC' ? summary : `${summary} (${timezone})`;
    };

    return (
        <AgentSection
            action={
                <Button
                    icon={<PlusIcon />}
                    label="Add schedule"
                    onClick={() => setShowAddDialog(true)}
                    size="sm"
                    variant="outline"
                />
            }
            title="Schedules"
        >
            {scheduleChannels.length === 0 ? (
                <p className="text-sm text-muted-foreground">
                    No schedules yet. A schedule runs the agent on a cron expression with its own prompt.
                </p>
            ) : (
                <ul className="space-y-2">
                    {scheduleChannels.map((channel) => {
                        const schedule = toScheduleProperties(channel);

                        return (
                            <li
                                aria-label={schedule.name || 'Schedule'}
                                className="flex items-center justify-between gap-2 rounded-md border border-border/50 px-3 py-2"
                                key={channel.id}
                            >
                                <div className="flex min-w-0 items-baseline gap-2">
                                    <span className="truncate font-medium">{schedule.name || 'Schedule'}</span>

                                    <span className="truncate text-xs text-muted-foreground">
                                        {describeSchedule(channel)}
                                    </span>
                                </div>

                                <div className="flex flex-none items-center gap-2">
                                    <Button
                                        aria-label={`Edit ${schedule.name || 'schedule'}`}
                                        icon={<SettingsIcon />}
                                        onClick={() => setEditingChannel(channel)}
                                        size="iconSm"
                                        variant="ghost"
                                    />

                                    <Button
                                        aria-label={`Delete ${schedule.name || 'schedule'}`}
                                        icon={<Trash2Icon />}
                                        onClick={() => deleteAgentChannelMutation.mutate({id: channel.id})}
                                        size="iconSm"
                                        variant="destructiveGhost"
                                    />
                                </div>
                            </li>
                        );
                    })}
                </ul>
            )}

            {(showAddDialog || editingChannel) && (
                <AgentScheduleDialog
                    key={editingChannel?.id ?? 'add'}
                    onClose={closeDialog}
                    onSubmit={handleSubmit}
                    open
                    pending={addAgentChannelMutation.isPending || updateAgentChannelMutation.isPending}
                    schedule={editingChannel ? toScheduleProperties(editingChannel) : null}
                />
            )}
        </AgentSection>
    );
};

export default AgentScheduleCard;
