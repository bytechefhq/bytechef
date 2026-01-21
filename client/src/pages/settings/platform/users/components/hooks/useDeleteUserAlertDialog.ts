import {useDeleteUserDialogStore} from '@/pages/settings/platform/users/stores/useDeleteUserDialogStore';
import {useDeleteUserMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';

interface UseDeleteUserAlertDialogI {
    handleClose: () => void;
    handleDelete: () => void;
    handleOpen: (login: string | null) => void;
    handleOpenChange: (open: boolean) => void;
    open: boolean;
}

export default function useDeleteUserAlertDialog(): UseDeleteUserAlertDialogI {
    const {clearLoginToDelete, loginToDelete, setLoginToDelete} = useDeleteUserDialogStore();

    const queryClient = useQueryClient();

    const deleteUserMutation = useDeleteUserMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['users']});
            clearLoginToDelete();
        },
    });

    const handleClose = () => {
        clearLoginToDelete();
    };

    const handleOpen = (login: string | null) => {
        setLoginToDelete(login);
    };

    const handleDelete = () => {
        if (loginToDelete) {
            deleteUserMutation.mutate({login: loginToDelete});
        }
    };

    const handleOpenChange = (open: boolean) => {
        if (!open) {
            handleClose();
        }
    };

    return {
        handleClose,
        handleDelete,
        handleOpen,
        handleOpenChange,
        open: loginToDelete !== null,
    };
}
