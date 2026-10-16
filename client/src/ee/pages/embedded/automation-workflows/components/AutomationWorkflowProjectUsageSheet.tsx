import Badge from '@/components/Badge/Badge';
import LoadingDots from '@/components/LoadingDots';
import {Sheet, SheetCloseButton, SheetContent, SheetDescription, SheetTitle} from '@/components/ui/sheet';
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {
    useConnectedUserCatalogWorkflowCopiesQuery,
    useConnectedUserCodeWorkflowReferencesQuery,
} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useMemo} from 'react';

interface AutomationWorkflowProjectUsageSheetProps {
    codeWorkflowProject: boolean;
    onClose: () => void;
    projectName: string;
    workflowTemplates: Array<{
        label?: string | null;
        workflowUuid: string;
    }>;
}

const UsageTableHeader = () => (
    <TableHeader>
        <TableRow>
            <TableHead>Workflow</TableHead>

            <TableHead>External User</TableHead>

            <TableHead>Status</TableHead>
        </TableRow>
    </TableHeader>
);

const EnabledBadge = ({enabled}: {enabled: boolean}) => (
    <Badge label={enabled ? 'Enabled' : 'Disabled'} styleType={enabled ? 'success-outline' : 'secondary-outline'} />
);

const AutomationWorkflowProjectUsageSheet = ({
    codeWorkflowProject,
    onClose,
    projectName,
    workflowTemplates,
}: AutomationWorkflowProjectUsageSheetProps) => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const automationWorkflowUuids = workflowTemplates.map((workflowTemplate) => workflowTemplate.workflowUuid);

    const queryEnabled = automationWorkflowUuids.length > 0 && currentEnvironmentId !== undefined;

    const {
        data: referencesData,
        error: referencesError,
        isLoading: referencesLoading,
    } = useConnectedUserCodeWorkflowReferencesQuery(
        {automationWorkflowUuids, environmentId: String(currentEnvironmentId)},
        {enabled: queryEnabled}
    );

    const {
        data: copiesData,
        error: copiesError,
        isLoading: copiesLoading,
    } = useConnectedUserCatalogWorkflowCopiesQuery(
        {automationWorkflowUuids, environmentId: String(currentEnvironmentId)},
        {enabled: queryEnabled && !codeWorkflowProject}
    );

    const references = referencesData?.connectedUserCodeWorkflowReferences || [];
    const copies = copiesData?.connectedUserCatalogWorkflowCopies || [];

    const workflowLabelsByUuid = useMemo(
        () =>
            new Map(
                workflowTemplates.map((workflowTemplate) => [workflowTemplate.workflowUuid, workflowTemplate.label])
            ),
        [workflowTemplates]
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
                        <SheetTitle className="text-lg font-semibold">{`Usage — ${projectName}`}</SheetTitle>

                        <SheetDescription>
                            Connected users referencing or copying the workflows in this project. This list is
                            read-only.
                        </SheetDescription>
                    </div>

                    <SheetCloseButton />
                </header>

                <div className="m-3 flex min-h-0 flex-1 flex-col overflow-y-auto rounded-lg bg-surface-neutral-primary p-3">
                    <Tabs className="w-full" defaultValue="references">
                        <TabsList>
                            <TabsTrigger value="references">References</TabsTrigger>

                            <TabsTrigger value="copies">Copies</TabsTrigger>
                        </TabsList>

                        <TabsContent value="references">
                            {referencesLoading && (
                                <div className="flex items-center justify-center py-8">
                                    <LoadingDots />
                                </div>
                            )}

                            {!referencesLoading && Boolean(referencesError) && (
                                <p className="text-sm text-destructive">Failed to load references.</p>
                            )}

                            {!referencesLoading && !referencesError && references.length === 0 && (
                                <p className="text-sm text-muted-foreground">
                                    No connected user has referenced this project yet.
                                </p>
                            )}

                            {!referencesLoading && !referencesError && references.length > 0 && (
                                <Table>
                                    <UsageTableHeader />

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

                                                <TableCell>
                                                    <div className="flex items-center gap-2">
                                                        <EnabledBadge enabled={reference.enabled} />

                                                        {reference.dangling && (
                                                            <Tooltip>
                                                                <TooltipTrigger>
                                                                    <Badge
                                                                        label="Dangling"
                                                                        styleType="warning-outline"
                                                                    />
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
                        </TabsContent>

                        <TabsContent value="copies">
                            {codeWorkflowProject && (
                                <p className="text-sm text-muted-foreground">Code workflows can&apos;t be copied.</p>
                            )}

                            {!codeWorkflowProject && copiesLoading && (
                                <div className="flex items-center justify-center py-8">
                                    <LoadingDots />
                                </div>
                            )}

                            {!codeWorkflowProject && !copiesLoading && Boolean(copiesError) && (
                                <p className="text-sm text-destructive">Failed to load copies.</p>
                            )}

                            {!codeWorkflowProject && !copiesLoading && !copiesError && copies.length === 0 && (
                                <p className="text-sm text-muted-foreground">
                                    No connected user has copied a workflow of this project yet.
                                </p>
                            )}

                            {!codeWorkflowProject && !copiesLoading && !copiesError && copies.length > 0 && (
                                <Table>
                                    <UsageTableHeader />

                                    <TableBody>
                                        {copies.map((copy) => (
                                            <TableRow key={copy.workflowUuid}>
                                                <TableCell>
                                                    {workflowLabelsByUuid.get(copy.copiedFromWorkflowUuid) ||
                                                        copy.copiedFromWorkflowUuid}
                                                </TableCell>

                                                <TableCell>
                                                    <div className="flex flex-col">
                                                        <span>{copy.externalUserId}</span>

                                                        {copy.connectedUserName && (
                                                            <span className="text-xs text-muted-foreground">
                                                                {copy.connectedUserName}
                                                            </span>
                                                        )}

                                                        {copy.connectedUserEmail && (
                                                            <span className="text-xs text-muted-foreground">
                                                                {copy.connectedUserEmail}
                                                            </span>
                                                        )}
                                                    </div>
                                                </TableCell>

                                                <TableCell>
                                                    <EnabledBadge enabled={copy.enabled} />
                                                </TableCell>
                                            </TableRow>
                                        ))}
                                    </TableBody>
                                </Table>
                            )}
                        </TabsContent>
                    </Tabs>
                </div>
            </SheetContent>
        </Sheet>
    );
};

export default AutomationWorkflowProjectUsageSheet;
