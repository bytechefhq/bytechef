import AlertDialog from '@/components/AlertDialog';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import {useVisibilityFeatureEnabled} from '@/pages/automation/connections/hooks/useVisibilityFeatureEnabled';
import {PlatformType} from '@/pages/home/stores/usePlatformTypeStore';
import TagList from '@/shared/components/TagList';
import ConnectionDialog from '@/shared/components/connection/ConnectionDialog';
import ConnectionListItemLayout from '@/shared/components/connection/ConnectionListItemLayout';
import ConnectionVisibilityPicker from '@/shared/components/connection/ConnectionVisibilityPicker';
import {useHasWorkspaceScope} from '@/shared/hooks/useHasWorkspaceScope';
import {useIsTenantAdmin} from '@/shared/hooks/useIsTenantAdmin';
import {Connection, Tag} from '@/shared/middleware/automation/configuration';
import {
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
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {useQueryClient} from '@tanstack/react-query';
import {Link2OffIcon, Trash2Icon} from 'lucide-react';
import {memo, useMemo, useState} from 'react';
import {toast} from 'sonner';

import ConnectionScopeBadge from '../ConnectionScopeBadge';

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
    const currentUserLogin = useAuthenticationStore((state) => state.account?.login);

    const canEdit = useHasWorkspaceScope(currentWorkspaceId, 'CONNECTION_EDIT');
    const canDelete = useHasWorkspaceScope(currentWorkspaceId, 'CONNECTION_DELETE');
    const isTenantAdmin = useIsTenantAdmin();

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
            void queryClient.invalidateQueries({
                queryKey: ComponentDefinitionKeys.componentDefinitions,
            });
            void queryClient.invalidateQueries({
                queryKey: ConnectionKeys.connections,
            });
            void queryClient.invalidateQueries({
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
            void queryClient.invalidateQueries({
                queryKey: ComponentDefinitionKeys.componentDefinitions,
            });
            void queryClient.invalidateQueries({
                queryKey: ConnectionKeys.connections,
            });
            void queryClient.invalidateQueries({
                queryKey: ConnectionKeys.connectionTags,
            });

            setShowDisconnectDialog(false);

            toast(`"${connection.name}" was successfully disconnected from all workflows.`);
        },
    });

    const updateConnectionTagsMutation = useUpdateConnectionTagsMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({
                queryKey: ConnectionKeys.connections,
            });
            void queryClient.invalidateQueries({
                queryKey: ConnectionKeys.connectionTags,
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

    const canReplaceCredentials =
        isTenantAdmin || (!!connection.createdBy && connection.createdBy === currentUserLogin);

    const canDisconnect = isTenantAdmin && connection.active === true;

    const hasSecondaryActions = canDisconnect || canDelete;

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
                <ConnectionVisibilityPicker
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
        <ConnectionListItemLayout
            active={connection.active}
            badges={
                visibilityFeatureEnabled &&
                (connection.id && currentWorkspaceId ? (
                    <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                            <button
                                aria-label="Change visibility"
                                className="cursor-pointer rounded-sm hover:bg-gray-100"
                                type="button"
                            >
                                <ConnectionScopeBadge
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
                    <ConnectionScopeBadge visibility={connection.visibility || 'WORKSPACE'} />
                ))
            }
            createdDate={connection.createdDate}
            credentialStatus={connection.credentialStatus}
            icon={componentDefinition?.icon}
            id={connection.id}
            menuItems={
                hasSecondaryActions && (
                    <>
                        {canDisconnect && (
                            <DropdownMenuItem
                                icon={<Link2OffIcon />}
                                label="Disconnect from all"
                                onClick={() => setShowDisconnectDialog(true)}
                            />
                        )}

                        {canDelete && (
                            <div
                                title={
                                    connection.active === true
                                        ? 'Disconnect from all workflows first to enable deletion'
                                        : 'Delete the connection'
                                }
                            >
                                <DropdownMenuItem
                                    disabled={connection.active}
                                    icon={<Trash2Icon />}
                                    label="Delete"
                                    onClick={() => setShowDeleteDialog(true)}
                                    variant="destructive"
                                />
                            </div>
                        )}
                    </>
                )
            }
            name={connection.name}
            onEditClick={canEdit ? () => setShowEditDialog(true) : undefined}
            tagList={
                connection.tags && (
                    <TagList
                        getRequest={(id, tags) => ({
                            id: id!,
                            updateTagsRequest: {
                                tags,
                            },
                        })}
                        id={connection.id!}
                        readOnly={!canEdit}
                        remainingTags={remainingTags}
                        tags={connection.tags}
                        updateTagsMutation={updateConnectionTagsMutation}
                    />
                )
            }
        >
            <AlertDialog
                description="This action cannot be undone. This will permanently delete the connection."
                isPending={deleteConnectionMutation.isPending}
                onCancel={() => setShowDeleteDialog(false)}
                onConfirm={handleAlertDeleteDialogClick}
                open={showDeleteDialog}
            />

            <AlertDialog
                confirmIcon={<Link2OffIcon />}
                confirmLabel="Disconnect from all"
                description="This action cannot be undone."
                isPending={disconnectConnectionMutation.isPending}
                onCancel={() => setShowDisconnectDialog(false)}
                onConfirm={handleDisconnectFromAllClick}
                open={showDisconnectDialog}
                title={`Disconnect ${connection.name} from all workflows?`}
            />

            {showEditDialog && componentDefinitions && (
                <ConnectionDialog
                    componentDefinitions={componentDefinitions}
                    connection={connection}
                    connectionPropertiesEditable
                    connectionTagsQueryKey={ConnectionKeys.connectionTags}
                    connectionsQueryKey={ConnectionKeys.connections}
                    onClose={() => setShowEditDialog(false)}
                    platformType={PlatformType.AUTOMATION}
                    useGetConnectionTagsQuery={useGetConnectionTagsQuery}
                    useUpdateConnectionCredentialsMutation={
                        canReplaceCredentials ? useUpdateConnectionCredentialsMutation : undefined
                    }
                    useUpdateConnectionMutation={useUpdateConnectionMutation}
                />
            )}
        </ConnectionListItemLayout>
    );
});

ConnectionListItem.displayName = 'ConnectionListItem';

export default ConnectionListItem;
