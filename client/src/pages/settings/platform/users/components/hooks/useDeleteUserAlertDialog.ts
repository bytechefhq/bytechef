import {useDeleteUserDialogStore} from '@/pages/settings/platform/users/stores/useDeleteUserDialogStore';
import {useDeleteUserMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';

interface UseDeleteUserAlertDialogI {
    handleClose: () => void;
    handleDelete: () => void;
    handleOpen: (login: string | null) => void;
    open: boolean;
}

export default function useDeleteUserAlertDialog(): UseDeleteUserAlertDialogI {
    const {handleClose, handleOpen, loginToDelete} = useDeleteUserDialogStore();

    const queryClient = useQueryClient();

    const deleteUserMutation = useDeleteUserMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['users']});
            handleClose();
        },
    });

    const handleDelete = () => {
        if (loginToDelete) {
            deleteUserMutation.mutate({login: loginToDelete});
        }
    };

    return {
        handleClose,
        handleDelete,
        handleOpen,
        open: loginToDelete !== null,
    };
}
