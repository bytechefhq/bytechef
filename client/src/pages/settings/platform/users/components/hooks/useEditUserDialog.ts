import {useAuthoritiesQuery, useUpdateUserMutation, useUsersQuery} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {Dispatch, SetStateAction, useMemo, useState} from 'react';

interface UseEditUserDialogI {
    authorities: string[];
    editRole: string | null;
    editUser: {email?: string | null; login?: string | null; authorities?: (string | null)[] | null} | null;
    handleEditUserDialogClose: () => void;
    handleEditUserDialogOpen: (login: string) => void;
    handleEditUserDialogUpdate: () => void;
    open: boolean;
    setEditRole: Dispatch<SetStateAction<string | null>>;
    updateDisabled: boolean;
}

export default function useEditUserDialog(): UseEditUserDialogI {
    const [editLogin, setEditLogin] = useState<string | null>(null);
    const [editRole, setEditRole] = useState<string | null>(null);

    const {data: usersData} = useUsersQuery({});
    const {data: authoritiesData} = useAuthoritiesQuery({});

    const queryClient = useQueryClient();

    const updateUserMutation = useUpdateUserMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['users']});
            setEditLogin(null);
        },
    });

    const authorities = useMemo(() => authoritiesData?.authorities ?? [], [authoritiesData]);
    const users = useMemo(() => usersData?.users?.content ?? [], [usersData]);
    const updateDisabled = !editLogin || !editRole;

    const editUser = users.find((user) => user?.login === editLogin) || null;

    const handleOpen = (login: string) => {
        setEditLogin(login);

        const current = users.find((user) => user?.login === login);

        const currentRole = current?.authorities?.[0] ?? null;

        setEditRole(currentRole ?? authorities[0] ?? null);
    };

    const handleClose = () => {
        setEditLogin(null);
    };

    const handleUpdate = () => {
        if (editLogin && editRole) {
            updateUserMutation.mutate({
                login: editLogin,
                role: editRole,
            });
        }
    };

    return {
        authorities,
        editRole,
        editUser,
        handleEditUserDialogClose: handleClose,
        handleEditUserDialogOpen: handleOpen,
        handleEditUserDialogUpdate: handleUpdate,
        open: !!editLogin,
        setEditRole,
        updateDisabled,
    };
}
