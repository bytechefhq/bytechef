import AlertDialog from '@/components/AlertDialog';
import {useDeleteConnectedUserMutation} from '@/ee/shared/mutations/embedded/connectedUsers.mutations';
import {ConnectedUserKeys} from '@/ee/shared/queries/embedded/connectedUsers.queries';
import {useQueryClient} from '@tanstack/react-query';

const ConnectedUserDeleteDialog = ({connectedUserId, onClose}: {connectedUserId: number; onClose: () => void}) => {
    const queryClient = useQueryClient();

    const deleteConnectedUserMutation = useDeleteConnectedUserMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ConnectedUserKeys.connectedUsers,
            });

            onClose();
        },
    });

    const handleClick = () => {
        deleteConnectedUserMutation.mutate({id: connectedUserId});
    };

    return (
        <AlertDialog
            description="This action cannot be undone. This will permanently delete the connected user."
            onCancel={onClose}
            onConfirm={handleClick}
            open
        />
    );
};

export default ConnectedUserDeleteDialog;
