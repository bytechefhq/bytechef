import AlertDialog from '@/components/AlertDialog';
import {DropdownMenuItem} from '@/components/DropdownMenu/DropdownMenu';
import useOpenInProject from '@/pages/automation/project-deployments/hooks/useOpenInProject';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import TagList from '@/shared/components/TagList';
import ConnectionDialog from '@/shared/components/connection/ConnectionDialog';
import ConnectionListItemLayout from '@/shared/components/connection/ConnectionListItemLayout';
import ConnectionWorkflowList, {type ConnectionWorkflowI} from '@/shared/components/connection/ConnectionWorkflowList';
import {useHasWorkspaceScope} from '@/shared/hooks/useHasWorkspaceScope';
import {useIsTenantAdmin} from '@/shared/hooks/useIsTenantAdmin';
import {Connection, Tag} from '@/shared/middleware/automation/configuration';
import {
    ConnectionWorkflowUsage,
    useConnectionWorkflowsQuery,
    useDisconnectConnectionFromWorkflowMutation,
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

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const currentUserLogin = useAuthenticationStore((state) => state.account?.login);

    const canEdit = useHasWorkspaceScope(currentWorkspaceId, 'CONNECTION_EDIT');
    const canDelete = useHasWorkspaceScope(currentWorkspaceId, 'CONNECTION_DELETE');
    const isTenantAdmin = useIsTenantAdmin();

    const queryClient = useQueryClient();
    const {openProjectWorkflow} = useOpenInProject();

    const {data: connectionWorkflowsData, isLoading: connectionWorkflowsLoading} = useConnectionWorkflowsQuery(
        {connectionId: String(connection.id)},
        {enabled: workflowsOpen && connection.id !== undefined}
    );

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

    return (
        <ConnectionListItemLayout
            active={connection.active}
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

            {showEditDialog && componentDefinitions && (
                <ConnectionDialog
                    componentDefinitions={componentDefinitions}
                    connection={connection}
                    connectionPropertiesEditable
                    connectionTagsQueryKey={ConnectionKeys.connectionTags}
                    connectionsQueryKey={ConnectionKeys.connections}
                    onClose={() => setShowEditDialog(false)}
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
