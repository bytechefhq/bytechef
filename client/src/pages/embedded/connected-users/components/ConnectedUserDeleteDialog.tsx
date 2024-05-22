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
import {useDeleteConnectedUserMutation} from '@/shared/mutations/embedded/connectedUsers.mutations';
import {ConnectedUserKeys} from '@/shared/queries/embedded/connectedUsers.queries';
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
        <AlertDialog onOpenChange={onClose} open={true}>
            <AlertDialogContent>
                <AlertDialogHeader>
                    <AlertDialogTitle>Are you absolutely sure?</AlertDialogTitle>

                    <AlertDialogDescription>
                        This action cannot be undone. This will permanently delete the project and workflows it
                        contains.
                    </AlertDialogDescription>
                </AlertDialogHeader>

                <AlertDialogFooter>
                    <AlertDialogCancel>Cancel</AlertDialogCancel>

                    <AlertDialogAction className="bg-destructive" onClick={handleClick}>
                        Delete
                    </AlertDialogAction>
                </AlertDialogFooter>
            </AlertDialogContent>
        </AlertDialog>
    );
};

export default ConnectedUserDeleteDialog;
