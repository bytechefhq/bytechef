import {useInviteUserDialogStore} from '@/pages/settings/platform/users/stores/useInviteUserDialogStore';
import {AUTHORITIES} from '@/shared/constants';
import useCeEdition from '@/shared/edition/useCeEdition';
import {WorkspaceAssignmentInput, useAuthoritiesQuery, useInviteUserMutation} from '@/shared/middleware/graphql';
import {useGetUserWorkspacesQuery} from '@/shared/queries/automation/workspaces.queries';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect, useMemo} from 'react';
import {useShallow} from 'zustand/react/shallow';

const DEFAULT_WORKSPACE_ROLE = 'EDITOR';

interface WorkspaceOptionI {
    id: string;
    name: string;
}

interface UseInviteUserDialogI {
    authorities: string[];
    handleClose: () => void;
    handleEmailChange: (email: string) => void;
    handleInvite: () => void;
    handleOpen: () => void;
    handleOpenChange: (open: boolean) => void;
    handleRoleChange: (role: string) => void;
    handleWorkspaceRoleChange: (workspaceId: string, roleName: string) => void;
    handleWorkspaceToggle: (workspaceId: string) => void;
    inviteDisabled: boolean;
    inviteEmail: string;
    inviteRole: string | null;
    inviteWorkspaces: WorkspaceAssignmentInput[];
    open: boolean;
    roleSelectVisible: boolean;
    workspaces: WorkspaceOptionI[];
}

export default function useInviteUserDialog(): UseInviteUserDialogI {
    const {
        inviteEmail,
        inviteRole,
        inviteWorkspaces,
        open,
        reset,
        setInviteEmail,
        setInviteRole,
        setInviteWorkspaceRole,
        setOpen,
        toggleInviteWorkspace,
    } = useInviteUserDialogStore();

    const {account} = useAuthenticationStore(useShallow((state) => ({account: state.account})));

    const ceEdition = useCeEdition();

    const {data: authoritiesData} = useAuthoritiesQuery({}, {enabled: !ceEdition});

    // Only tenant admins reach this dialog, and getUserWorkspaces skips the membership filter for them — so this
    // returns every workspace in the tenant rather than just the ones the admin happens to belong to.
    const {data: workspacesData} = useGetUserWorkspacesQuery(account?.id ?? 0, open && account?.id != null);

    const queryClient = useQueryClient();

    const inviteUserMutation = useInviteUserMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['users']});
            reset();
        },
    });

    const authorities = useMemo(() => authoritiesData?.authorities ?? [], [authoritiesData]);
    const roleSelectVisible = !ceEdition;

    const workspaces = useMemo(
        () =>
            (workspacesData ?? [])
                .filter((workspace) => workspace?.id != null)
                .map((workspace) => ({
                    id: String(workspace!.id),
                    name: workspace!.name ?? '',
                })),
        [workspacesData]
    );

    // Workspaces are optional: an invite with none provisions an account belonging to no workspace, which is how a
    // second tenant admin is created.
    const inviteDisabled = !inviteEmail || (roleSelectVisible && !inviteRole);

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

    const handleWorkspaceToggle = (workspaceId: string) => {
        toggleInviteWorkspace(workspaceId, DEFAULT_WORKSPACE_ROLE);
    };

    const handleWorkspaceRoleChange = (workspaceId: string, roleName: string) => {
        setInviteWorkspaceRole(workspaceId, roleName);
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
                role,
                workspaces: inviteWorkspaces,
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
        handleRoleChange,
        handleWorkspaceRoleChange,
        handleWorkspaceToggle,
        inviteDisabled,
        inviteEmail,
        inviteRole,
        inviteWorkspaces,
        open,
        roleSelectVisible,
        workspaces,
    };
}
