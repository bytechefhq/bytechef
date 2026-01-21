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
    handleRoleChange: (role: string) => void;
    handleUpdate: () => void;
    open: boolean;
    updateDisabled: boolean;
}

export default function useEditUserDialog(): UseEditUserDialogI {
    const {editRole, handleClose, handleOpen, handleRoleChange, loginToEdit} = useEditUserDialogStore();

    const {data: usersData} = useUsersQuery({});
    const {data: authoritiesData} = useAuthoritiesQuery({});

    const queryClient = useQueryClient();

    const updateUserMutation = useUpdateUserMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['users']});
            handleClose();
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
                handleRoleChange(currentRole);
            }
        }
    }, [loginToEdit, users, authorities, handleRoleChange]);

    const handleUpdate = () => {
        if (loginToEdit && editRole) {
            updateUserMutation.mutate({
                login: loginToEdit,
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
        handleRoleChange,
        handleUpdate,
        open: loginToEdit !== null,
        updateDisabled,
    };
}
