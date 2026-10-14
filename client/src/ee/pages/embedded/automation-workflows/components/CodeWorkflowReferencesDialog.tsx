import Badge from '@/components/Badge/Badge';
import {Dialog, DialogBody, DialogContent, DialogHeader, DialogMain} from '@/components/Dialog';
import LoadingDots from '@/components/LoadingDots';
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {useConnectedUserCodeWorkflowReferencesQuery} from '@/shared/middleware/graphql';

interface CodeWorkflowReferencesDialogProps {
    onClose: () => void;
    projectName: string;
    workflowTemplates: Array<{
        label?: string | null;
        workflowUuid: string;
    }>;
}

const CodeWorkflowReferencesDialog = ({onClose, projectName, workflowTemplates}: CodeWorkflowReferencesDialogProps) => {
    const automationWorkflowUuids = workflowTemplates.map((workflowTemplate) => workflowTemplate.workflowUuid);

    const {data, error, isLoading} = useConnectedUserCodeWorkflowReferencesQuery(
        {automationWorkflowUuids},
        {enabled: automationWorkflowUuids.length > 0}
    );

    const references = data?.connectedUserCodeWorkflowReferences || [];

    const workflowLabelsByUuid = new Map(
        workflowTemplates.map((workflowTemplate) => [workflowTemplate.workflowUuid, workflowTemplate.label])
    );

    return (
        <Dialog onOpenChange={onClose} open={true}>
            <DialogContent size="md">
                <DialogMain>
                    <DialogHeader
                        description="Connected users referencing the code workflows in this project. This list is read-only."
                        title={`References — ${projectName}`}
                    />

                    <DialogBody>
                        {isLoading && (
                            <div className="flex items-center justify-center py-8">
                                <LoadingDots />
                            </div>
                        )}

                        {!isLoading && Boolean(error) && (
                            <p className="text-sm text-destructive">Failed to load references.</p>
                        )}

                        {!isLoading && !error && references.length === 0 && (
                            <p className="text-sm text-muted-foreground">
                                No connected user has referenced this project yet.
                            </p>
                        )}

                        {!isLoading && !error && references.length > 0 && (
                            <Table>
                                <TableHeader>
                                    <TableRow>
                                        <TableHead>Workflow</TableHead>

                                        <TableHead>External User</TableHead>

                                        <TableHead>Environment</TableHead>

                                        <TableHead>Status</TableHead>
                                    </TableRow>
                                </TableHeader>

                                <TableBody>
                                    {references.map((reference) => (
                                        <TableRow
                                            key={`${reference.automationWorkflowUuid}-${reference.externalUserId}`}
                                        >
                                            <TableCell>
                                                {workflowLabelsByUuid.get(reference.automationWorkflowUuid) ||
                                                    reference.automationWorkflowUuid}
                                            </TableCell>

                                            <TableCell>{reference.externalUserId}</TableCell>

                                            <TableCell>{reference.environment}</TableCell>

                                            <TableCell>
                                                <div className="flex items-center gap-2">
                                                    <Badge
                                                        label={reference.enabled ? 'Enabled' : 'Disabled'}
                                                        styleType={
                                                            reference.enabled ? 'success-outline' : 'secondary-outline'
                                                        }
                                                    />

                                                    {reference.dangling && (
                                                        <Tooltip>
                                                            <TooltipTrigger>
                                                                <Badge label="Dangling" styleType="warning-outline" />
                                                            </TooltipTrigger>

                                                            <TooltipContent>
                                                                {reference.danglingReason ||
                                                                    'This reference is dangling.'}
                                                            </TooltipContent>
                                                        </Tooltip>
                                                    )}
                                                </div>
                                            </TableCell>
                                        </TableRow>
                                    ))}
                                </TableBody>
                            </Table>
                        )}
                    </DialogBody>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default CodeWorkflowReferencesDialog;
