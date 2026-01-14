import {useAuthoritiesQuery, useUpdateUserMutation, useUsersQuery} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useMemo, useState} from 'react';

export default function useEditUserDialog() {
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

        const current = users.find((u) => u?.login === login);

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
        handleClose,
        handleOpen,
        handleUpdate,
        open: !!editLogin,
        setEditRole,
        updateDisabled,
    };
}
