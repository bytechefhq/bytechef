import Button from '@/components/Button/Button';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {Label} from '@/components/ui/label';
import AgentSection from '@/pages/automation/agents/components/detail/AgentSection';
import {
    type AiAgentChannelDefinitionType,
    useAiAgentChannelDefinitions,
} from '@/pages/automation/agents/hooks/useAiAgentChannelDefinitions';
import invalidateAgentQueries from '@/pages/automation/agents/utils/invalidateAgentQueries';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import ComponentConfigDialog from '@/shared/components/component-config/ComponentConfigDialog';
import {
    AiAgentChannel,
    useAddAiAgentChannelMutation,
    useDeleteAiAgentChannelMutation,
    useUpdateAiAgentChannelMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {ComponentIcon, LockIcon, PlusIcon, SettingsIcon, Trash2Icon} from 'lucide-react';
import {useState} from 'react';
import InlineSVG from 'react-inlinesvg';
import {toast} from 'sonner';

// The one reserved channel key this card names. Its row is hidden rather than removed: the generator always
// emits a workflowCall channel and it is how a workflow invokes the agent, but it carries no configuration --
// its input schema is fixed -- so a permanently inert row was only taking up space.
const WORKFLOW_CALL_CHANNEL_TYPE = 'workflowCall';

interface AgentChannelsCardProps {
    agentId: string;
    channels: AiAgentChannel[];
}

interface AgentChannelRowProps {
    channel: AiAgentChannel;
    /** The channel's registry entry, or undefined for a stored channel type no deployed component declares. */
    definition?: AiAgentChannelDefinitionType;
    onDelete?: (id: string) => void;
    onEdit?: (channel: AiAgentChannel) => void;
    pinned: boolean;
}

const AgentChannelRow = ({channel, definition, onDelete, onEdit, pinned}: AgentChannelRowProps) => {
    const label = definition?.title || channel.channelType;

    // Configuring a channel opens its trigger's Connection + Properties pair, so the affordance is offered where
    // there is either half to fill in. This used to read connectionRequired alone, which was a proxy for the
    // wrong thing: it happens to match every channel that needs a connection, and silently hides the button on a
    // channel configured purely through its trigger's properties -- which chat, with its mode property and no
    // connection, already was. workflowCall stays buttonless on the honest predicate too: no connection, and its
    // one property is the inputSchema its own channel declaration pins.
    const configurable = (definition?.connectionRequired ?? false) || (definition?.propertiesConfigurable ?? false);

    return (
        // One line, label left / controls right — the same shape ApprovalChannelRow uses. The two channel
        // lists sit on the same page, so a stacked "Connection" fieldset here read as a different kind of
        // row rather than the same row with different content.
        <li
            aria-label={label}
            className="flex items-center justify-between gap-2 rounded-md border border-border/50 p-3"
        >
            <div className="flex min-w-0 items-center gap-2 font-medium">
                {/* Fixed-width icon slot so every label starts at the same x, whether the row is a pinned
                    platform channel (lock), a messaging channel (component logo) or neither. */}

                <span className="flex size-5 flex-none items-center justify-center">
                    {definition?.icon ? (
                        <InlineSVG
                            className="size-5"
                            loader={<ComponentIcon className="size-5 text-gray-700" />}
                            src={definition.icon}
                        />
                    ) : (
                        pinned && <LockIcon aria-hidden className="size-3.5 text-content-neutral-tertiary" />
                    )}
                </span>

                <span className="truncate">{label}</span>

                {channel.channelType === WORKFLOW_CALL_CHANNEL_TYPE && (
                    <span className="truncate text-xs font-normal text-muted-foreground">
                        Predefined input schema: {'{message, conversationId?, attachments?}'}
                    </span>
                )}
            </div>

            <div className="flex flex-none items-center gap-2">
                {configurable && onEdit && (
                    <Button
                        aria-label={`Edit ${label} channel`}
                        icon={<SettingsIcon />}
                        onClick={() => onEdit(channel)}
                        size="iconSm"
                        variant="ghost"
                    />
                )}

                {!pinned && onDelete && (
                    <Button
                        aria-label={`Delete ${label} trigger`}
                        icon={<Trash2Icon />}
                        onClick={() => onDelete(channel.id)}
                        size="iconSm"
                        variant="destructiveGhost"
                    />
                )}
            </div>
        </li>
    );
};

const AgentChannelsCard = ({agentId, channels}: AgentChannelsCardProps) => {
    const [showAddDialog, setShowAddDialog] = useState(false);
    const [addChannelType, setAddChannelType] = useState('');
    const [editingChannel, setEditingChannel] = useState<AiAgentChannel | null>(null);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {addableDefinitions, definitionsByType, isLoading} = useAiAgentChannelDefinitions();

    const queryClient = useQueryClient();

    const onError = (error: unknown) => {
        toast.error(error instanceof Error ? error.message : 'Failed to save the trigger.');
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

    // No channel can be classified until the registry answers, and guessing is worse than waiting: an
    // unclassified chat row would fall through to the list below and offer the Delete button a pinned channel
    // must never have. One frame of an empty list beats one frame of a destructive affordance.
    const classifiableChannels = isLoading ? [] : channels;

    // pinned marks the two channels every agent gets by construction and can never remove. workflowCall is one
    // of them but its row stays hidden, for the reason WORKFLOW_CALL_CHANNEL_TYPE records.
    const pinnedChannels = classifiableChannels.filter(
        (channel) =>
            definitionsByType[channel.channelType]?.pinned && channel.channelType !== WORKFLOW_CALL_CHANNEL_TYPE
    );

    // A schedule row is a real agent_channel row, but it belongs to the Schedules section (AgentScheduleCard)
    // rather than to a list of channels — it is the one entry the registry marks as not a channel at all.
    const otherChannels = classifiableChannels.filter(
        (channel) =>
            !definitionsByType[channel.channelType]?.pinned && !definitionsByType[channel.channelType]?.schedule
    );

    const closeAddDialog = () => {
        setShowAddDialog(false);
        setAddChannelType('');
    };

    const handleAddChannel = ({
        connectionId,
        parameters,
    }: {
        connectionId: string | null;
        parameters: Record<string, unknown>;
    }) => {
        addAgentChannelMutation.mutate({
            input: {agentId, channelType: addChannelType, connectionId, parameters},
        });

        closeAddDialog();
    };

    // updateAiAgentChannel treats connectionId: null as "leave unchanged" (AiAgentFacadeImpl), so clearing a
    // wired connection has to replace the row -- an update can only ever set one. The replacement carries the
    // parameters just submitted, not the row's previous ones: the dialog edits connection and properties
    // together, so a clear-plus-edit would otherwise silently drop the property changes.
    const handleEditChannel = ({
        connectionId,
        parameters,
    }: {
        connectionId: string | null;
        parameters: Record<string, unknown>;
    }) => {
        const channel = editingChannel;

        setEditingChannel(null);

        if (!channel) {
            return;
        }

        if (connectionId === null && channel.connectionId != null) {
            handleClearChannelConnection(channel, parameters);

            return;
        }

        updateAgentChannelMutation.mutate({input: {connectionId, id: channel.id, parameters}});
    };

    const handleDeleteChannel = (id: string) => {
        deleteAgentChannelMutation.mutate({id});
    };

    const handleClearChannelConnection = (channel: AiAgentChannel, parameters?: Record<string, unknown>) => {
        deleteAgentChannelMutation.mutate(
            {id: channel.id},
            {
                onSuccess: () =>
                    addAgentChannelMutation.mutate({
                        input: {
                            agentId,
                            channelType: channel.channelType,
                            connectionId: null,
                            parameters: parameters ?? channel.parameters ?? {},
                        },
                    }),
            }
        );
    };

    return (
        <AgentSection
            action={
                <Button
                    icon={<PlusIcon />}
                    label="Add channel"
                    onClick={() => setShowAddDialog(true)}
                    size="sm"
                    variant="outline"
                />
            }
            title="Channels"
        >
            <ul className="space-y-2">
                {pinnedChannels.map((channel) => (
                    <AgentChannelRow
                        channel={channel}
                        definition={definitionsByType[channel.channelType]}
                        key={channel.id}
                        onEdit={setEditingChannel}
                        pinned
                    />
                ))}

                {otherChannels.map((channel) => (
                    <AgentChannelRow
                        channel={channel}
                        definition={definitionsByType[channel.channelType]}
                        key={channel.id}
                        onDelete={handleDeleteChannel}
                        onEdit={setEditingChannel}
                        pinned={false}
                    />
                ))}
            </ul>

            {/* Adding and editing share the one dialog: the channel type is picked in its picker slot, and the
                trigger's own connection and properties are set in the same pass. Adding first and configuring
                after would leave a channel that cannot register its webhook until a second visit — infobip's
                required monitored number is exactly that case. */}

            {showAddDialog && (
                <ComponentConfigDialog
                    description="Choose a channel, then set the connection and properties it uses."
                    onClose={closeAddDialog}
                    onSubmit={handleAddChannel}
                    open
                    pending={addAgentChannelMutation.isPending}
                    picker={
                        <div className="space-y-1">
                            <Label htmlFor="agent-channel-type">Channel</Label>

                            <Select onValueChange={setAddChannelType} value={addChannelType}>
                                <SelectTrigger id="agent-channel-type">
                                    <SelectValue placeholder="Choose a channel…" />
                                </SelectTrigger>

                                <SelectContent>
                                    {addableDefinitions.map((definition) => (
                                        <SelectItem key={definition.channelType} value={definition.channelType}>
                                            {definition.title}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                        </div>
                    }
                    submitLabel="Add"
                    target={
                        addChannelType
                            ? {
                                  clusterElementName: definitionsByType[addChannelType]?.triggerName ?? '',
                                  componentName: definitionsByType[addChannelType]?.componentName ?? '',
                                  componentVersion: definitionsByType[addChannelType]?.componentVersion ?? 1,
                                  kind: 'TRIGGER',
                                  title: definitionsByType[addChannelType]?.title || addChannelType,
                              }
                            : null
                    }
                    title="Add Channel"
                    workspaceId={Number(currentWorkspaceId)}
                />
            )}

            {/* Editing reuses the tool config dialog rather than the add dialog: a channel needs the same
                Connection + Properties pair a tool does, and its properties are the trigger's own (infobip's
                required monitored number, slack's event filters). The add dialog stays connection-only because
                the channel type is not chosen yet, so there is no trigger whose properties could be rendered. */}

            {editingChannel && (
                <ComponentConfigDialog
                    initialValues={{
                        connectionId: editingChannel.connectionId ?? null,
                        parameters: (editingChannel.parameters ?? {}) as Record<string, unknown>,
                    }}
                    onClose={() => setEditingChannel(null)}
                    onSubmit={handleEditChannel}
                    open
                    pending={updateAgentChannelMutation.isPending}
                    target={{
                        clusterElementName: definitionsByType[editingChannel.channelType]?.triggerName ?? '',
                        componentName: definitionsByType[editingChannel.channelType]?.componentName ?? '',
                        componentVersion: definitionsByType[editingChannel.channelType]?.componentVersion ?? 1,
                        kind: 'TRIGGER',
                        title: definitionsByType[editingChannel.channelType]?.title || editingChannel.channelType,
                    }}
                    title={`Configure ${
                        definitionsByType[editingChannel.channelType]?.title || editingChannel.channelType
                    } channel`}
                    workspaceId={Number(currentWorkspaceId)}
                />
            )}
        </AgentSection>
    );
};

export default AgentChannelsCard;
