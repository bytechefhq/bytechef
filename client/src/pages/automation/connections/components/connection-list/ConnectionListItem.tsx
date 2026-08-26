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
import ConnectionDialog from '@/shared/components/connection/ConnectionDialog';
import {connectionCredentialStoreLabels} from '@/shared/components/connection/connectionCredentialStoreLabels';
import ResourceVisibilityBadge from '@/shared/components/visibility/ResourceVisibilityBadge';
import ResourceVisibilityPicker from '@/shared/components/visibility/ResourceVisibilityPicker';
import {useVisibilityFeatureEnabled} from '@/shared/hooks/useVisibilityFeatureEnabled';
import {Connection, Tag} from '@/shared/middleware/automation/configuration';
import {
    ConnectionCredentialStoreType,
    ResourceVisibility,
    useConnectionGrantsQuery,
    useGrantConnectionAccessMutation,
    useRevokeConnectionAccessMutation,
    useSetConnectionVisibilityMutation,
    useWorkspaceUsersQuery,
} from '@/shared/middleware/graphql';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {useUpdateConnectionTagsMutation} from '@/shared/mutations/automation/connectionTags.mutations';
import {
    useDeleteConnectionMutation,
    useDisconnectConnectionMutation,
    useUpdateConnectionCredentialsMutation,
    useUpdateConnectionMutation,
} from '@/shared/mutations/automation/connections.mutations';
import {ConnectionKeys, useGetConnectionTagsQuery} from '@/shared/queries/automation/connections.queries';
import {ComponentDefinitionKeys} from '@/shared/queries/platform/componentDefinitions.queries';
import {useQueryClient} from '@tanstack/react-query';
import {ComponentIcon, EditIcon, EllipsisVerticalIcon, Link2OffIcon, Trash2Icon} from 'lucide-react';
import {memo, useMemo, useState} from 'react';
import {toast} from 'sonner';

import TagList from '../../../../../shared/components/TagList';

interface ConnectionListItemProps {
    componentDefinitions: ComponentDefinitionBasic[];
    connection: Connection;
    remainingTags?: Tag[];
}

const ConnectionListItem = memo(({componentDefinitions, connection, remainingTags}: ConnectionListItemProps) => {
    const [showEditDialog, setShowEditDialog] = useState(false);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [showDisconnectDialog, setShowDisconnectDialog] = useState(false);

    const {enabled: visibilityFeatureEnabled, workspaceId: currentWorkspaceId} = useVisibilityFeatureEnabled();

    const connectionTagsQueryResult = useGetConnectionTagsQuery(currentWorkspaceId!);

    const queryClient = useQueryClient();

    const invalidateConnections = () => {
        queryClient.invalidateQueries({queryKey: ConnectionKeys.connections});
        queryClient.invalidateQueries({queryKey: ComponentDefinitionKeys.componentDefinitions});
    };

    const setConnectionVisibilityMutation = useSetConnectionVisibilityMutation({
        onSuccess: () => {
            invalidateConnections();
        },
    });

    const grantConnectionAccessMutation = useGrantConnectionAccessMutation({
        onSuccess: () => invalidateConnections(),
    });

    const revokeConnectionAccessMutation = useRevokeConnectionAccessMutation({
        onSuccess: () => invalidateConnections(),
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
                queryKey: ConnectionKeys.connectionTags(currentWorkspaceId!),
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
                queryKey: ConnectionKeys.connectionTags(currentWorkspaceId!),
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
                queryKey: ConnectionKeys.connectionTags(currentWorkspaceId!),
            });
        },
    });

    const componentDefinition = useMemo(() => {
        const matchingComponentDefinitions = componentDefinitions.filter(
            (definition) => definition.name === connection.componentName
        );

        // The list normally holds a single entry per component, but if several
        // versions are present resolve to the latest so the icon is picked
        // deterministically instead of depending on array order.
        return matchingComponentDefinitions.reduce<ComponentDefinitionBasic | undefined>(
            (latest, definition) => (!latest || definition.version > latest.version ? definition : latest),
            undefined
        );
    }, [componentDefinitions, connection.componentName]);

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

    // Only a withheld connection can have a meaningful audience, so the two lookups the picker needs are skipped
    // entirely for the workspace-visible majority.
    const isWithheld = connection.visibility === 'PRIVATE';

    const connectionGrantsQuery = useConnectionGrantsQuery(
        {connectionId: String(connection.id), workspaceId: String(currentWorkspaceId)},
        {enabled: visibilityFeatureEnabled && isWithheld && !!connection.id && !!currentWorkspaceId}
    );

    const workspaceUsersQuery = useWorkspaceUsersQuery(
        {workspaceId: String(currentWorkspaceId)},
        {enabled: visibilityFeatureEnabled && isWithheld && !!currentWorkspaceId}
    );

    const grantedUserIds = (connectionGrantsQuery.data?.connectionGrants ?? []).map(Number);

    const workspaceMembers = (workspaceUsersQuery.data?.workspaceUsers ?? []).map((workspaceUser) => ({
        label: workspaceUser.user?.email ?? `User ${workspaceUser.userId}`,
        userId: Number(workspaceUser.userId),
    }));

    const renderVisibilityPicker = () => {
        if (!visibilityFeatureEnabled || !connection.id || !currentWorkspaceId) {
            return null;
        }

        const connectionIdStr = String(connection.id);
        const workspaceIdStr = String(currentWorkspaceId);

        return (
            <div className="min-w-64 p-3">
                <ResourceVisibilityPicker
                    grantedUserIds={grantedUserIds}
                    onGrantedUserIdsChange={(nextUserIds) => {
                        // Diff rather than replace: the server has no set-grants operation, and expressing it as
                        // one would mean revoking every grant and re-adding it on each keystroke.
                        grantedUserIds
                            .filter((userId) => !nextUserIds.includes(userId))
                            .forEach((userId) =>
                                revokeConnectionAccessMutation.mutate({
                                    connectionId: connectionIdStr,
                                    userId: String(userId),
                                    workspaceId: workspaceIdStr,
                                })
                            );

                        nextUserIds
                            .filter((userId) => !grantedUserIds.includes(userId))
                            .forEach((userId) =>
                                grantConnectionAccessMutation.mutate({
                                    connectionId: connectionIdStr,
                                    userId: String(userId),
                                    workspaceId: workspaceIdStr,
                                })
                            );
                    }}
                    onVisibilityChange={(visibility) =>
                        setConnectionVisibilityMutation.mutate({
                            connectionId: connectionIdStr,
                            // The picker speaks plain strings so it does not depend on generated GraphQL types.
                            // The generated enum's values are exactly those strings, so this is a representation
                            // cast rather than a claim about the value.
                            visibility: visibility as ResourceVisibility,
                            workspaceId: workspaceIdStr,
                        })
                    }
                    visibility={connection.visibility || 'WORKSPACE'}
                    workspaceMembers={workspaceMembers}
                />
            </div>
        );
    };

    return (
        <li className="mb-2 rounded border border-border/50" key={connection.id}>
            <>
                <div className="group flex items-center rounded-md bg-surface-neutral-primary px-3 hover:bg-destructive-foreground">
                    <div className="flex flex-1 items-center py-3">
                        <div className="flex-1">
                            <div className="flex min-h-8 items-center justify-between">
                                <div className="relative flex items-center gap-2">
                                    {componentDefinition?.icon ? (
                                        <LazyLoadSVG
                                            className="size-5 flex-none"
                                            preloader={<ComponentIcon />}
                                            src={componentDefinition.icon}
                                        />
                                    ) : (
                                        <ComponentIcon className="size-5 flex-none" />
                                    )}

                                    <span className="text-base font-semibold">{connection.name}</span>

                                    {visibilityFeatureEnabled && connection.id && currentWorkspaceId ? (
                                        <DropdownMenu>
                                            <DropdownMenuTrigger asChild>
                                                <button
                                                    aria-label="Change visibility"
                                                    className="cursor-pointer rounded-sm hover:bg-surface-neutral-primary-hover"
                                                    type="button"
                                                >
                                                    <ResourceVisibilityBadge
                                                        grantedUserCount={grantedUserIds.length}
                                                        visibility={connection.visibility || 'WORKSPACE'}
                                                    />
                                                </button>
                                            </DropdownMenuTrigger>

                                            <DropdownMenuContent align="start" className="p-0">
                                                {renderVisibilityPicker()}
                                            </DropdownMenuContent>
                                        </DropdownMenu>
                                    ) : (
                                        visibilityFeatureEnabled && (
                                            <ResourceVisibilityBadge
                                                visibility={connection.visibility || 'WORKSPACE'}
                                            />
                                        )
                                    )}
                                </div>
                            </div>

                            <div className="mt-2 min-h-7 sm:flex sm:items-center sm:justify-between">
                                <div className="flex items-center" onClick={(event) => event.preventDefault()}>
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
                            <div className="flex min-w-52 flex-col items-end gap-y-2">
                                <div className="flex min-h-8 flex-wrap items-center justify-end gap-2">
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

                                    {connection.credentialStoreType &&
                                        connection.credentialStoreType !== 'DATABASE' && (
                                            <Tooltip>
                                                <TooltipTrigger asChild>
                                                    <Badge
                                                        label={
                                                            connectionCredentialStoreLabels[
                                                                connection.credentialStoreType as unknown as ConnectionCredentialStoreType
                                                            ]
                                                        }
                                                        styleType="secondary-outline"
                                                        weight="semibold"
                                                    />
                                                </TooltipTrigger>

                                                <TooltipContent>Credentials stored externally</TooltipContent>
                                            </Tooltip>
                                        )}
                                </div>

                                {connection.createdDate && (
                                    <Tooltip>
                                        <TooltipTrigger className="flex min-h-7 items-center text-sm text-content-neutral-secondary sm:mt-0">
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
                                    {!connection.managed && (
                                        <DropdownMenuItem
                                            className="dropdown-menu-item"
                                            onClick={() => setShowEditDialog(true)}
                                        >
                                            <EditIcon /> Edit
                                        </DropdownMenuItem>
                                    )}

                                    <DropdownMenuSeparator className="m-0" />

                                    {connection.active === true && (
                                        <DropdownMenuItem
                                            className="dropdown-menu-item"
                                            onClick={() => setShowDisconnectDialog(true)}
                                        >
                                            <Link2OffIcon /> Disconnect from all
                                        </DropdownMenuItem>
                                    )}

                                    {!connection.managed && (
                                        <div
                                            title={
                                                connection.active === true
                                                    ? 'Disconnect from all workflows first to enable deletion'
                                                    : 'Delete the connection'
                                            }
                                        >
                                            <DropdownMenuItem
                                                className={
                                                    connection.active === true
                                                        ? 'dropdown-menu-item-destructive-disabled'
                                                        : 'dropdown-menu-item-destructive'
                                                }
                                                disabled={connection.active}
                                                onClick={() => setShowDeleteDialog(true)}
                                                variant="destructive"
                                            >
                                                <Trash2Icon /> Delete
                                            </DropdownMenuItem>
                                        </div>
                                    )}
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

                {showEditDialog && componentDefinitions && (
                    <ConnectionDialog
                        componentDefinitions={componentDefinitions}
                        connection={connection}
                        connectionTagsQueryKey={ConnectionKeys.connectionTags(currentWorkspaceId!)}
                        connectionsQueryKey={ConnectionKeys.connections}
                        onClose={() => setShowEditDialog(false)}
                        useGetConnectionTagsQuery={() => connectionTagsQueryResult}
                        useUpdateConnectionCredentialsMutation={useUpdateConnectionCredentialsMutation}
                        useUpdateConnectionMutation={useUpdateConnectionMutation}
                    />
                )}
            </>
        </li>
    );
});

ConnectionListItem.displayName = 'ConnectionListItem';

export default ConnectionListItem;
