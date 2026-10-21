import '@/shared/styles/dropdownMenu.css';
import AlertDialog from '@/components/AlertDialog';
import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import EmptyState from '@/components/EmptyState/EmptyState';
import PageLoader from '@/components/PageLoader';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import AppApiCollectionBindingDialog from '@/pages/automation/apps/components/AppApiCollectionBindingDialog';
import AppApiCollectionWorkflows from '@/pages/automation/apps/components/AppApiCollectionWorkflows';
import ProjectDeploymentWorkflowExecutionsSheet from '@/pages/automation/project-deployments/components/project-deployment-workflow-executions-sheet/ProjectDeploymentWorkflowExecutionsSheet';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {WorkflowReadOnlyProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {useHasWorkspaceScope} from '@/shared/hooks/useHasWorkspaceScope';
import {
    AppApiCollectionBinding,
    useAppApiCollectionsQuery,
    useUnbindAppApiCollectionMutation,
} from '@/shared/middleware/graphql';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {handleActivationKeyDown} from '@/shared/util/keyboard-utils';
import {useQueryClient} from '@tanstack/react-query';
import {ChevronRightIcon, EllipsisVerticalIcon, NetworkIcon, Trash2Icon} from 'lucide-react';
import {Fragment, useMemo, useState} from 'react';
import {twMerge} from 'tailwind-merge';

interface AppApiCollectionsTabProps {
    appInstanceId: string;
    canManage: boolean;
}

const AppApiCollectionsTab = ({appInstanceId, canManage}: AppApiCollectionsTabProps) => {
    const [bindingToUnbind, setBindingToUnbind] = useState<
        Pick<AppApiCollectionBinding, 'apiCollectionId' | 'name'> | undefined
    >();
    const [expandedBindingIds, setExpandedBindingIds] = useState<string[]>([]);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const canEditDeployment = useHasWorkspaceScope(currentWorkspaceId, 'DEPLOYMENT_EDIT');

    const queryClient = useQueryClient();

    const {data, error, isLoading} = useAppApiCollectionsQuery({appInstanceId});

    const unbindAppApiCollectionMutation = useUnbindAppApiCollectionMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['appApiCollections']});

            setBindingToUnbind(undefined);
        },
    });

    const appApiCollections = useMemo(() => data?.appApiCollections ?? [], [data?.appApiCollections]);

    const toggleExpanded = (bindingId: string) =>
        setExpandedBindingIds((previousBindingIds) =>
            previousBindingIds.includes(bindingId)
                ? previousBindingIds.filter((previousBindingId) => previousBindingId !== bindingId)
                : [...previousBindingIds, bindingId]
        );

    const boundApiCollectionIds = useMemo(
        () => appApiCollections.map((appApiCollection) => appApiCollection.apiCollectionId),
        [appApiCollections]
    );

    const empty = !isLoading && !error && appApiCollections.length === 0;

    const bindApiCollectionButton = canManage && (
        <AppApiCollectionBindingDialog
            appInstanceId={appInstanceId}
            boundApiCollectionIds={boundApiCollectionIds}
            triggerNode={<Button label="Bind API Collection" />}
        />
    );

    return (
        <div className="flex flex-1 flex-col space-y-4">
            {!empty && (
                <div className="flex flex-col items-start gap-3 sm:flex-row sm:items-center sm:justify-between">
                    <p className="text-sm text-content-neutral-secondary">
                        The API collections whose endpoints this app serves with its secret key.
                    </p>

                    {bindApiCollectionButton}
                </div>
            )}

            <PageLoader errors={[error as Error | null]} loading={isLoading}>
                {appApiCollections.length > 0 ? (
                    <Table>
                        <TableHeader>
                            <TableRow>
                                <TableHead>Name</TableHead>

                                <TableHead className="max-sm:hidden">Context path</TableHead>

                                <TableHead className="max-md:hidden">Project</TableHead>

                                <TableHead />
                            </TableRow>
                        </TableHeader>

                        <TableBody>
                            {appApiCollections.map((appApiCollection) => {
                                const expanded = expandedBindingIds.includes(appApiCollection.id);
                                const hasDeployment =
                                    appApiCollection.projectDeploymentId != null &&
                                    appApiCollection.projectId != null &&
                                    appApiCollection.projectVersion != null;

                                return (
                                    <Fragment key={appApiCollection.id}>
                                        <TableRow
                                            aria-expanded={hasDeployment ? expanded : undefined}
                                            className={twMerge(
                                                hasDeployment && 'cursor-pointer',
                                                expanded && hasDeployment && 'border-b-0'
                                            )}
                                            onClick={
                                                hasDeployment ? () => toggleExpanded(appApiCollection.id) : undefined
                                            }
                                            onKeyDown={hasDeployment ? handleActivationKeyDown : undefined}
                                            tabIndex={hasDeployment ? 0 : undefined}
                                        >
                                            <TableCell className="font-medium">
                                                <span className="flex items-center gap-2">
                                                    {hasDeployment ? (
                                                        <ChevronRightIcon
                                                            aria-hidden="true"
                                                            className={twMerge(
                                                                'size-4 shrink-0 text-content-neutral-secondary duration-300',
                                                                expanded && 'rotate-90'
                                                            )}
                                                        />
                                                    ) : (
                                                        <span aria-hidden="true" className="size-4 shrink-0" />
                                                    )}

                                                    {appApiCollection.name}

                                                    {!!appApiCollection.collectionVersion && (
                                                        <Badge
                                                            label={`v${appApiCollection.collectionVersion}`}
                                                            styleType="secondary-filled"
                                                            weight="semibold"
                                                        />
                                                    )}

                                                    {!appApiCollection.available && (
                                                        <Badge
                                                            label="Unavailable"
                                                            styleType="warning-outline"
                                                            title="This collection, its deployment or its project is gone or disabled, so the app cannot serve its endpoints."
                                                        />
                                                    )}
                                                </span>
                                            </TableCell>

                                            <TableCell className="max-sm:hidden">
                                                {appApiCollection.contextPath && `/${appApiCollection.contextPath}`}
                                            </TableCell>

                                            <TableCell className="max-md:hidden">
                                                <div className="flex items-center gap-2">
                                                    {appApiCollection.projectName}

                                                    {appApiCollection.projectVersion != null && (
                                                        <Badge
                                                            label={`V${appApiCollection.projectVersion}`}
                                                            styleType="secondary-filled"
                                                            weight="semibold"
                                                        />
                                                    )}
                                                </div>
                                            </TableCell>

                                            <TableCell
                                                className="text-right"
                                                onClick={(event) => event.stopPropagation()}
                                            >
                                                {canManage && (
                                                    <DropdownMenu>
                                                        <DropdownMenuTrigger asChild>
                                                            <Button
                                                                aria-label={`${appApiCollection.name} actions`}
                                                                icon={<EllipsisVerticalIcon />}
                                                                size="icon"
                                                                variant="ghost"
                                                            />
                                                        </DropdownMenuTrigger>

                                                        <DropdownMenuContent align="end" className="p-0">
                                                            <DropdownMenuItem
                                                                className="dropdown-menu-item-destructive"
                                                                onClick={() => setBindingToUnbind(appApiCollection)}
                                                                variant="destructive"
                                                            >
                                                                <Trash2Icon /> Unbind
                                                            </DropdownMenuItem>
                                                        </DropdownMenuContent>
                                                    </DropdownMenu>
                                                )}
                                            </TableCell>
                                        </TableRow>

                                        {expanded && hasDeployment && (
                                            <TableRow className="hover:bg-transparent has-aria-expanded:bg-transparent">
                                                <TableCell colSpan={4}>
                                                    <AppApiCollectionWorkflows
                                                        apiCollectionId={Number(appApiCollection.apiCollectionId)}
                                                        canEdit={canManage && canEditDeployment}
                                                        projectDeploymentId={Number(
                                                            appApiCollection.projectDeploymentId
                                                        )}
                                                        projectId={Number(appApiCollection.projectId)}
                                                        projectName={appApiCollection.projectName}
                                                        projectVersion={appApiCollection.projectVersion!}
                                                    />
                                                </TableCell>
                                            </TableRow>
                                        )}
                                    </Fragment>
                                );
                            })}
                        </TableBody>
                    </Table>
                ) : (
                    <EmptyState
                        actions={bindApiCollectionButton}
                        description="Bind an API collection so the app can serve its endpoints with its secret key."
                        icon={<NetworkIcon />}
                        title="No API collections bound"
                    />
                )}
            </PageLoader>

            <AlertDialog
                confirmLabel="Unbind"
                description={`Unbind "${bindingToUnbind?.name}"? The app can no longer serve its endpoints.`}
                isPending={unbindAppApiCollectionMutation.isPending}
                onCancel={() => setBindingToUnbind(undefined)}
                onConfirm={() => {
                    if (bindingToUnbind) {
                        unbindAppApiCollectionMutation.mutate({
                            apiCollectionId: bindingToUnbind.apiCollectionId,
                            appInstanceId,
                        });
                    }
                }}
                open={bindingToUnbind !== undefined}
                title="Unbind API collection?"
            />

            <WorkflowReadOnlyProvider value={{useGetComponentDefinitionsQuery}}>
                <ProjectDeploymentWorkflowExecutionsSheet />
            </WorkflowReadOnlyProvider>
        </div>
    );
};

export default AppApiCollectionsTab;
