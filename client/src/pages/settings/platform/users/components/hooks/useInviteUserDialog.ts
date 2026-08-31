import {useInviteUserDialogStore} from '@/pages/settings/platform/users/stores/useInviteUserDialogStore';
import {isValidPassword} from '@/pages/settings/platform/users/util/password-utils';
import {AUTHORITIES} from '@/shared/constants';
import {useAuthoritiesQuery, useInviteUserMutation} from '@/shared/middleware/graphql';
import {EditionType, useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
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
    roleSelectVisible: boolean;
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

    const application = useApplicationInfoStore((state) => state.application);

    const {data: authoritiesData} = useAuthoritiesQuery({});

    const queryClient = useQueryClient();

    const inviteUserMutation = useInviteUserMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['users']});
            reset();
        },
    });

    const authorities = useMemo(() => authoritiesData?.authorities ?? [], [authoritiesData]);
    const ceEdition = application?.edition === EditionType.CE;
    const roleSelectVisible = !ceEdition;
    const inviteDisabled = !inviteEmail || (roleSelectVisible && !inviteRole) || !isValidPassword(invitePassword);

    useEffect(() => {
        if (roleSelectVisible && open && !inviteRole && authorities.length > 0) {
            setInviteRole(authorities[0]);
        }
    }, [roleSelectVisible, open, inviteRole, authorities, setInviteRole]);

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
        const role = ceEdition ? AUTHORITIES.ADMIN : inviteRole;

        if (inviteEmail && role) {
            inviteUserMutation.mutate({
                email: inviteEmail,
                password: invitePassword,
                role,
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
        roleSelectVisible,
    };
}
