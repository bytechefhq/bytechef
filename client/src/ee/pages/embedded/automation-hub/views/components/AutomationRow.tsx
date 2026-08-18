import Button from '@/components/Button/Button';
import DeleteAlertDialog from '@/components/DeleteAlertDialog';
import {Badge} from '@/components/ui/badge';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {Switch} from '@/components/ui/switch';
import {TableCell, TableRow} from '@/components/ui/table';
import {SetAutomationEnabledRequestI} from '@/ee/pages/embedded/automation-hub/mutations/automationHub.mutations';
import {removeAutomation} from '@/ee/pages/embedded/automation-hub/utils/removeAutomation';
import {ConnectedUserProjectWorkflow} from '@/ee/shared/middleware/embedded/public';
import {EllipsisVerticalIcon, ExternalLinkIcon, Trash2Icon} from 'lucide-react';
import {useState} from 'react';
import InlineSVG from 'react-inlinesvg';
import {useNavigate} from 'react-router-dom';

interface AutomationRowProps {
    automation: ConnectedUserProjectWorkflow;
    onDeleteAutomation: (workflowUuid: string) => void;
    onDeprovisionReference: (workflowUuid: string) => void;
    onSetEnabled: (request: SetAutomationEnabledRequestI) => void;
}

/**
 * One row of the "Your automations" section — everything the connected user has that no published
 * template accounts for, plus any extra automation beyond the first matching a given template.
 * The `kind` field drives the row's divergence between a COPY (the connected user's own editable
 * workflow) and a REFERENCE (a pointer at a shared catalog workflow): only a COPY offers "Open in
 * builder". Confirming Delete branches through `removeAutomation`, the same helper the activated
 * template card's Remove uses.
 *
 * A `dangling` row keeps its "Needs attention" badge and its Delete, but its enable toggle is
 * DISABLED: the reference points at a catalog workflow a redeploy withdrew, nothing ever clears the
 * flag, and enabling it fails server-side with nothing on screen to explain the failure. Delete is
 * the only action (spec §3 and the spec's Risks and notes).
 */
const AutomationRow = ({automation, onDeleteAutomation, onDeprovisionReference, onSetEnabled}: AutomationRowProps) => {
    const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);

    const navigate = useNavigate();

    const statusLabel = automation.dangling ? 'Needs attention' : automation.enabled ? 'Enabled' : 'Disabled';
    const statusVariant = automation.dangling ? 'destructive' : automation.enabled ? 'default' : 'secondary';

    const handleDelete = () => {
        removeAutomation(automation, {onDeleteAutomation, onDeprovisionReference});

        setDeleteDialogOpen(false);
    };

    return (
        <TableRow>
            <TableCell className="font-medium">{automation.label}</TableCell>

            <TableCell>
                <div className="flex items-center gap-2">
                    {automation.components?.map((component) => (
                        <div
                            className="flex size-6 shrink-0 items-center justify-center rounded-full border bg-background p-1"
                            key={`component-${component.name}`}
                            title={component.title || component.name}
                        >
                            {component.icon && <InlineSVG className="size-3.5 flex-none" src={component.icon} />}
                        </div>
                    ))}
                </div>
            </TableCell>

            <TableCell>
                <Badge variant={statusVariant}>{statusLabel}</Badge>
            </TableCell>

            <TableCell>
                <Switch
                    checked={!!automation.enabled}
                    disabled={automation.dangling}
                    onCheckedChange={(checked) =>
                        onSetEnabled({enabled: checked, workflowUuid: automation.workflowUuid!})
                    }
                />
            </TableCell>

            <TableCell>
                <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                        <Button
                            aria-label="Automation actions"
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
                                <ExternalLinkIcon /> Open in builder
                            </DropdownMenuItem>
                        )}

                        <DropdownMenuItem onClick={() => setDeleteDialogOpen(true)} variant="destructive">
                            <Trash2Icon /> Delete
                        </DropdownMenuItem>
                    </DropdownMenuContent>
                </DropdownMenu>

                <DeleteAlertDialog
                    description={`This will remove "${automation.label}" from your automations. This action cannot be undone.`}
                    onCancel={() => setDeleteDialogOpen(false)}
                    onDelete={handleDelete}
                    open={deleteDialogOpen}
                />
            </TableCell>
        </TableRow>
    );
};

export default AutomationRow;
