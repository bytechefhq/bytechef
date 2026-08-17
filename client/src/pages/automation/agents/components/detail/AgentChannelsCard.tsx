import Button from '@/components/Button/Button';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {Label} from '@/components/ui/label';
import AgentSection from '@/pages/automation/agents/components/detail/AgentSection';
import invalidateAgentQueries from '@/pages/automation/agents/utils/invalidateAgentQueries';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import ComponentConfigDialog from '@/shared/components/component-config/ComponentConfigDialog';
import {
    AiAgentChannel,
    useAddAiAgentChannelMutation,
    useDeleteAiAgentChannelMutation,
    useUpdateAiAgentChannelMutation,
} from '@/shared/middleware/graphql';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {useQueryClient} from '@tanstack/react-query';
import {ComponentIcon, LockIcon, PlusIcon, SettingsIcon, Trash2Icon} from 'lucide-react';
import {useState} from 'react';
import InlineSVG from 'react-inlinesvg';
import {toast} from 'sonner';

// The registry mirrors ChannelDefinitions on the server (automation-ai-agent-service): chat/workflowCall are
// created automatically for every agent and can never be removed, the rest are opt-in triggers.
const PINNED_CHANNEL_TYPES = ['chat', 'workflowCall'];
// 'whatsapp' is intentionally excluded from the add-trigger menu: the whatsapp component's declared trigger
// output shape (ChannelDefinitions.whatsapp()'s source comment) nests entry/changes/value/messages as single
// objects, but WhatsApp's real Cloud API webhook sends those as arrays — the generated envelope mapping would
// read the wrong path against a real payload. The channel type stays registered server-side (and this card
// still renders an existing whatsapp channel row if one is already configured) so nothing breaks for an agent
// that already has one; only the "add a new whatsapp trigger" affordance is hidden until the component's
// output schema is fixed. See docs/agents/agents.md's "known-disabled channel" note.
// 'schedule' is deliberately absent too: it is still an agent_channel row of type 'schedule', but it is
// presented in its own Schedules section (AgentScheduleCard) rather than as a messaging channel.
// infobip's trigger declares a REQUIRED `number` property (the number to monitor), which the add dialog does
// not collect -- it only picks a type and a connection. The channel is added first and configured after, through
// the row's own Configure dialog, the same two-step a tool row uses; until that number is set the channel will
// fail to register its webhook.
const ADDABLE_CHANNEL_TYPES = ['slack', 'telegram', 'rocketchat', 'twilio', 'infobip'];

const CHANNEL_TYPE_LABELS: Record<string, string> = {
    chat: 'Chat',
    // Both are WhatsApp channels — twilio/v1/newWhatsappMessage and infobip/v1/newWhatsappMessage. Each
    // component also exposes SMS and inbound-call triggers, so a bare "Twilio" would not say which one this is.
    infobip: 'Infobip (WhatsApp)',
    rocketchat: 'Rocket.Chat',
    schedule: 'Schedule',
    slack: 'Slack',
    telegram: 'Telegram',
    twilio: 'Twilio (WhatsApp)',
    whatsapp: 'WhatsApp',
    workflowCall: 'Workflow Call',
};

// Connection component names, as registered via ComponentDsl.component(...). Most match the channel type
// literally; WhatsApp is the one exception (the component name capitalizes the "A").
const MESSAGING_CHANNEL_COMPONENT_NAMES: Record<string, string> = {
    infobip: 'infobip',
    rocketchat: 'rocketchat',
    slack: 'slack',
    telegram: 'telegram',
    twilio: 'twilio',
    whatsapp: 'whatsApp',
};

// The trigger each channel type installs, matching ChannelDefinitions' triggerType on the server (the segment
// after /v1/). Editing a channel opens that trigger's own property tree, so a wrong name here yields an empty
// Properties tab rather than an error — keep it in step with the server registry.
const CHANNEL_TRIGGER_NAMES: Record<string, string> = {
    infobip: 'newWhatsappMessage',
    rocketchat: 'newMessage',
    slack: 'anyEvent',
    telegram: 'newMessage',
    twilio: 'newWhatsappMessage',
    whatsapp: 'messageReceived',
};

// Deliberately separate from the map above, which also drives the per-row connections query and the
// connection control it gates. chat and workflow ARE real components (ChatComponentHandler /
// WorkflowComponentHandler, each shipping an icon asset), so their rows should carry a logo -- but neither
// takes a connection, and listing them above would give the pinned rows a connection picker they cannot use.
const CHANNEL_ICON_COMPONENT_NAMES: Record<string, string> = {
    ...MESSAGING_CHANNEL_COMPONENT_NAMES,
    chat: 'chat',
    workflowCall: 'workflow',
};

interface AgentChannelsCardProps {
    agentId: string;
    channels: AiAgentChannel[];
}

interface AgentChannelRowProps {
    channel: AiAgentChannel;
    onDelete?: (id: string) => void;
    onEdit?: (channel: AiAgentChannel) => void;
    pinned: boolean;
}

const AgentChannelRow = ({channel, onDelete, onEdit, pinned}: AgentChannelRowProps) => {
    const componentName = MESSAGING_CHANNEL_COMPONENT_NAMES[channel.channelType];
    const iconComponentName = CHANNEL_ICON_COMPONENT_NAMES[channel.channelType];

    const {data: componentDefinition} = useGetComponentDefinitionQuery(
        {componentName: iconComponentName, componentVersion: 1},
        !!iconComponentName
    );

    return (
        // One line, label left / controls right — the same shape ApprovalChannelRow uses. The two channel
        // lists sit on the same page, so a stacked "Connection" fieldset here read as a different kind of
        // row rather than the same row with different content.
        <li
            aria-label={CHANNEL_TYPE_LABELS[channel.channelType] || channel.channelType}
            className="flex items-center justify-between gap-2 rounded-md border border-border/50 p-3"
        >
            <div className="flex min-w-0 items-center gap-2 font-medium">
                {/* Fixed-width icon slot so every label starts at the same x, whether the row is a pinned
                    platform channel (lock), a messaging channel (component logo) or neither. */}

                <span className="flex size-5 flex-none items-center justify-center">
                    {componentDefinition?.icon ? (
                        <InlineSVG
                            className="size-5"
                            loader={<ComponentIcon className="size-5 text-gray-700" />}
                            src={componentDefinition.icon}
                        />
                    ) : (
                        pinned && <LockIcon aria-hidden className="size-3.5 text-content-neutral-tertiary" />
                    )}
                </span>

                <span className="truncate">{CHANNEL_TYPE_LABELS[channel.channelType] || channel.channelType}</span>

                {channel.channelType === 'workflowCall' && (
                    <span className="truncate text-xs font-normal text-muted-foreground">
                        Predefined input schema: {'{message, conversationId?, attachments?}'}
                    </span>
                )}
            </div>

            <div className="flex flex-none items-center gap-2">
                {componentName && onEdit && (
                    <Button
                        aria-label={`Edit ${CHANNEL_TYPE_LABELS[channel.channelType] || channel.channelType} channel`}
                        icon={<SettingsIcon />}
                        onClick={() => onEdit(channel)}
                        size="iconSm"
                        variant="ghost"
                    />
                )}

                {!pinned && onDelete && (
                    <Button
                        aria-label={`Delete ${CHANNEL_TYPE_LABELS[channel.channelType] || channel.channelType} trigger`}
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

    // workflowCall is hidden rather than removed: the generator always emits it and it is how a workflow
    // invokes the agent, but it carries no configuration -- its input schema is fixed -- so a permanently
    // inert row was only taking up space. It is still a real channel on the agent; only the row is gone.
    const pinnedChannels = channels.filter(
        (channel) => PINNED_CHANNEL_TYPES.includes(channel.channelType) && channel.channelType !== 'workflowCall'
    );
    const otherChannels = channels.filter(
        (channel) => !PINNED_CHANNEL_TYPES.includes(channel.channelType) && channel.channelType !== 'schedule'
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
                    <AgentChannelRow channel={channel} key={channel.id} onEdit={setEditingChannel} pinned />
                ))}

                {otherChannels.map((channel) => (
                    <AgentChannelRow
                        channel={channel}
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
                                    {ADDABLE_CHANNEL_TYPES.map((type) => (
                                        <SelectItem key={type} value={type}>
                                            {CHANNEL_TYPE_LABELS[type] ?? type}
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
                                  clusterElementName: CHANNEL_TRIGGER_NAMES[addChannelType] ?? '',
                                  componentName: MESSAGING_CHANNEL_COMPONENT_NAMES[addChannelType] ?? '',
                                  componentVersion: 1,
                                  kind: 'TRIGGER',
                                  title: CHANNEL_TYPE_LABELS[addChannelType] || addChannelType,
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
                        clusterElementName: CHANNEL_TRIGGER_NAMES[editingChannel.channelType] ?? '',
                        componentName: MESSAGING_CHANNEL_COMPONENT_NAMES[editingChannel.channelType] ?? '',
                        componentVersion: 1,
                        kind: 'TRIGGER',
                        title: CHANNEL_TYPE_LABELS[editingChannel.channelType] || editingChannel.channelType,
                    }}
                    title={`Configure ${
                        CHANNEL_TYPE_LABELS[editingChannel.channelType] || editingChannel.channelType
                    } channel`}
                    workspaceId={Number(currentWorkspaceId)}
                />
            )}
        </AgentSection>
    );
};

export default AgentChannelsCard;
