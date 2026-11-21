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

const WORKFLOW_CALL_CHANNEL_TYPE = 'workflowCall';

interface AgentChannelsCardProps {
    agentId: string;
    channels: AiAgentChannel[];
}

interface AgentChannelRowProps {
    channel: AiAgentChannel;
    definition?: AiAgentChannelDefinitionType;
    onDelete?: (id: string) => void;
    onEdit?: (channel: AiAgentChannel) => void;
    pinned: boolean;
}

const AgentChannelRow = ({channel, definition, onDelete, onEdit, pinned}: AgentChannelRowProps) => {
    const label = definition?.title || channel.channelType;

    const configurable = (definition?.connectionRequired ?? false) || (definition?.propertiesConfigurable ?? false);

    return (
        <li
            aria-label={label}
            className="flex items-center justify-between gap-2 rounded-md border border-border/50 p-3"
        >
            <div className="flex min-w-0 items-center gap-2 font-medium">
                <span className="flex size-5 flex-none items-center justify-center">
                    {definition?.icon ? (
                        <InlineSVG
                            className="size-5"
                            loader={<ComponentIcon className="size-5 text-content-neutral-primary" />}
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

            <div className="flex flex-none items-center gap-1">
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

    const classifiableChannels = isLoading ? [] : channels;

    const pinnedChannels = classifiableChannels.filter(
        (channel) =>
            definitionsByType[channel.channelType]?.pinned && channel.channelType !== WORKFLOW_CALL_CHANNEL_TYPE
    );

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
