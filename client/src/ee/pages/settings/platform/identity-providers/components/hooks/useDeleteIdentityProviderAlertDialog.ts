import {useDeleteIdentityProviderDialogStore} from '@/ee/pages/settings/platform/identity-providers/stores/useDeleteIdentityProviderDialogStore';
import {useDeleteIdentityProviderMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';

interface UseDeleteIdentityProviderAlertDialogI {
    handleClose: () => void;
    handleDelete: () => void;
    handleOpen: (id: string | null) => void;
    isPending: boolean;
    open: boolean;
}

export default function useDeleteIdentityProviderAlertDialog(): UseDeleteIdentityProviderAlertDialogI {
    const {clearIdToDelete, idToDelete, setIdToDelete} = useDeleteIdentityProviderDialogStore();

    const queryClient = useQueryClient();

    const deleteIdentityProviderMutation = useDeleteIdentityProviderMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['identityProviders']});
            clearIdToDelete();
        },
    });

    const handleClose = () => {
        clearIdToDelete();
    };

    const handleOpen = (id: string | null) => {
        setIdToDelete(id);
    };

    const handleDelete = () => {
        if (idToDelete) {
            deleteIdentityProviderMutation.mutate({id: idToDelete});
        }
    };

    return {
        handleClose,
        handleDelete,
        handleOpen,
        isPending: deleteIdentityProviderMutation.isPending,
        open: idToDelete !== null,
    };
}
