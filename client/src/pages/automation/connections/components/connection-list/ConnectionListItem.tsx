import AlertDialog from '@/components/AlertDialog';
import {DropdownMenuItem} from '@/components/DropdownMenu/DropdownMenu';
import TagList from '@/shared/components/TagList';
import ConnectionDialog from '@/shared/components/connection/ConnectionDialog';
import ConnectionListItemLayout from '@/shared/components/connection/ConnectionListItemLayout';
import {Connection, Tag} from '@/shared/middleware/automation/configuration';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {useUpdateConnectionTagsMutation} from '@/shared/mutations/automation/connectionTags.mutations';
import {
    useDeleteConnectionMutation,
    useDisconnectConnectionMutation,
    useUpdateConnectionMutation,
} from '@/shared/mutations/automation/connections.mutations';
import {ConnectionKeys, useGetConnectionTagsQuery} from '@/shared/queries/automation/connections.queries';
import {ComponentDefinitionKeys} from '@/shared/queries/platform/componentDefinitions.queries';
import {useQueryClient} from '@tanstack/react-query';
import {Link2OffIcon, Trash2Icon} from 'lucide-react';
import {memo, useMemo, useState} from 'react';
import {toast} from 'sonner';

interface ConnectionListItemProps {
    componentDefinitions: ComponentDefinitionBasic[];
    connection: Connection;
    remainingTags?: Tag[];
}

const ConnectionListItem = memo(({componentDefinitions, connection, remainingTags}: ConnectionListItemProps) => {
    const [showEditDialog, setShowEditDialog] = useState(false);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [showDisconnectDialog, setShowDisconnectDialog] = useState(false);

    const queryClient = useQueryClient();

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

    return (
        <ConnectionListItemLayout
            active={connection.active}
            createdDate={connection.createdDate}
            credentialStatus={connection.credentialStatus}
            icon={componentDefinition?.icon}
            id={connection.id}
            menuItems={
                <>
                    {connection.active === true && (
                        <DropdownMenuItem
                            icon={<Link2OffIcon />}
                            label="Disconnect from all"
                            onClick={() => setShowDisconnectDialog(true)}
                        />
                    )}

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
                </>
            }
            name={connection.name}
            onEditClick={() => setShowEditDialog(true)}
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
                    connectionTagsQueryKey={ConnectionKeys.connectionTags}
                    connectionsQueryKey={ConnectionKeys.connections}
                    onClose={() => setShowEditDialog(false)}
                    useGetConnectionTagsQuery={useGetConnectionTagsQuery}
                    useUpdateConnectionMutation={useUpdateConnectionMutation}
                />
            )}
        </ConnectionListItemLayout>
    );
});

ConnectionListItem.displayName = 'ConnectionListItem';

export default ConnectionListItem;
