import AlertDialog from '@/components/AlertDialog';
import Badge from '@/components/Badge/Badge';
import {DropdownMenuItem} from '@/components/DropdownMenu/DropdownMenu';
import ConnectedUserConnectionCredentialsDialog from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserConnectionCredentialsDialog';
import ConnectedUserIntegrationCredentialsDialog from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserIntegrationCredentialsDialog';
import {getConnectedUserConnectionPermissions} from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/connectedUserConnectionPermissions';
import ConnectionOriginBadge from '@/ee/pages/embedded/connections/components/connection-list/ConnectionOriginBadge';
import {Connection, ConnectionOriginEnum} from '@/ee/shared/middleware/embedded/configuration';
import {ConnectedUserIntegrationInstance} from '@/ee/shared/middleware/embedded/connected-user';
import {useDeleteConnectedUserConnectionMutation} from '@/ee/shared/mutations/embedded/connections.mutations';
import {ConnectionKeys} from '@/ee/shared/queries/embedded/connections.queries';
import ConnectionListItemLayout from '@/shared/components/connection/ConnectionListItemLayout';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {useQueryClient} from '@tanstack/react-query';
import {Trash2Icon} from 'lucide-react';
import {useState} from 'react';

interface ConnectedUserConnectionListItemProps {
    componentDefinitions: ComponentDefinitionBasic[];
    connectedUserId: number;
    connection: Connection;
    provisionedIntegrationInstance?: ConnectedUserIntegrationInstance;
}

const ConnectedUserConnectionListItem = ({
    componentDefinitions,
    connectedUserId,
    connection,
    provisionedIntegrationInstance,
}: ConnectedUserConnectionListItemProps) => {
    const [showCredentialsDialog, setShowCredentialsDialog] = useState(false);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);

    const queryClient = useQueryClient();

    const deleteConnectedUserConnectionMutation = useDeleteConnectedUserConnectionMutation(connectedUserId, {
        onError: () => setShowDeleteDialog(false),
        onSuccess: () => {
            void queryClient.invalidateQueries({
                queryKey: ConnectionKeys.connections,
            });

            setShowDeleteDialog(false);
        },
    });

    const {credentialsEditable, deletable} = getConnectedUserConnectionPermissions(
        connection,
        provisionedIntegrationInstance
    );

    const provisionedDeletable = deletable && connection.origin === ConnectionOriginEnum.Provisioned;

    const componentDefinition = componentDefinitions.find(
        (componentDefinition) => componentDefinition.name === connection.componentName
    );

    return (
        <ConnectionListItemLayout
            active={connection.active}
            badges={<ConnectionOriginBadge origin={connection.origin} />}
            createdDate={connection.createdDate}
            credentialStatus={connection.credentialStatus}
            icon={componentDefinition?.icon}
            id={connection.id}
            menuItems={
                provisionedDeletable && (
                    <DropdownMenuItem
                        icon={<Trash2Icon />}
                        label="Delete"
                        onClick={() => setShowDeleteDialog(true)}
                        variant="destructive"
                    />
                )
            }
            name={connection.name}
            onEditClick={credentialsEditable ? () => setShowCredentialsDialog(true) : undefined}
            tagList={
                !!connection.tags?.length && (
                    <div className="flex flex-wrap items-center gap-1">
                        {connection.tags?.map((tag) => (
                            <Badge key={tag.id} label={tag.name} styleType="secondary-outline" weight="semibold" />
                        ))}
                    </div>
                )
            }
        >
            {provisionedDeletable && (
                <AlertDialog
                    description={`This action cannot be undone. This will permanently delete the connection ${connection.name}.`}
                    isPending={deleteConnectedUserConnectionMutation.isPending}
                    onCancel={() => setShowDeleteDialog(false)}
                    onConfirm={() => deleteConnectedUserConnectionMutation.mutate(connection.id!)}
                    open={showDeleteDialog}
                />
            )}

            {showCredentialsDialog && provisionedIntegrationInstance && (
                <ConnectedUserIntegrationCredentialsDialog
                    connectedUserId={connectedUserId}
                    integrationInstance={provisionedIntegrationInstance}
                    onClose={() => setShowCredentialsDialog(false)}
                />
            )}

            {showCredentialsDialog && !provisionedIntegrationInstance && (
                <ConnectedUserConnectionCredentialsDialog
                    connectedUserId={connectedUserId}
                    connection={connection}
                    onClose={() => setShowCredentialsDialog(false)}
                />
            )}
        </ConnectionListItemLayout>
    );
};

export default ConnectedUserConnectionListItem;
