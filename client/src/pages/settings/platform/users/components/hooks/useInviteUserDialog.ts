import {useInviteUserDialogStore} from '@/pages/settings/platform/users/stores/useInviteUserDialogStore';
import {isValidPassword} from '@/pages/settings/platform/users/util/password-utils';
import {useAuthoritiesQuery, useInviteUserMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect, useMemo} from 'react';

interface UseInviteUserDialogI {
    authorities: string[];
    handleClose: () => void;
    handleEmailChange: (email: string) => void;
    handleInvite: () => void;
    handleOpen: () => void;
    handleRegeneratePassword: () => void;
    handleRoleChange: (role: string) => void;
    inviteDisabled: boolean;
    inviteEmail: string;
    invitePassword: string;
    inviteRole: string | null;
    open: boolean;
}

export default function useInviteUserDialog(): UseInviteUserDialogI {
    const {
        handleClose,
        handleEmailChange,
        handleOpen: storeHandleOpen,
        handleRegeneratePassword,
        handleRoleChange,
        inviteEmail,
        invitePassword,
        inviteRole,
        open,
    } = useInviteUserDialogStore();

    const {data: authoritiesData} = useAuthoritiesQuery({});

    const queryClient = useQueryClient();

    const inviteUserMutation = useInviteUserMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['users']});
            handleClose();
        },
    });

    const authorities = useMemo(() => authoritiesData?.authorities ?? [], [authoritiesData]);
    const inviteDisabled = !inviteEmail || !inviteRole || !isValidPassword(invitePassword);

    useEffect(() => {
        if (open && !inviteRole && authorities.length > 0) {
            handleRoleChange(authorities[0]);
        }
    }, [open, inviteRole, authorities, handleRoleChange]);

    const handleOpen = () => {
        storeHandleOpen();
    };

    const handleInvite = () => {
        if (inviteEmail && inviteRole) {
            inviteUserMutation.mutate({
                email: inviteEmail,
                password: invitePassword,
                role: inviteRole,
            });
        }
    };

    return {
        authorities,
        handleClose,
        handleEmailChange,
        handleInvite,
        handleOpen,
        handleRegeneratePassword,
        handleRoleChange,
        inviteDisabled,
        inviteEmail,
        invitePassword,
        inviteRole,
        open,
    };
}
