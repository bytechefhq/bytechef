import {generatePassword, isValidPassword} from '@/pages/settings/platform/users/util/password-utils';
import {useAuthoritiesQuery, useInviteUserMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {Dispatch, SetStateAction, useMemo, useState} from 'react';

interface UseInviteUserDialogI {
    authorities: string[];
    handleInviteUserDialogClose: () => void;
    handleInviteUserDialogInvite: () => void;
    handleInviteUserDialogOpen: () => void;
    handleInviteUserDialogRegeneratePassword: () => void;
    inviteDisabled: boolean;
    inviteEmail: string;
    invitePassword: string;
    inviteRole: string | null;
    open: boolean;
    setInviteEmail: Dispatch<SetStateAction<string>>;
    setInviteRole: Dispatch<SetStateAction<string | null>>;
}

export default function useInviteUserDialog(): UseInviteUserDialogI {
    const [inviteOpen, setInviteOpen] = useState(false);
    const [inviteEmail, setInviteEmail] = useState('');
    const [invitePassword, setInvitePassword] = useState(generatePassword());
    const [inviteRole, setInviteRole] = useState<string | null>(null);

    const {data: authoritiesData} = useAuthoritiesQuery({});

    const queryClient = useQueryClient();

    const inviteUserMutation = useInviteUserMutation({
        onSuccess: () => {
            const newPassword = generatePassword();

            queryClient.invalidateQueries({queryKey: ['users']});
            setInviteOpen(false);
            setInviteEmail('');
            setInvitePassword(newPassword);
            setInviteRole(null);
        },
    });

    const authorities = useMemo(() => authoritiesData?.authorities ?? [], [authoritiesData]);
    const inviteDisabled = !inviteEmail || !inviteRole || !isValidPassword(invitePassword);

    const handleOpen = () => {
        setInviteOpen(true);
        setInviteRole((authorities && authorities.length > 0 && authorities[0]) || null);
    };

    const handleClose = () => {
        setInviteOpen(false);
    };

    const handleInvite = () => {
        inviteUserMutation.mutate({
            email: inviteEmail,
            password: invitePassword,
            role: inviteRole as string,
        });
    };

    const handleRegeneratePassword = () => {
        const newPassword = generatePassword();

        setInvitePassword(newPassword);
    };

    return {
        authorities,
        handleInviteUserDialogClose: handleClose,
        handleInviteUserDialogInvite: handleInvite,
        handleInviteUserDialogOpen: handleOpen,
        handleInviteUserDialogRegeneratePassword: handleRegeneratePassword,
        inviteDisabled,
        inviteEmail,
        invitePassword,
        inviteRole,
        open: inviteOpen,
        setInviteEmail,
        setInviteRole,
    };
}
