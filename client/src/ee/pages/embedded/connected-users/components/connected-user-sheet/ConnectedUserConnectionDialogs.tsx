import AlertDialog from '@/components/AlertDialog';
import ConnectedUserConnectionCredentialsDialog from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserConnectionCredentialsDialog';
import ConnectedUserIntegrationCredentialsDialog from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserIntegrationCredentialsDialog';
import {Connection} from '@/ee/shared/middleware/embedded/configuration';
import {ConnectedUserIntegrationInstance} from '@/ee/shared/middleware/embedded/connected-user';
import {useDeleteConnectedUserConnectionMutation} from '@/ee/shared/mutations/embedded/connections.mutations';
import {ConnectionKeys} from '@/ee/shared/queries/embedded/connections.queries';
import {useQueryClient} from '@tanstack/react-query';

interface ConnectedUserConnectionDialogsProps {
    connectedUserId: number;
    connection: Connection;
    deletable: boolean;
    onCredentialsDialogClose: () => void;
    onDeleteDialogClose: () => void;
    provisionedIntegrationInstance?: ConnectedUserIntegrationInstance;
    showCredentialsDialog: boolean;
    showDeleteDialog: boolean;
}

const ConnectedUserConnectionDialogs = ({
    connectedUserId,
    connection,
    deletable,
    onCredentialsDialogClose,
    onDeleteDialogClose,
    provisionedIntegrationInstance,
    showCredentialsDialog,
    showDeleteDialog,
}: ConnectedUserConnectionDialogsProps) => {
    const queryClient = useQueryClient();

    const deleteConnectedUserConnectionMutation = useDeleteConnectedUserConnectionMutation(connectedUserId, {
        onError: onDeleteDialogClose,
        onSuccess: () => {
            void queryClient.invalidateQueries({
                queryKey: ConnectionKeys.connections,
            });

            onDeleteDialogClose();
        },
    });

    return (
        <>
            {deletable && (
                <AlertDialog
                    description={`This action cannot be undone. This will permanently delete the connection ${connection.name}.`}
                    isPending={deleteConnectedUserConnectionMutation.isPending}
                    onCancel={onDeleteDialogClose}
                    onConfirm={() => deleteConnectedUserConnectionMutation.mutate(connection.id!)}
                    open={showDeleteDialog}
                />
            )}

            {showCredentialsDialog && provisionedIntegrationInstance && (
                <ConnectedUserIntegrationCredentialsDialog
                    connectedUserId={connectedUserId}
                    integrationInstance={provisionedIntegrationInstance}
                    onClose={onCredentialsDialogClose}
                />
            )}

            {showCredentialsDialog && !provisionedIntegrationInstance && (
                <ConnectedUserConnectionCredentialsDialog
                    connectedUserId={connectedUserId}
                    connection={connection}
                    onClose={onCredentialsDialogClose}
                />
            )}
        </>
    );
};

export default ConnectedUserConnectionDialogs;
