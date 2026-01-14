import {generatePassword, isValidPassword} from '@/pages/settings/platform/users/util/password-utils';
import {useAuthoritiesQuery, useInviteUserMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useMemo, useState} from 'react';

export default function useInviteUserDialog() {
    const [inviteOpen, setInviteOpen] = useState(false);
    const [inviteEmail, setInviteEmail] = useState('');
    const [invitePassword, setInvitePassword] = useState(generatePassword());
    const [inviteRole, setInviteRole] = useState<string | null>(null);

    const {data: authoritiesData} = useAuthoritiesQuery({});

    const queryClient = useQueryClient();

    const inviteUserMutation = useInviteUserMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['users']});
            setInviteOpen(false);
            setInviteEmail('');
            setInvitePassword(generatePassword());
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
        setInvitePassword(generatePassword());
    };

    return {
        authorities,
        handleClose,
        handleInvite,
        handleOpen,
        handleRegeneratePassword,
        inviteDisabled,
        inviteEmail,
        invitePassword,
        inviteRole,
        open: inviteOpen,
        setInviteEmail,
        setInviteRole,
    };
}
