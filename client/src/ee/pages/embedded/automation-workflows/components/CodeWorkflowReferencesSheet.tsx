import Badge from '@/components/Badge/Badge';
import LoadingDots from '@/components/LoadingDots';
import {Sheet, SheetCloseButton, SheetContent, SheetDescription, SheetTitle} from '@/components/ui/sheet';
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {useConnectedUserCodeWorkflowReferencesQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';

interface CodeWorkflowReferencesSheetProps {
    onClose: () => void;
    projectName: string;
    workflowTemplates: Array<{
        label?: string | null;
        workflowUuid: string;
    }>;
}

const CodeWorkflowReferencesSheet = ({onClose, projectName, workflowTemplates}: CodeWorkflowReferencesSheetProps) => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const automationWorkflowUuids = workflowTemplates.map((workflowTemplate) => workflowTemplate.workflowUuid);

    const {data, error, isLoading} = useConnectedUserCodeWorkflowReferencesQuery(
        {automationWorkflowUuids, environmentId: String(currentEnvironmentId)},
        {enabled: automationWorkflowUuids.length > 0 && currentEnvironmentId !== undefined}
    );

    const references = data?.connectedUserCodeWorkflowReferences || [];

    const workflowLabelsByUuid = new Map(
        workflowTemplates.map((workflowTemplate) => [workflowTemplate.workflowUuid, workflowTemplate.label])
    );

    return (
        <Sheet onOpenChange={(open) => !open && onClose()} open={true}>
            <SheetContent
                className="top-3 right-4 bottom-4 flex h-auto flex-col gap-0 rounded-md bg-surface-neutral-secondary p-0 sm:max-w-2xl"
                onFocusOutside={(event) => event.preventDefault()}
                onPointerDownOutside={(event) => event.preventDefault()}
            >
                <header className="flex w-full shrink-0 items-start justify-between gap-x-3 rounded-t-md border-b border-b-border/50 bg-surface-neutral-primary p-3">
                    <div className="flex flex-col gap-1">
                        <SheetTitle className="text-lg font-semibold">{`References — ${projectName}`}</SheetTitle>

                        <SheetDescription>
                            Connected users referencing the code workflows in this project. This list is read-only.
                        </SheetDescription>
                    </div>

                    <SheetCloseButton />
                </header>

                <div className="m-3 flex min-h-0 flex-1 flex-col overflow-y-auto rounded-lg bg-surface-neutral-primary p-3">
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

                                    <TableHead>Status</TableHead>
                                </TableRow>
                            </TableHeader>

                            <TableBody>
                                {references.map((reference) => (
                                    <TableRow key={`${reference.automationWorkflowUuid}-${reference.externalUserId}`}>
                                        <TableCell>
                                            {workflowLabelsByUuid.get(reference.automationWorkflowUuid) ||
                                                reference.automationWorkflowUuid}
                                        </TableCell>

                                        <TableCell>{reference.externalUserId}</TableCell>

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
                                                            {reference.danglingReason || 'This reference is dangling.'}
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
                </div>
            </SheetContent>
        </Sheet>
    );
};

export default CodeWorkflowReferencesSheet;
