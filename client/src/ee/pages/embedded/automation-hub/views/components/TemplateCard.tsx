import Button from '@/components/Button/Button';
import DeleteAlertDialog from '@/components/DeleteAlertDialog';
import {Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle} from '@/components/ui/card';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {Switch} from '@/components/ui/switch';
import {SetAutomationEnabledRequestI} from '@/ee/pages/embedded/automation-hub/mutations/automationHub.mutations';
import {removeAutomation} from '@/ee/pages/embedded/automation-hub/utils/removeAutomation';
import {
    AutomationWorkflowProjectWorkflowTemplate,
    ConnectedUserProjectWorkflow,
} from '@/ee/shared/middleware/embedded/public';
import {EllipsisVerticalIcon, ExternalLinkIcon, Trash2Icon} from 'lucide-react';
import {useState} from 'react';
import InlineSVG from 'react-inlinesvg';
import {useNavigate} from 'react-router-dom';

interface TemplateCardProps {
    activationDisabled?: boolean;
    automation?: ConnectedUserProjectWorkflow;
    onDeleteAutomation: (workflowUuid: string) => void;
    onDeprovisionReference: (workflowUuid: string) => void;
    onSetEnabled: (request: SetAutomationEnabledRequestI) => void;
    onUseTemplate: () => void;
    template: AutomationWorkflowProjectWorkflowTemplate;
}

/**
 * One catalog template, carrying its own usage state: an unused template offers "Use template",
 * while one the connected user has already activated offers an enable/disable toggle plus an
 * overflow menu holding Customize and Remove.
 *
 * Customize is offered for a COPY only — a REFERENCE points at a shared catalog workflow that the
 * user must never edit. Remove is offered for both, and branches through `removeAutomation`, the
 * same helper the "Your automations" row uses.
 *
 * `activationDisabled` blanks the activation affordance while the automations query is failing:
 * without knowing what the user already has, an activated template would render as unused and a
 * click would silently create a second copy.
 */
const TemplateCard = ({
    activationDisabled,
    automation,
    onDeleteAutomation,
    onDeprovisionReference,
    onSetEnabled,
    onUseTemplate,
    template,
}: TemplateCardProps) => {
    const [removeDialogOpen, setRemoveDialogOpen] = useState(false);

    const navigate = useNavigate();

    const handleRemove = () => {
        removeAutomation(automation!, {onDeleteAutomation, onDeprovisionReference});

        setRemoveDialogOpen(false);
    };

    return (
        <Card className="gap-3">
            <CardHeader>
                <CardTitle className="text-base">{template.label}</CardTitle>

                <CardDescription className="line-clamp-2">{template.description}</CardDescription>
            </CardHeader>

            {!!template.components?.length && (
                <CardContent className="flex items-center gap-2">
                    {template.components.map((component) => (
                        <div
                            className="flex size-7 shrink-0 items-center justify-center rounded-full border bg-background p-1"
                            key={`component-${component.name}`}
                            title={component.title || component.name}
                        >
                            {component.icon && <InlineSVG className="size-4 flex-none" src={component.icon} />}
                        </div>
                    ))}
                </CardContent>
            )}

            <CardFooter>
                {automation ? (
                    <div className="flex w-full items-center justify-between gap-2">
                        <div className="flex items-center gap-2">
                            <Switch
                                aria-label={`Enable ${template.label}`}
                                checked={!!automation.enabled}
                                onCheckedChange={(checked) =>
                                    onSetEnabled({enabled: checked, workflowUuid: automation.workflowUuid!})
                                }
                            />

                            <span className="text-sm text-muted-foreground">
                                {automation.enabled ? 'Enabled' : 'Disabled'}
                            </span>
                        </div>

                        <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                                <Button
                                    aria-label={`${template.label} actions`}
                                    icon={<EllipsisVerticalIcon />}
                                    size="icon"
                                    variant="ghost"
                                />
                            </DropdownMenuTrigger>

                            <DropdownMenuContent align="end">
                                {automation.kind === 'COPY' && (
                                    <DropdownMenuItem
                                        onClick={() => navigate(`/embedded/hub/builder/${automation.workflowUuid}`)}
                                    >
                                        <ExternalLinkIcon /> Customize
                                    </DropdownMenuItem>
                                )}

                                <DropdownMenuItem onClick={() => setRemoveDialogOpen(true)} variant="destructive">
                                    <Trash2Icon /> Remove
                                </DropdownMenuItem>
                            </DropdownMenuContent>
                        </DropdownMenu>

                        <DeleteAlertDialog
                            description={`This will remove "${template.label}" from your automations. This action cannot be undone.`}
                            onCancel={() => setRemoveDialogOpen(false)}
                            onDelete={handleRemove}
                            open={removeDialogOpen}
                        />
                    </div>
                ) : (
                    <Button
                        className="w-full"
                        disabled={activationDisabled}
                        label="Use template"
                        onClick={onUseTemplate}
                        variant="outline"
                    />
                )}
            </CardFooter>
        </Card>
    );
};

export default TemplateCard;
