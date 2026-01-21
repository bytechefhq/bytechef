import {useEditUserDialogStore} from '@/pages/settings/platform/users/stores/useEditUserDialogStore';
import {useAuthoritiesQuery, useUpdateUserMutation, useUsersQuery} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect, useMemo} from 'react';

interface UseEditUserDialogI {
    authorities: string[];
    editRole: string | null;
    editUser: {email?: string | null; login?: string | null; authorities?: (string | null)[] | null} | null;
    handleClose: () => void;
    handleOpen: (login: string) => void;
    handleOpenChange: (open: boolean) => void;
    handleRoleChange: (role: string) => void;
    handleUpdate: () => void;
    open: boolean;
    updateDisabled: boolean;
}

export default function useEditUserDialog(): UseEditUserDialogI {
    const {clearLoginToEdit, editRole, loginToEdit, setEditRole, setLoginToEdit} = useEditUserDialogStore();

    const {data: usersData} = useUsersQuery({});
    const {data: authoritiesData} = useAuthoritiesQuery({});

    const queryClient = useQueryClient();

    const updateUserMutation = useUpdateUserMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['users']});
            clearLoginToEdit();
        },
    });

    const authorities = useMemo(() => authoritiesData?.authorities ?? [], [authoritiesData]);
    const users = useMemo(() => usersData?.users?.content ?? [], [usersData]);
    const updateDisabled = !loginToEdit || !editRole;

    const editUser = users.find((user) => user?.login === loginToEdit) || null;

    useEffect(() => {
        if (loginToEdit) {
            const current = users.find((user) => user?.login === loginToEdit);
            const currentRole = current?.authorities?.[0] ?? authorities[0] ?? null;

            if (currentRole) {
                setEditRole(currentRole);
            }
        }
    }, [loginToEdit, users, authorities]);

    const handleClose = () => {
        clearLoginToEdit();
    };

    const handleOpen = (login: string) => {
        setLoginToEdit(login);
    };

    const handleRoleChange = (role: string) => {
        setEditRole(role);
    };

    const handleUpdate = () => {
        if (loginToEdit && editRole) {
            updateUserMutation.mutate({
                login: loginToEdit,
                role: editRole,
            });
        }
    };

    const handleOpenChange = (open: boolean) => {
        if (!open) {
            handleClose();
        }
    };

    return {
        authorities,
        editRole,
        editUser,
        handleClose,
        handleOpen,
        handleOpenChange,
        handleRoleChange,
        handleUpdate,
        open: loginToEdit !== null,
        updateDisabled,
    };
}
