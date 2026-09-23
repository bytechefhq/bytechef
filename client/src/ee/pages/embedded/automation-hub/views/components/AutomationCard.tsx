import AlertDialog from '@/components/AlertDialog';
import {Badge} from '@/components/ui/badge';
import {Card, CardAction, CardContent, CardDescription, CardFooter, CardHeader, CardTitle} from '@/components/ui/card';
import {SetAutomationEnabledRequestI} from '@/ee/pages/embedded/automation-hub/mutations/automationHub.mutations';
import {describeAttentionReason} from '@/ee/pages/embedded/automation-hub/utils/attentionReason';
import {removeAutomation} from '@/ee/pages/embedded/automation-hub/utils/removeAutomation';
import {reportRemoveError, reportSetEnabledError} from '@/ee/pages/embedded/automation-hub/utils/reportAutomationError';
import AutomationCardMenu from '@/ee/pages/embedded/automation-hub/views/components/AutomationCardMenu';
import AutomationInputsDialog from '@/ee/pages/embedded/automation-hub/views/components/AutomationInputsDialog';
import AutomationStatusButton from '@/ee/pages/embedded/automation-hub/views/components/AutomationStatusButton';
import AutomationVersion from '@/ee/pages/embedded/automation-hub/views/components/AutomationVersion';
import {ConnectedUserProjectWorkflow} from '@/ee/shared/middleware/embedded/public';
import {useState} from 'react';
import InlineSVG from 'react-inlinesvg';
import {useNavigate} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';

interface AutomationCardProps {
    automation: ConnectedUserProjectWorkflow;
    onDeleteAutomation: (workflowUuid: string) => Promise<unknown>;
    onDeprovisionReference: (workflowUuid: string) => Promise<unknown>;
    onSetEnabled: (request: SetAutomationEnabledRequestI) => Promise<unknown>;
}

const AutomationCard = ({
    automation,
    onDeleteAutomation,
    onDeprovisionReference,
    onSetEnabled,
}: AutomationCardProps) => {
    const [inputsDialogOpen, setInputsDialogOpen] = useState(false);
    const [removeDialogOpen, setRemoveDialogOpen] = useState(false);
    const [removing, setRemoving] = useState(false);

    const inputs = automation.inputs ?? [];

    const navigate = useNavigate();

    const label = automation.label || 'Untitled automation';

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
                onMissingInput: inputs.length > 0 ? () => setInputsDialogOpen(true) : undefined,
            });
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
                <CardTitle className="flex min-w-0 items-center gap-2 self-center text-base">
                    <h3 className="truncate text-base font-semibold" title={label}>
                        {label}
                    </h3>

                    <AutomationVersion workflowVersion={automation.workflowVersion} />

                    {needsAttention && <Badge variant="destructive">Needs attention</Badge>}
                </CardTitle>

                {attentionDescription && <p className="text-sm text-muted-foreground">{attentionDescription}</p>}

                {automation.description && (
                    <CardDescription className="line-clamp-2" title={automation.description}>
                        {automation.description}
                    </CardDescription>
                )}

                <CardAction className="row-span-1 self-center">
                    <AutomationCardMenu
                        label={label}
                        onCustomize={
                            automation.kind === 'COPY' && !automation.dangling
                                ? () => navigate(`/embedded/hub/builder/${automation.workflowUuid}`)
                                : undefined
                        }
                        onEditInputs={inputs.length > 0 ? () => setInputsDialogOpen(true) : undefined}
                        onRemove={() => setRemoveDialogOpen(true)}
                    />
                </CardAction>
            </CardHeader>

            {!!automation.components?.length && (
                <CardContent className="flex items-center -space-x-2 px-3">
                    {automation.components.map((component) => (
                        <div
                            className="flex size-9 shrink-0 items-center justify-center rounded-full border bg-background p-1.5"
                            key={`component-${component.name}`}
                            title={component.title || component.name}
                        >
                            {component.icon && <InlineSVG className="size-5 flex-none" src={component.icon} />}
                        </div>
                    ))}
                </CardContent>
            )}

            <CardFooter className="mt-auto justify-end px-3">
                <AutomationStatusButton automation={automation} label={label} onEnabledChange={handleEnabledChange} />

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
