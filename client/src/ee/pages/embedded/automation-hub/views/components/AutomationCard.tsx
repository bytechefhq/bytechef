import AlertDialog from '@/components/AlertDialog';
import {Badge} from '@/components/ui/badge';
import {Card, CardAction, CardDescription, CardFooter, CardHeader, CardTitle} from '@/components/ui/card';
import {SetAutomationEnabledRequestI} from '@/ee/pages/embedded/automation-hub/mutations/automationHub.mutations';
import {describeAttentionReason} from '@/ee/pages/embedded/automation-hub/utils/attentionReason';
import {canOpenInBuilder, useBuilderAccess} from '@/ee/pages/embedded/automation-hub/utils/builderAccess';
import {removeAutomation} from '@/ee/pages/embedded/automation-hub/utils/removeAutomation';
import {reportRemoveError, reportSetEnabledError} from '@/ee/pages/embedded/automation-hub/utils/reportAutomationError';
import AutomationAiAgentsSwitch from '@/ee/pages/embedded/automation-hub/views/components/AutomationAiAgentsSwitch';
import AutomationCardMenu from '@/ee/pages/embedded/automation-hub/views/components/AutomationCardMenu';
import AutomationInputsDialog from '@/ee/pages/embedded/automation-hub/views/components/AutomationInputsDialog';
import AutomationStatusButton from '@/ee/pages/embedded/automation-hub/views/components/AutomationStatusButton';
import AutomationVersion from '@/ee/pages/embedded/automation-hub/views/components/AutomationVersion';
import ComponentIconStack from '@/ee/pages/embedded/automation-hub/views/components/ComponentIconStack';
import {ConnectedUserProjectWorkflow} from '@/ee/shared/middleware/embedded/public';
import {useCopyToClipboard} from '@uidotdev/usehooks';
import {useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {toast} from 'sonner';
import {twMerge} from 'tailwind-merge';

interface AutomationCardProps {
    automation: ConnectedUserProjectWorkflow;
    onDeleteAutomation: (workflowUuid: string) => Promise<unknown>;
    onDeprovisionReference: (workflowUuid: string) => Promise<unknown>;
    onSetEnabled: (request: SetAutomationEnabledRequestI) => Promise<unknown>;
    onSetMcpEnabled: (request: SetAutomationEnabledRequestI) => Promise<unknown>;
}

const AutomationCard = ({
    automation,
    onDeleteAutomation,
    onDeprovisionReference,
    onSetEnabled,
    onSetMcpEnabled,
}: AutomationCardProps) => {
    const [inputsDialogOpen, setInputsDialogOpen] = useState(false);
    const [removeDialogOpen, setRemoveDialogOpen] = useState(false);
    const [removing, setRemoving] = useState(false);

    const builderAccess = useBuilderAccess();

    const [, copyToClipboard] = useCopyToClipboard();

    const navigate = useNavigate();

    const inputs = automation.inputs ?? [];
    const hasEditableInputs = inputs.some((input) => !input.provided);

    const label = automation.label || 'Untitled automation';

    const {staticWebhookUrl} = automation;

    const attentionDescription = describeAttentionReason(automation.attentionReason);
    const needsAttention = automation.dangling || !!attentionDescription;

    const handleRemove = async () => {
        setRemoving(true);

        try {
            await removeAutomation(automation, {onDeleteAutomation, onDeprovisionReference});

            setRemoveDialogOpen(false);
        } catch (error) {
            reportRemoveError(error, label);
        } finally {
            setRemoving(false);
        }
    };

    const handleEnabledChange = async (enabled: boolean) => {
        try {
            await onSetEnabled({enabled, workflowUuid: automation.workflowUuid!});
        } catch (error) {
            await reportSetEnabledError(error, {
                components: automation.components,
                label,
                onMissingInput: hasEditableInputs ? () => setInputsDialogOpen(true) : undefined,
            });
        }
    };

    const handleCopyWebhookUrl = async (webhookUrl: string) => {
        await copyToClipboard(webhookUrl);

        toast('Webhook URL copied to clipboard');
    };

    const handleMcpEnabledChange = async (enabled: boolean) => {
        try {
            await onSetMcpEnabled({enabled, workflowUuid: automation.workflowUuid!});
        } catch (error) {
            console.error(`Failed to change whether "${label}" is available to AI agents:`, error);
        }
    };

    return (
        <Card
            className={twMerge(
                'gap-4 border-transparent bg-(--hub-card) py-3 shadow-none',
                automation.enabled && !needsAttention && 'border-(--hub-active-border)'
            )}
        >
            <CardHeader className="px-3">
                <CardTitle className="flex min-h-9 min-w-0 items-center gap-2 self-center text-base">
                    <h3 className="truncate text-base font-semibold" title={label}>
                        {label}
                    </h3>

                    <AutomationVersion workflowVersion={automation.workflowVersion} />

                    {needsAttention && <Badge variant="destructive">Needs attention</Badge>}
                </CardTitle>

                {attentionDescription && <p className="text-sm text-muted-foreground">{attentionDescription}</p>}

                {automation.description && (
                    <CardDescription className="col-span-full line-clamp-2" title={automation.description}>
                        {automation.description}
                    </CardDescription>
                )}

                <CardAction className="row-span-1 self-center">
                    <AutomationCardMenu
                        label={label}
                        onCopyWebhookUrl={staticWebhookUrl ? () => handleCopyWebhookUrl(staticWebhookUrl) : undefined}
                        onCustomize={
                            automation.kind === 'COPY' && canOpenInBuilder(automation, builderAccess)
                                ? () => navigate(`/embedded/hub/builder/${automation.workflowUuid}`)
                                : undefined
                        }
                        onEditInputs={hasEditableInputs ? () => setInputsDialogOpen(true) : undefined}
                        onRemove={() => setRemoveDialogOpen(true)}
                    />
                </CardAction>
            </CardHeader>

            <CardFooter className="mt-auto justify-end px-3">
                <ComponentIconStack components={automation.components} />

                {automation.mcpEnabled != null && (
                    <AutomationAiAgentsSwitch automation={automation} onChange={handleMcpEnabledChange} />
                )}

                {!automation.toolOnly && (
                    <AutomationStatusButton
                        automation={automation}
                        label={label}
                        onEnabledChange={handleEnabledChange}
                    />
                )}

                {inputsDialogOpen && (
                    <AutomationInputsDialog
                        inputValues={(automation.inputValues ?? {}) as Record<string, unknown>}
                        inputs={inputs}
                        label={label}
                        onClose={() => setInputsDialogOpen(false)}
                        workflowUuid={automation.workflowUuid!}
                    />
                )}

                <AlertDialog
                    confirmLabel="Remove"
                    description={`This will remove "${label}" from your automations. This action cannot be undone.`}
                    isPending={removing}
                    onCancel={() => setRemoveDialogOpen(false)}
                    onConfirm={handleRemove}
                    open={removeDialogOpen}
                    title="Remove automation?"
                />
            </CardFooter>
        </Card>
    );
};

export default AutomationCard;
