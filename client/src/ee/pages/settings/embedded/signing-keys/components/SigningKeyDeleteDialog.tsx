import AlertDialog from '@/components/AlertDialog';
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
        <AlertDialog
            description="This action cannot be undone. This will permanently delete the signing key."
            isPending={deleteSigningKeyMutation.isPending}
            onCancel={onClose}
            onConfirm={handleClick}
            open
        />
    );
};

export default SigningKeyDeleteDialog;
