import {useDeleteUserMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';

export default function useDeleteUserAlertDialog() {
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
        handleClose,
        handleDelete,
        handleOpen,
        open: !!deleteLogin,
    };
}
