import {Input} from '@/components/Input/Input';
import Switch from '@/components/Switch/Switch';
import AgentSettingRow from '@/pages/automation/agents/components/detail/AgentSettingRow';
import {useAiAgentChannelDefinitions} from '@/pages/automation/agents/hooks/useAiAgentChannelDefinitions';
import invalidateAgentQueries from '@/pages/automation/agents/utils/invalidateAgentQueries';
import {
    AiAgentChannel,
    AiAgentElement,
    useAddAiAgentElementMutation,
    useDeleteAiAgentElementMutation,
    useUpdateAiAgentElementMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {toast} from 'sonner';

interface AgentApprovalSettingsProps {
    agentId: string;
    /** The agent's channels — approvals are delivered over these, so they are named rather than configured. */
    channels: AiAgentChannel[];
    elements: AiAgentElement[];
}

/**
 * The agent's two independent HITL switches, rendered inside the Settings tab's toggle list rather than as a
 * section of their own.
 *
 * <ul>
 * <li><b>Agent may request approval</b> — the {@code APPROVAL_TOOL} row, an LLM-invocable "ask a human" tool.</li>
 * <li><b>Tool approval</b> — the {@code APPROVAL_GATE} row, the master switch for the per-tool "Requires
 * approval" flags set in the Tools section. With it off, {@code AiAgentWorkflowGenerator.buildToolSequence}
 * emits every tool ungated; the flags stay on their rows, so switching it back on restores the gating.</li>
 * </ul>
 *
 * <p>
 * Both default OFF, which needs no default to be declared anywhere: absence of the row IS off. Approval
 * channels are shared by the two mechanisms, so they show whenever either is on.
 * </p>
 */
const AgentApprovalSettings = ({agentId, channels, elements}: AgentApprovalSettingsProps) => {
    const {definitionsByType} = useAiAgentChannelDefinitions();

    // A channel can carry an approval when its own declaration names an approval-delivery element, which is what
    // approvalCapable reports. schedule has nobody to ask and workflowCall's caller is another workflow, so
    // neither declares one -- and neither is named here, without this component having to know that.
    const deliveryChannelLabels = channels
        .map((channel) => definitionsByType[channel.channelType])
        .filter((definition) => definition?.approvalCapable)
        .map((definition) => definition.title);

    const gateElement = elements.find((element) => element.kind === 'APPROVAL_GATE');
    const approvalToolElement = elements.find((element) => element.kind === 'APPROVAL_TOOL');

    const queryClient = useQueryClient();

    const onError = (error: unknown) => toast.error(error instanceof Error ? error.message : 'Failed to save.');

    const addAiAgentElementMutation = useAddAiAgentElementMutation({
        onError,
        onSuccess: () => invalidateAgentQueries(queryClient),
    });

    const updateAiAgentElementMutation = useUpdateAiAgentElementMutation({
        onError,
        onSuccess: () => invalidateAgentQueries(queryClient),
    });

    const deleteAiAgentElementMutation = useDeleteAiAgentElementMutation({
        onError,
        onSuccess: () => invalidateAgentQueries(queryClient),
    });

    const isBusy =
        addAiAgentElementMutation.isPending ||
        updateAiAgentElementMutation.isPending ||
        deleteAiAgentElementMutation.isPending;

    // Only ever updates: the input is unreachable until the gate toggle has created the row.
    const handleGateExpiryBlur = (value: string) => {
        if (!gateElement) {
            return;
        }

        const trimmed = value.trim();

        if (!trimmed) {
            return;
        }

        const days = Number(trimmed);

        if (!Number.isFinite(days) || days <= 0) {
            return;
        }

        updateAiAgentElementMutation.mutate({
            input: {
                id: gateElement.id,
                parameters: {approvalExpiresIn: Math.trunc(days), approvalExpiresInUnit: 'DAYS'},
            },
        });
    };

    const handleToolApprovalToggle = (checked: boolean) => {
        if (checked && !gateElement) {
            addAiAgentElementMutation.mutate({input: {agentId, kind: 'APPROVAL_GATE'}});

            return;
        }

        if (!checked && gateElement) {
            deleteAiAgentElementMutation.mutate({id: gateElement.id});
        }
    };

    const handleRequestApprovalToggle = (checked: boolean) => {
        if (checked && !approvalToolElement) {
            addAiAgentElementMutation.mutate({input: {agentId, kind: 'APPROVAL_TOOL'}});

            return;
        }

        if (!checked && approvalToolElement) {
            deleteAiAgentElementMutation.mutate({id: approvalToolElement.id});
        }
    };

    return (
        <>
            <AgentSettingRow
                control={
                    <Switch
                        aria-label="Agent may request approval"
                        checked={!!approvalToolElement}
                        disabled={isBusy}
                        id="agent-approval-request"
                        onCheckedChange={handleRequestApprovalToggle}
                    />
                }
                controlId="agent-approval-request"
                description="Adds a tool the agent's LLM can call mid-turn to ask a human a question."
                label="Agent may request approval"
            />

            <AgentSettingRow
                control={
                    <Switch
                        aria-label="Tool approval"
                        checked={!!gateElement}
                        disabled={isBusy}
                        id="agent-approval-gate"
                        onCheckedChange={handleToolApprovalToggle}
                    />
                }
                controlId="agent-approval-gate"
                description="Pauses tools marked “Requires approval” until a human approves them."
                label="Tool approval"
            />

            {gateElement && (
                <div className="ml-1 space-y-4 border-l border-stroke-neutral-secondary pl-4">
                    <AgentSettingRow
                        control={
                            <Input
                                className="w-56"
                                defaultValue={
                                    gateElement.parameters?.approvalExpiresIn != null
                                        ? String(gateElement.parameters.approvalExpiresIn)
                                        : ''
                                }
                                disabled={isBusy}
                                id="agent-approval-gate-expiry"
                                min={1}
                                onBlur={(event) => handleGateExpiryBlur(event.target.value)}
                                placeholder="4"
                                type="number"
                            />
                        }
                        controlId="agent-approval-gate-expiry"
                        label="Approval expires after (days)"
                    />
                </div>
            )}

            {/* Approvals are delivered over the agent's OWN channels — there is nothing to configure here, and
                nothing that could drift from the channels the agent actually has. Both mechanisms deliver the
                same way, so this belongs to neither toggle alone. */}

            {(approvalToolElement || gateElement) && (
                <p className="text-sm text-muted-foreground">
                    {deliveryChannelLabels.length > 0
                        ? `Approvals are delivered to the agent's channels: ${deliveryChannelLabels.join(', ')}.`
                        : 'This agent has no channel that can carry an approval, so approvals fall back to the chat channel — invisible on a webhook or schedule-only agent.'}
                </p>
            )}
        </>
    );
};

export default AgentApprovalSettings;
