import '@/shared/styles/dropdownMenu.css';
import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import LazyLoadSVG from '@/components/LazyLoadSVG/LazyLoadSVG';
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import ConnectionProjectShareDialog from '@/pages/automation/connections/components/ConnectionProjectShareDialog';
import VisibilityMenuItems from '@/pages/automation/connections/components/VisibilityMenuItems';
import {useVisibilityFeatureEnabled} from '@/pages/automation/connections/hooks/useVisibilityFeatureEnabled';
import ConnectionDialog from '@/shared/components/connection/ConnectionDialog';
import {Connection, Tag} from '@/shared/middleware/automation/configuration';
import {
    useDemoteConnectionToPrivateMutation,
    usePromoteConnectionToWorkspaceMutation,
} from '@/shared/middleware/graphql';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {useUpdateConnectionTagsMutation} from '@/shared/mutations/automation/connectionTags.mutations';
import {
    useDeleteConnectionMutation,
    useDisconnectConnectionMutation,
    useUpdateConnectionMutation,
} from '@/shared/mutations/automation/connections.mutations';
import {ConnectionKeys, useGetConnectionTagsQuery} from '@/shared/queries/automation/connections.queries';
import {useGetWorkspaceProjectsQuery} from '@/shared/queries/automation/projects.queries';
import {
    ComponentDefinitionKeys,
    useGetConnectionComponentDefinitionQuery,
} from '@/shared/queries/platform/componentDefinitions.queries';
import {useQueryClient} from '@tanstack/react-query';
import {ComponentIcon, EditIcon, EllipsisVerticalIcon, Link2OffIcon, Trash2Icon} from 'lucide-react';
import {memo, useState} from 'react';
import {toast} from 'sonner';

import TagList from '../../../../../shared/components/TagList';
import ConnectionScopeBadge from '../ConnectionScopeBadge';

interface ConnectionListItemProps {
    componentDefinitions: ComponentDefinitionBasic[];
    connection: Connection;
    remainingTags?: Tag[];
}

const ConnectionListItem = memo(({componentDefinitions, connection, remainingTags}: ConnectionListItemProps) => {
    const [showEditDialog, setShowEditDialog] = useState(false);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [showDemoteConfirmDialog, setShowDemoteConfirmDialog] = useState(false);
    const [showDisconnectDialog, setShowDisconnectDialog] = useState(false);
    const [showProjectShareDialog, setShowProjectShareDialog] = useState(false);

    const {data: componentDefinition} = useGetConnectionComponentDefinitionQuery({
        componentName: connection.componentName,
        connectionVersion: connection.connectionVersion,
    });

    const {enabled: visibilityFeatureEnabled, workspaceId: currentWorkspaceId} = useVisibilityFeatureEnabled();

    const {data: workspaceProjects} = useGetWorkspaceProjectsQuery(
        {id: currentWorkspaceId!},
        visibilityFeatureEnabled && !!currentWorkspaceId && connection.visibility === 'PROJECT'
    );

    const sharedProjectNames =
        connection.visibility === 'PROJECT' && connection.sharedProjectIds && workspaceProjects
            ? connection.sharedProjectIds
                  .map((projectId) => workspaceProjects.find((project) => project.id === projectId)?.name)
                  .filter((name): name is string => !!name)
            : undefined;

    const queryClient = useQueryClient();

    const invalidateConnections = () => {
        queryClient.invalidateQueries({queryKey: ConnectionKeys.connections});
        queryClient.invalidateQueries({queryKey: ComponentDefinitionKeys.componentDefinitions});
    };

    const promoteConnectionToWorkspaceMutation = usePromoteConnectionToWorkspaceMutation({
        onSuccess: () => {
            invalidateConnections();
            toast(`"${connection.name}" is now shared with the workspace.`);
        },
    });

    const demoteConnectionToPrivateMutation = useDemoteConnectionToPrivateMutation({
        onError: (error) => {
            // Close the confirm dialog on failure too so the user can retry via the menu; the global
            // useFetchInterceptor toast surfaces the error. Leaving the dialog open would hide the
            // toast behind it and let the user click "Make private" again, queuing a second mutation.
            // Log the error so a devtools trace is available alongside the global toast — future
            // refactors that bypass the interceptor would otherwise leave no record of the failure.
            console.error('demoteConnectionToPrivate failed', error);

            setShowDemoteConfirmDialog(false);
        },
        onSuccess: () => {
            setShowDemoteConfirmDialog(false);
            invalidateConnections();
            toast(`"${connection.name}" is now private.`);
        },
    });

    const deleteConnectionMutation = useDeleteConnectionMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ComponentDefinitionKeys.componentDefinitions,
            });
            queryClient.invalidateQueries({
                queryKey: ConnectionKeys.connections,
            });
            queryClient.invalidateQueries({
                queryKey: ConnectionKeys.connectionTags,
            });

            setShowDeleteDialog(false);

            toast(`Connection "${connection.name}" has been successfully deleted.`);
        },
    });

    const disconnectConnectionMutation = useDisconnectConnectionMutation({
        onError: () => {
            setShowDisconnectDialog(false);
        },
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ComponentDefinitionKeys.componentDefinitions,
            });
            queryClient.invalidateQueries({
                queryKey: ConnectionKeys.connections,
            });
            queryClient.invalidateQueries({
                queryKey: ConnectionKeys.connectionTags,
            });

            setShowDisconnectDialog(false);

            toast(`"${connection.name}" was successfully disconnected from all workflows.`);
        },
    });

    const updateConnectionTagsMutation = useUpdateConnectionTagsMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ConnectionKeys.connections,
            });
            queryClient.invalidateQueries({
                queryKey: ConnectionKeys.connectionTags,
            });
        },
    });

    const handleAlertDeleteDialogClick = () => {
        if (connection.id) {
            deleteConnectionMutation.mutate(connection.id);
        }
    };

    const handleDisconnectFromAllClick = () => {
        if (connection.id) {
            disconnectConnectionMutation.mutate(connection.id);
        }
    };

    const renderVisibilityMenu = () => {
        if (!visibilityFeatureEnabled || !connection.id || !currentWorkspaceId) {
            return null;
        }

        const connectionIdStr = String(connection.id);
        const workspaceIdStr = String(currentWorkspaceId);
        const visibility = connection.visibility || 'PRIVATE';

        return (
            <VisibilityMenuItems
                connectionId={connectionIdStr}
                onDemoteRequest={(currentVisibility) => {
                    if (currentVisibility === 'PROJECT') {
                        setShowDemoteConfirmDialog(true);
                    } else {
                        demoteConnectionToPrivateMutation.mutate({
                            connectionId: connectionIdStr,
                            workspaceId: workspaceIdStr,
                        });
                    }
                }}
                onPromoteToWorkspace={(variables) => promoteConnectionToWorkspaceMutation.mutate(variables)}
                onShareWithProjects={() => setShowProjectShareDialog(true)}
                visibility={visibility}
                workspaceId={workspaceIdStr}
            />
        );
    };

    if (!componentDefinition) {
        return <></>;
    }

    return (
        <li key={connection.id}>
            <>
                <div className="group flex items-center rounded-md bg-white px-2 hover:bg-gray-50">
                    <div className="flex flex-1 items-center py-5">
                        <div className="flex-1">
                            <div className="flex items-center justify-between">
                                <div className="relative flex items-center gap-2">
                                    <LazyLoadSVG
                                        className="size-5 flex-none"
                                        preloader={<ComponentIcon />}
                                        src={componentDefinition.icon!}
                                    />

                                    <span className="text-base font-semibold">{connection.name}</span>

                                    {visibilityFeatureEnabled && connection.id && currentWorkspaceId ? (
                                        <DropdownMenu>
                                            <DropdownMenuTrigger asChild>
                                                <button
                                                    aria-label="Change visibility"
                                                    className="cursor-pointer rounded-sm hover:bg-gray-100"
                                                    type="button"
                                                >
                                                    <ConnectionScopeBadge
                                                        sharedProjectNames={sharedProjectNames}
                                                        visibility={connection.visibility || 'PRIVATE'}
                                                    />
                                                </button>
                                            </DropdownMenuTrigger>

                                            <DropdownMenuContent align="start" className="p-0">
                                                {renderVisibilityMenu()}
                                            </DropdownMenuContent>
                                        </DropdownMenu>
                                    ) : (
                                        visibilityFeatureEnabled && (
                                            <ConnectionScopeBadge visibility={connection.visibility || 'PRIVATE'} />
                                        )
                                    )}
                                </div>
                            </div>

                            <div className="mt-2 sm:flex sm:items-center sm:justify-between">
                                <div
                                    className="flex h-connection-list-item-taglist-height items-center"
                                    onClick={(event) => event.preventDefault()}
                                >
                                    {connection.tags && (
                                        <TagList
                                            getRequest={(id, tags) => ({
                                                id: id!,
                                                updateTagsRequest: {
                                                    tags: tags || [],
                                                },
                                            })}
                                            id={connection.id!}
                                            remainingTags={remainingTags}
                                            tags={connection.tags}
                                            updateTagsMutation={updateConnectionTagsMutation}
                                        />
                                    )}
                                </div>
                            </div>
                        </div>

                        <div className="flex items-center justify-end gap-x-6">
                            <div className="flex min-w-52 flex-col items-end gap-y-4">
                                {connection.credentialStatus === 'VALID' ? (
                                    <Badge
                                        className="uppercase"
                                        label={connection.active ? 'Active' : 'Not Active'}
                                        styleType={connection.active ? 'success-outline' : 'secondary-outline'}
                                        weight="semibold"
                                    />
                                ) : (
                                    <Badge
                                        className="uppercase"
                                        label={connection.credentialStatus ?? 'INVALID'}
                                        styleType="destructive-outline"
                                        weight="semibold"
                                    />
                                )}

                                {connection.createdDate && (
                                    <Tooltip>
                                        <TooltipTrigger className="flex items-center text-sm text-gray-500 sm:mt-0">
                                            <span className="text-xs">
                                                {`Created at ${connection.createdDate?.toLocaleDateString()} ${connection.createdDate?.toLocaleTimeString()}`}
                                            </span>
                                        </TooltipTrigger>

                                        <TooltipContent>Created Date</TooltipContent>
                                    </Tooltip>
                                )}
                            </div>

                            <DropdownMenu>
                                <DropdownMenuTrigger asChild>
                                    <Button icon={<EllipsisVerticalIcon />} size="icon" variant="ghost" />
                                </DropdownMenuTrigger>

                                <DropdownMenuContent align="end" className="p-0">
                                    <DropdownMenuItem
                                        className="dropdown-menu-item"
                                        onClick={() => setShowEditDialog(true)}
                                    >
                                        <EditIcon /> Edit
                                    </DropdownMenuItem>

                                    {renderVisibilityMenu()}

                                    <DropdownMenuSeparator className="m-0" />

                                    {connection.active === true && (
                                        <DropdownMenuItem
                                            className="dropdown-menu-item"
                                            onClick={() => setShowDisconnectDialog(true)}
                                        >
                                            <Link2OffIcon /> Disconnect from all
                                        </DropdownMenuItem>
                                    )}

                                    <Tooltip>
                                        <TooltipTrigger asChild>
                                            <div>
                                                <DropdownMenuItem
                                                    className={
                                                        connection.active === true
                                                            ? 'dropdown-menu-item-destructive-disabled'
                                                            : 'dropdown-menu-item-destructive'
                                                    }
                                                    disabled={connection.active}
                                                    onClick={() => setShowDeleteDialog(true)}
                                                >
                                                    <Trash2Icon /> Delete
                                                </DropdownMenuItem>
                                            </div>
                                        </TooltipTrigger>

                                        <TooltipContent side="left">
                                            {connection.active === true
                                                ? 'Disconnect from all workflows first to enable deletion.'
                                                : 'Delete the connection permanently.'}
                                        </TooltipContent>
                                    </Tooltip>
                                </DropdownMenuContent>
                            </DropdownMenu>
                        </div>
                    </div>
                </div>

                <AlertDialog open={showDeleteDialog}>
                    <AlertDialogContent>
                        <AlertDialogHeader>
                            <AlertDialogTitle>Are you absolutely sure?</AlertDialogTitle>

                            <AlertDialogDescription>
                                This action cannot be undone. This will permanently delete the connection.
                            </AlertDialogDescription>
                        </AlertDialogHeader>

                        <AlertDialogFooter>
                            <AlertDialogCancel onClick={() => setShowDeleteDialog(false)}>Cancel</AlertDialogCancel>

                            <AlertDialogAction
                                className="bg-surface-destructive-primary shadow-none hover:bg-surface-destructive-primary-hover active:bg-surface-destructive-primary-active"
                                onClick={handleAlertDeleteDialogClick}
                            >
                                Delete
                            </AlertDialogAction>
                        </AlertDialogFooter>
                    </AlertDialogContent>
                </AlertDialog>

                <AlertDialog open={showDisconnectDialog}>
                    <AlertDialogContent>
                        <AlertDialogHeader>
                            <AlertDialogTitle>
                                Disconnect <strong>{connection.name}</strong> from all workflows?
                            </AlertDialogTitle>

                            <AlertDialogDescription>This action cannot be undone.</AlertDialogDescription>
                        </AlertDialogHeader>

                        <AlertDialogFooter>
                            <AlertDialogCancel onClick={() => setShowDisconnectDialog(false)}>Cancel</AlertDialogCancel>

                            <AlertDialogAction
                                className="bg-surface-destructive-primary shadow-none hover:bg-surface-destructive-primary-hover active:bg-surface-destructive-primary-active"
                                onClick={handleDisconnectFromAllClick}
                            >
                                <Link2OffIcon className="size-4" />
                                Disconnect from all
                            </AlertDialogAction>
                        </AlertDialogFooter>
                    </AlertDialogContent>
                </AlertDialog>

                {visibilityFeatureEnabled && (
                    <AlertDialog open={showDemoteConfirmDialog}>
                        <AlertDialogContent>
                            <AlertDialogHeader>
                                <AlertDialogTitle>Make connection private?</AlertDialogTitle>

                                <AlertDialogDescription>
                                    {connection.sharedProjectIds && connection.sharedProjectIds.length > 0
                                        ? `This will revoke access from ${connection.sharedProjectIds.length} project(s). The connection will become private to you.`
                                        : 'The connection will become private to you.'}
                                </AlertDialogDescription>
                            </AlertDialogHeader>

                            <AlertDialogFooter>
                                <AlertDialogCancel
                                    disabled={demoteConnectionToPrivateMutation.isPending}
                                    onClick={() => setShowDemoteConfirmDialog(false)}
                                >
                                    Cancel
                                </AlertDialogCancel>

                                <AlertDialogAction
                                    disabled={demoteConnectionToPrivateMutation.isPending}
                                    onClick={(event) => {
                                        // Radix default closes the dialog on click; we want it to stay open until
                                        // the mutation settles, so the onSuccess/onError handlers own dismissal.
                                        event.preventDefault();

                                        if (connection.id && currentWorkspaceId) {
                                            demoteConnectionToPrivateMutation.mutate({
                                                connectionId: String(connection.id),
                                                workspaceId: String(currentWorkspaceId),
                                            });
                                        }
                                    }}
                                >
                                    Make private
                                </AlertDialogAction>
                            </AlertDialogFooter>
                        </AlertDialogContent>
                    </AlertDialog>
                )}

                {visibilityFeatureEnabled && showProjectShareDialog && currentWorkspaceId && (
                    <ConnectionProjectShareDialog
                        connection={connection}
                        onClose={() => setShowProjectShareDialog(false)}
                        open={showProjectShareDialog}
                        workspaceId={currentWorkspaceId}
                    />
                )}

                {showEditDialog && componentDefinitions && (
                    <ConnectionDialog
                        componentDefinitions={componentDefinitions}
                        connection={connection}
                        connectionTagsQueryKey={ConnectionKeys.connectionTags}
                        connectionsQueryKey={ConnectionKeys.connections}
                        onClose={() => setShowEditDialog(false)}
                        useGetConnectionTagsQuery={useGetConnectionTagsQuery}
                        useUpdateConnectionMutation={useUpdateConnectionMutation}
                    />
                )}
            </>
        </li>
    );
});

ConnectionListItem.displayName = 'ConnectionListItem';

export default ConnectionListItem;
