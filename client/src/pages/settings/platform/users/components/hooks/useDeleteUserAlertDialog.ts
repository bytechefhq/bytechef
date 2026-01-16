import {useDeleteUserMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';

interface UseDeleteUserAlertDialogI {
    deleteLogin: string | null;
    handleDeleteUserAlertDialogClose: () => void;
    handleDeleteUserAlertDialogDelete: () => void;
    handleDeleteUserAlertDialogOpen: (login: string | null) => void;
    open: boolean;
}

export default function useDeleteUserAlertDialog(): UseDeleteUserAlertDialogI {
    const [deleteLogin, setDeleteLogin] = useState<string | null>(null);

    const queryClient = useQueryClient();

    const deleteUserMutation = useDeleteUserMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['users']});
            setDeleteLogin(null);
        },
    });

    const handleClose = () => {
        setDeleteLogin(null);
    };

    const handleDelete = () => {
        if (deleteLogin) {
            deleteUserMutation.mutate({login: deleteLogin});
        }
    };

    const handleOpen = (login: string | null) => {
        setDeleteLogin(login);
    };

    return {
        deleteLogin,
        handleDeleteUserAlertDialogClose: handleClose,
        handleDeleteUserAlertDialogDelete: handleDelete,
        handleDeleteUserAlertDialogOpen: handleOpen,
        open: !!deleteLogin,
    };
}
