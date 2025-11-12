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
import {useDeleteSigningKeyMutation} from '@/ee/shared/mutations/embedded/signingKeys.mutations';
import {SigningKeyKeys} from '@/ee/shared/queries/embedded/signingKeys.queries';
import {useQueryClient} from '@tanstack/react-query';

const SigningKeyDeleteDialog = ({apiKeyId, onClose}: {apiKeyId: number; onClose: () => void}) => {
    const queryClient = useQueryClient();

    const deleteSigningKeyMutation = useDeleteSigningKeyMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: SigningKeyKeys.signingKeys,
            });

            onClose();
        },
    });

    const handleClick = () => {
        deleteSigningKeyMutation.mutate(apiKeyId);
    };

    return (
        <AlertDialog onOpenChange={onClose} open={true}>
            <AlertDialogContent>
                <AlertDialogHeader>
                    <AlertDialogTitle>Are you absolutely sure?</AlertDialogTitle>

                    <AlertDialogDescription>
                        This action cannot be undone. This will permanently delete the signing key.
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

export default SigningKeyDeleteDialog;
