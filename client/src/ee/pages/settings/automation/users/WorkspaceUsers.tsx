import Button from '@/components/Button/Button';
import PageLoader from '@/components/PageLoader';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import WorkspaceInviteUserDialog from '@/ee/pages/settings/automation/users/components/WorkspaceInviteUserDialog';
import WorkspaceUserEnvironmentRoles from '@/ee/pages/settings/automation/users/components/WorkspaceUserEnvironmentRoles';
import {
    ENVIRONMENT_LABELS,
    ENVIRONMENT_ORDER,
    toWorkspaceMembers,
} from '@/ee/pages/settings/automation/users/util/workspace-environment-roles';
import {CUSTOM_ROLE_PREFIX, toRoleArguments} from '@/ee/pages/settings/automation/users/util/workspace-role-values';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {
    EnvironmentEnum,
    WorkspaceRole,
    useAddWorkspaceUserMutation,
    useAssignWorkspaceUserCustomRoleMutation,
    useCustomRolesQuery,
    useInviteWorkspaceUserMutation,
    useMyWorkspaceScopesQuery,
    useRemoveWorkspaceUserEnvironmentRoleMutation,
    useRemoveWorkspaceUserMutation,
    useSetWorkspaceUserEnvironmentRoleMutation,
    useUpdateWorkspaceUserRoleMutation,
    useUsersQuery,
    useWorkspaceUsersQuery,
} from '@/shared/middleware/graphql';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {getRoleLabel} from '@/shared/util/role-utils';
import {useQueryClient} from '@tanstack/react-query';
import {Trash2Icon} from 'lucide-react';
import {useMemo, useState} from 'react';
import {useShallow} from 'zustand/react/shallow';

// Derive from the generated GraphQL enum so a new server-side role appears here without a client change.
const WORKSPACE_ROLES = Object.values(WorkspaceRole);

const MEMBER_MANAGE_SCOPE = 'WORKSPACE_MEMBER_MANAGE';

const TENANT_ADMIN_AUTHORITY = 'ROLE_ADMIN';

const WorkspaceUsers = () => {
    const [actionError, setActionError] = useState<string | null>(null);
    const [inviteDialogOpen, setInviteDialogOpen] = useState(false);
    const [splittingUserId, setSplittingUserId] = useState<string | null>(null);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const queryClient = useQueryClient();

    const workspaceId = String(currentWorkspaceId);

    // Gated on the scope rather than on ROLE_ADMIN: that is what makes this page reachable by a workspace admin who
    // is not a tenant admin, which is the whole point of it existing.
    const {data: scopesData, isLoading: scopesLoading} = useMyWorkspaceScopesQuery({workspaceId});

    const canManageMembers = (scopesData?.myWorkspaceScopes ?? []).includes(MEMBER_MANAGE_SCOPE);

    const {data: usersData, isLoading: usersLoading} = useWorkspaceUsersQuery({workspaceId});

    // Memoised because addableUsers depends on it: a fresh [] literal each render would rebuild that list every time.
    const workspaceUsers = useMemo(() => usersData?.workspaceUsers ?? [], [usersData]);

    const members = useMemo(() => toWorkspaceMembers(workspaceUsers), [workspaceUsers]);

    const {account} = useAuthenticationStore(useShallow((state) => ({account: state.account})));

    // Listing every account in the tenant is ROLE_ADMIN-only, and deliberately so — it exposes the whole
    // organisation's user list. A workspace admin therefore gets no picker; adding a colleague who already has an
    // account is the invite-by-email path above, which reuses the account and consumes no extra seat.
    const isTenantAdmin = (account?.authorities ?? []).includes(TENANT_ADMIN_AUTHORITY);

    const {data: tenantUsersData} = useUsersQuery({pageNumber: 0, pageSize: 100}, {enabled: isTenantAdmin});

    const addableUsers = useMemo(() => {
        const memberUserIds = new Set(workspaceUsers.map((workspaceUser) => String(workspaceUser.userId)));

        return (tenantUsersData?.users?.content ?? [])
            .filter((user) => user?.id != null && !memberUserIds.has(String(user.id)))
            .map((user) => ({email: user!.email ?? '', id: String(user!.id)}));
    }, [tenantUsersData, workspaceUsers]);

    const {data: customRolesData} = useCustomRolesQuery({workspaceId});

    const customRoles = customRolesData?.customRoles ?? [];

    const invalidateWorkspaceUsers = () => {
        setActionError(null);
        queryClient.invalidateQueries({queryKey: ['WorkspaceUsers']});
    };

    // The typed membership errors -- last admin, self-demotion, inherited entry -- are answers to what the operator
    // just tried, so they belong beside the control rather than in a toast that outlives the page.
    const onActionError = (error: Error) => setActionError(error.message);

    const inviteWorkspaceUserMutation = useInviteWorkspaceUserMutation({
        onError: onActionError,
        onSuccess: () => {
            setInviteDialogOpen(false);
            invalidateWorkspaceUsers();
        },
    });

    const updateWorkspaceUserRoleMutation = useUpdateWorkspaceUserRoleMutation({
        onError: onActionError,
        onSuccess: invalidateWorkspaceUsers,
    });

    const assignCustomRoleMutation = useAssignWorkspaceUserCustomRoleMutation({
        onError: onActionError,
        onSuccess: invalidateWorkspaceUsers,
    });

    const removeWorkspaceUserMutation = useRemoveWorkspaceUserMutation({
        onError: onActionError,
        onSuccess: invalidateWorkspaceUsers,
    });

    const setEnvironmentRoleMutation = useSetWorkspaceUserEnvironmentRoleMutation({
        onError: onActionError,
        onSuccess: invalidateWorkspaceUsers,
    });

    const removeEnvironmentRoleMutation = useRemoveWorkspaceUserEnvironmentRoleMutation({
        onError: onActionError,
        onSuccess: invalidateWorkspaceUsers,
    });

    const addWorkspaceUserMutation = useAddWorkspaceUserMutation({
        onError: onActionError,
        onSuccess: () => {
            setInviteDialogOpen(false);
            invalidateWorkspaceUsers();
        },
    });

    const handleInvite = (email: string, roleValue: string) => {
        setActionError(null);

        inviteWorkspaceUserMutation.mutate({email, ...toRoleArguments(roleValue), workspaceId});
    };

    const handleAddExistingUser = (userId: string, roleValue: string) => {
        setActionError(null);

        addWorkspaceUserMutation.mutate({...toRoleArguments(roleValue), userId, workspaceId});
    };

    const handleRoleChange = (userId: string, value: string) => {
        setActionError(null);

        if (value.startsWith(CUSTOM_ROLE_PREFIX)) {
            assignCustomRoleMutation.mutate({
                customRoleId: value.slice(CUSTOM_ROLE_PREFIX.length),
                userId,
                workspaceId,
            });
        } else {
            updateWorkspaceUserRoleMutation.mutate({role: value as WorkspaceRole, userId, workspaceId});
        }
    };

    const handleEnvironmentRoleChange = (userId: string, environment: EnvironmentEnum, value: string) => {
        setActionError(null);
        setSplittingUserId(null);

        // Same custom-role encoding the workspace-wide select uses: custom_role_id rides in the same row as the
        // environment, so a custom role is per-environment without any further model change.
        if (value.startsWith(CUSTOM_ROLE_PREFIX)) {
            setEnvironmentRoleMutation.mutate({
                customRoleId: value.slice(CUSTOM_ROLE_PREFIX.length),
                environment,
                userId,
                workspaceId,
            });
        } else {
            setEnvironmentRoleMutation.mutate({environment, role: value as WorkspaceRole, userId, workspaceId});
        }
    };

    const handleEnvironmentRoleRemove = (userId: string, environment: EnvironmentEnum) => {
        setActionError(null);

        removeEnvironmentRoleMutation.mutate({environment, userId, workspaceId});
    };

    const handleRemove = (userId: string) => {
        setActionError(null);
        removeWorkspaceUserMutation.mutate({userId, workspaceId});
    };

    return (
        <LayoutContainer
            header={
                <Header
                    centerTitle
                    position="main"
                    right={
                        canManageMembers ? (
                            <Button onClick={() => setInviteDialogOpen(true)}>Invite User</Button>
                        ) : undefined
                    }
                    title="Users"
                />
            }
            leftSidebarOpen={false}
        >
            <PageLoader loading={scopesLoading || usersLoading}>
                {canManageMembers ? (
                    <div className="w-full space-y-6 p-4">
                        {actionError && (
                            <div
                                className="rounded-md border border-destructive/50 p-3 text-sm text-destructive"
                                role="alert"
                            >
                                {actionError}
                            </div>
                        )}

                        <WorkspaceInviteUserDialog
                            addableUsers={addableUsers}
                            customRoles={customRoles.map((customRole) => ({
                                id: String(customRole.id),
                                name: customRole.name,
                            }))}
                            isTenantAdmin={isTenantAdmin}
                            onAdd={handleAddExistingUser}
                            onInvite={handleInvite}
                            onOpenChange={setInviteDialogOpen}
                            open={inviteDialogOpen}
                        />

                        <Table>
                            <TableHeader>
                                <TableRow>
                                    <TableHead>Email</TableHead>

                                    <TableHead>Name</TableHead>

                                    <TableHead>Role</TableHead>

                                    <TableHead />
                                </TableRow>
                            </TableHeader>

                            <TableBody>
                                {members.map((member) => (
                                    <TableRow key={member.userId}>
                                        <TableCell>{member.user?.email}</TableCell>

                                        <TableCell>
                                            {[member.user?.firstName, member.user?.lastName].filter(Boolean).join(' ')}
                                        </TableCell>

                                        <TableCell>
                                            {member.inherited && (
                                                <span className="text-sm text-muted-foreground">
                                                    {getRoleLabel(WorkspaceRole.Admin)} — inherited from tenant admin
                                                </span>
                                            )}

                                            {!member.inherited && member.environmentRoles.length > 0 && (
                                                <WorkspaceUserEnvironmentRoles
                                                    customRoles={customRoles.map((customRole) => ({
                                                        id: String(customRole.id),
                                                        name: customRole.name,
                                                    }))}
                                                    environmentRoles={member.environmentRoles}
                                                    onRemove={(environment) =>
                                                        handleEnvironmentRoleRemove(member.userId, environment)
                                                    }
                                                    onRoleChange={(environment, value) =>
                                                        handleEnvironmentRoleChange(member.userId, environment, value)
                                                    }
                                                />
                                            )}

                                            {!member.inherited && member.environmentRoles.length === 0 && (
                                                <div className="space-y-2">
                                                    <Select
                                                        onValueChange={(value) =>
                                                            handleRoleChange(member.userId, value)
                                                        }
                                                        value={
                                                            member.implicitRow?.customRoleId
                                                                ? `${CUSTOM_ROLE_PREFIX}${member.implicitRow.customRoleId}`
                                                                : (member.implicitRow?.workspaceRole ?? undefined)
                                                        }
                                                    >
                                                        <SelectTrigger className="w-44">
                                                            <SelectValue placeholder="Select a role" />
                                                        </SelectTrigger>

                                                        <SelectContent>
                                                            {WORKSPACE_ROLES.map((workspaceRole) => (
                                                                <SelectItem key={workspaceRole} value={workspaceRole}>
                                                                    {getRoleLabel(workspaceRole)}
                                                                </SelectItem>
                                                            ))}

                                                            {customRoles.map((customRole) => (
                                                                <SelectItem
                                                                    key={customRole.id}
                                                                    value={`${CUSTOM_ROLE_PREFIX}${customRole.id}`}
                                                                >
                                                                    {customRole.name}
                                                                </SelectItem>
                                                            ))}
                                                        </SelectContent>
                                                    </Select>

                                                    {splittingUserId === member.userId ? (
                                                        <div className="space-y-1">
                                                            <Select
                                                                onValueChange={(environment) =>
                                                                    handleEnvironmentRoleChange(
                                                                        member.userId,
                                                                        environment as EnvironmentEnum,
                                                                        member.implicitRow?.workspaceRole ??
                                                                            WorkspaceRole.Viewer
                                                                    )
                                                                }
                                                                value=""
                                                            >
                                                                <SelectTrigger
                                                                    aria-label="Environment to grant first"
                                                                    className="w-44"
                                                                >
                                                                    <SelectValue placeholder="Choose environment..." />
                                                                </SelectTrigger>

                                                                <SelectContent>
                                                                    {ENVIRONMENT_ORDER.map((environment) => (
                                                                        <SelectItem
                                                                            key={environment}
                                                                            value={environment}
                                                                        >
                                                                            {ENVIRONMENT_LABELS[environment]}
                                                                        </SelectItem>
                                                                    ))}
                                                                </SelectContent>
                                                            </Select>

                                                            {/*
                                                              Naming the first environment is what switches the member
                                                              out of workspace-wide mode, and an environment with no
                                                              row is denied -- so this grant also revokes the rest
                                                              until they are granted back.
                                                            */}

                                                            <p className="text-xs text-muted-foreground">
                                                                They will lose access to every other environment until
                                                                you grant it.
                                                            </p>
                                                        </div>
                                                    ) : (
                                                        <button
                                                            className="text-xs text-muted-foreground hover:text-foreground"
                                                            onClick={() => setSplittingUserId(member.userId)}
                                                            type="button"
                                                        >
                                                            Use per-environment roles
                                                        </button>
                                                    )}
                                                </div>
                                            )}
                                        </TableCell>

                                        <TableCell>
                                            {/*
                                              An inherited entry has no membership row to remove — the access comes
                                              from tenant admin, so revoking it is a tenant-level act, not a
                                              workspace one.
                                            */}

                                            {!member.inherited && (
                                                <Button
                                                    icon={<Trash2Icon />}
                                                    onClick={() => handleRemove(member.userId)}
                                                    variant="ghost"
                                                />
                                            )}
                                        </TableCell>
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
                    </div>
                ) : (
                    <div className="p-4 text-sm text-muted-foreground">
                        You do not have permission to manage members of this workspace.
                    </div>
                )}
            </PageLoader>
        </LayoutContainer>
    );
};

export default WorkspaceUsers;
