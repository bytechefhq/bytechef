import AlertDialog from '@/components/AlertDialog';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import useOpenInProject from '@/pages/automation/project-deployments/hooks/useOpenInProject';
import {PlatformType} from '@/pages/home/stores/usePlatformTypeStore';
import TagList from '@/shared/components/TagList';
import ConnectionDialog from '@/shared/components/connection/ConnectionDialog';
import ConnectionListItemLayout from '@/shared/components/connection/ConnectionListItemLayout';
import ConnectionWorkflowList, {type ConnectionWorkflowI} from '@/shared/components/connection/ConnectionWorkflowList';
import ResourceVisibilityBadge from '@/shared/components/visibility/ResourceVisibilityBadge';
import ResourceVisibilityPicker from '@/shared/components/visibility/ResourceVisibilityPicker';
import {useCanManageResourceSharing} from '@/shared/hooks/useCanManageResourceSharing';
import {useHasWorkspaceScope} from '@/shared/hooks/useHasWorkspaceScope';
import {useIsResourceOwnerOrTenantAdmin} from '@/shared/hooks/useIsResourceOwnerOrTenantAdmin';
import {useIsTenantAdmin} from '@/shared/hooks/useIsTenantAdmin';
import {useVisibilityFeatureEnabled} from '@/shared/hooks/useVisibilityFeatureEnabled';
import {Connection, Tag} from '@/shared/middleware/automation/configuration';
import {
    ConnectionWorkflowUsage,
    ResourceVisibility,
    useConnectionGrantsQuery,
    useConnectionWorkflowsQuery,
    useDisconnectConnectionFromWorkflowMutation,
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
import {Link2OffIcon, Trash2Icon, UserCogIcon} from 'lucide-react';
import {memo, useMemo, useState} from 'react';
import {toast} from 'sonner';

import ConnectionReassignmentDialog from '../ConnectionReassignmentDialog';

const CONNECTION_WORKFLOWS_QUERY_KEY = ['ConnectionWorkflows'];

interface ConnectionListItemProps {
    componentDefinitions: ComponentDefinitionBasic[];
    connection: Connection;
    remainingTags?: Tag[];
}

const ConnectionListItem = memo(({componentDefinitions, connection, remainingTags}: ConnectionListItemProps) => {
    const [showEditDialog, setShowEditDialog] = useState(false);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [showDisconnectDialog, setShowDisconnectDialog] = useState(false);
    const [connectionWorkflowToDisconnect, setConnectionWorkflowToDisconnect] = useState<ConnectionWorkflowI>();
    const [workflowsOpen, setWorkflowsOpen] = useState(false);
    const [showReassignmentDialog, setShowReassignmentDialog] = useState(false);
    const [showVisibilityMenu, setShowVisibilityMenu] = useState(false);

    const {enabled: visibilityFeatureEnabled, workspaceId: currentWorkspaceId} = useVisibilityFeatureEnabled();

    const hasConnectionDeleteScope = useHasWorkspaceScope(currentWorkspaceId, 'CONNECTION_DELETE');
    const hasConnectionEditScope = useHasWorkspaceScope(currentWorkspaceId, 'CONNECTION_EDIT');
    const canManageResourceSharing = useCanManageResourceSharing(connection.createdBy);
    const isConnectionOwnerOrTenantAdmin = useIsResourceOwnerOrTenantAdmin(connection.createdBy);
    const isTenantAdmin = useIsTenantAdmin();

    const canEdit = hasConnectionEditScope && isConnectionOwnerOrTenantAdmin;
    const canDelete = hasConnectionDeleteScope && isConnectionOwnerOrTenantAdmin;
    const canManageSharing =
        visibilityFeatureEnabled && canManageResourceSharing && connection.visibility !== 'ORGANIZATION';

    const queryClient = useQueryClient();
    const {openProjectWorkflow} = useOpenInProject();

    const {data: connectionWorkflowsData, isLoading: connectionWorkflowsLoading} = useConnectionWorkflowsQuery(
        {connectionId: String(connection.id)},
        {enabled: workflowsOpen && connection.id !== undefined}
    );

    const invalidateConnections = () => {
        queryClient.invalidateQueries({queryKey: ConnectionKeys.connections});
        queryClient.invalidateQueries({queryKey: ComponentDefinitionKeys.componentDefinitions});
    };

    const invalidateConnectionGrants = () => {
        queryClient.invalidateQueries({queryKey: ['ConnectionGrants']});
    };

    const setConnectionVisibilityMutation = useSetConnectionVisibilityMutation({
        onSuccess: () => {
            invalidateConnections();
            invalidateConnectionGrants();
        },
    });

    const grantConnectionAccessMutation = useGrantConnectionAccessMutation({
        onSuccess: () => invalidateConnectionGrants(),
    });

    const revokeConnectionAccessMutation = useRevokeConnectionAccessMutation({
        onSuccess: () => invalidateConnectionGrants(),
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
            void queryClient.invalidateQueries({
                queryKey: CONNECTION_WORKFLOWS_QUERY_KEY,
            });

            setShowDisconnectDialog(false);

            toast(`"${connection.name}" was successfully disconnected from all workflows.`);
        },
    });

    const disconnectConnectionFromWorkflowMutation = useDisconnectConnectionFromWorkflowMutation({
        onSettled: () => {
            setConnectionWorkflowToDisconnect(undefined);
        },
        onSuccess: () => {
            void queryClient.invalidateQueries({
                queryKey: ConnectionKeys.connections,
            });
            void queryClient.invalidateQueries({
                queryKey: CONNECTION_WORKFLOWS_QUERY_KEY,
            });

            toast(
                `"${connection.name}" was disconnected from the "${connectionWorkflowToDisconnect?.workflowName}" workflow.`
            );
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

    // Only a withheld connection can have a meaningful audience, so the two lookups the picker needs are skipped for
    // the workspace-visible majority until the sharing menu is opened.
    const isWithheld = connection.visibility === 'PRIVATE';

    const connectionGrantsQuery = useConnectionGrantsQuery(
        {connectionId: String(connection.id), workspaceId: String(currentWorkspaceId)},
        {enabled: canManageSharing && (isWithheld || showVisibilityMenu) && !!connection.id && !!currentWorkspaceId}
    );

    const workspaceUsersQuery = useWorkspaceUsersQuery(
        {workspaceId: String(currentWorkspaceId)},
        {enabled: canManageSharing && (isWithheld || showVisibilityMenu) && !!currentWorkspaceId}
    );

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

    const connectionWorkflows = useMemo<ConnectionWorkflowI[] | undefined>(
        () =>
            connectionWorkflowsData?.connectionWorkflows.map((connectionWorkflow) => ({
                id: connectionWorkflow.id,
                onOpenClick: connectionWorkflow.workflowUuid
                    ? () =>
                          void openProjectWorkflow(
                              Number(connectionWorkflow.projectId),
                              connectionWorkflow.workflowUuid ?? undefined
                          )
                    : undefined,
                openLabel: 'Open in project',
                parentName: connectionWorkflow.projectName,
                usage: connectionWorkflow.usage,
                version: connectionWorkflow.projectVersion,
                workflowName: connectionWorkflow.workflowName,
            })),
        [connectionWorkflowsData, openProjectWorkflow]
    );

    const canDisconnect = isTenantAdmin && connection.active === true;
    const canReassign =
        visibilityFeatureEnabled &&
        isTenantAdmin &&
        connection.status === 'PENDING_REASSIGNMENT' &&
        !!connection.createdBy;

    const hasSecondaryActions = canDisconnect || canDelete || canReassign;

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

    const handleDisconnectFromWorkflowClick = () => {
        if (connection.id && connectionWorkflowToDisconnect) {
            disconnectConnectionFromWorkflowMutation.mutate({
                connectionId: String(connection.id),
                id: connectionWorkflowToDisconnect.id,
                usage:
                    connectionWorkflowToDisconnect.usage === 'DEPLOYMENT'
                        ? ConnectionWorkflowUsage.Deployment
                        : ConnectionWorkflowUsage.Editor,
            });
        }
    };

    const grantedUserIds = (connectionGrantsQuery.data?.connectionGrants ?? []).map(Number);

    const workspaceMembers = (workspaceUsersQuery.data?.workspaceUsers ?? []).map((workspaceUser) => ({
        label: workspaceUser.user?.email ?? `User ${workspaceUser.userId}`,
        userId: Number(workspaceUser.userId),
    }));

    const renderVisibilityPicker = () => {
        if (!canManageSharing || !connection.id || !currentWorkspaceId) {
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
        <ConnectionListItemLayout
            active={connection.active}
            badges={
                visibilityFeatureEnabled &&
                (canManageSharing && connection.id && currentWorkspaceId ? (
                    <DropdownMenu onOpenChange={setShowVisibilityMenu}>
                        <DropdownMenuTrigger asChild>
                            <button
                                aria-label="Change visibility"
                                className="cursor-pointer rounded-sm hover:bg-gray-100"
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
                    <ResourceVisibilityBadge visibility={connection.visibility || 'WORKSPACE'} />
                ))
            }
            createdDate={connection.createdDate}
            credentialStatus={connection.credentialStatus}
            expandLabel="Workflows"
            expandableContent={
                <ConnectionWorkflowList
                    connectionWorkflows={connectionWorkflows}
                    isLoading={connectionWorkflowsLoading}
                    onDisconnectClick={isTenantAdmin ? setConnectionWorkflowToDisconnect : undefined}
                />
            }
            icon={componentDefinition?.icon}
            id={connection.id}
            menuItems={
                hasSecondaryActions && (
                    <>
                        {canReassign && (
                            <DropdownMenuItem
                                icon={<UserCogIcon />}
                                label="Reassign owner"
                                onClick={() => setShowReassignmentDialog(true)}
                            />
                        )}

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
            onOpenChange={setWorkflowsOpen}
            open={workflowsOpen}
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

            <AlertDialog
                confirmIcon={<Link2OffIcon />}
                confirmLabel="Disconnect"
                description={
                    connectionWorkflowToDisconnect?.usage === 'DEPLOYMENT'
                        ? 'The deployed workflow keeps running without this connection, so its steps that need it will fail until you pick another one.'
                        : 'The workflow editor will no longer use this connection when testing the workflow.'
                }
                isPending={disconnectConnectionFromWorkflowMutation.isPending}
                onCancel={() => setConnectionWorkflowToDisconnect(undefined)}
                onConfirm={handleDisconnectFromWorkflowClick}
                open={!!connectionWorkflowToDisconnect}
                title={`Disconnect ${connection.name} from ${connectionWorkflowToDisconnect?.workflowName}?`}
            />

            {canReassign && connection.createdBy && currentWorkspaceId && (
                <ConnectionReassignmentDialog
                    onClose={() => setShowReassignmentDialog(false)}
                    open={showReassignmentDialog}
                    userLogin={connection.createdBy}
                    workspaceId={currentWorkspaceId}
                />
            )}

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
                        canEdit ? useUpdateConnectionCredentialsMutation : undefined
                    }
                    useUpdateConnectionMutation={useUpdateConnectionMutation}
                />
            )}
        </ConnectionListItemLayout>
    );
});

ConnectionListItem.displayName = 'ConnectionListItem';

export default ConnectionListItem;
