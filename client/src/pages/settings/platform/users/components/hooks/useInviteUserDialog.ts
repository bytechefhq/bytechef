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
    handleOpenChange: (open: boolean) => void;
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
        inviteEmail,
        invitePassword,
        inviteRole,
        open,
        regeneratePassword,
        reset,
        setInviteEmail,
        setInviteRole,
        setOpen,
    } = useInviteUserDialogStore();

    const {data: authoritiesData} = useAuthoritiesQuery({});

    const queryClient = useQueryClient();

    const inviteUserMutation = useInviteUserMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['users']});
            reset();
        },
    });

    const authorities = useMemo(() => authoritiesData?.authorities ?? [], [authoritiesData]);
    const inviteDisabled = !inviteEmail || !inviteRole || !isValidPassword(invitePassword);

    useEffect(() => {
        if (open && !inviteRole && authorities.length > 0) {
            setInviteRole(authorities[0]);
        }
    }, [open, inviteRole, authorities]);

    const handleClose = () => {
        reset();
    };

    const handleOpen = () => {
        setOpen();
    };

    const handleEmailChange = (email: string) => {
        setInviteEmail(email);
    };

    const handleRoleChange = (role: string) => {
        setInviteRole(role);
    };

    const handleRegeneratePassword = () => {
        regeneratePassword();
    };

    const handleOpenChange = (open: boolean) => {
        if (!open) {
            handleClose();
        }
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
        handleOpenChange,
        handleRegeneratePassword,
        handleRoleChange,
        inviteDisabled,
        inviteEmail,
        invitePassword,
        inviteRole,
        open,
    };
}
