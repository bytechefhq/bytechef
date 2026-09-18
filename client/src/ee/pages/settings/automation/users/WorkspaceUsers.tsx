import Button from '@/components/Button/Button';
import PageLoader from '@/components/PageLoader';
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from '@/components/ui/alert-dialog';
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
import {WorkspaceScopeType} from '@/shared/hooks/useHasWorkspaceScope';
import {useIsTenantAdmin} from '@/shared/hooks/useIsTenantAdmin';
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
import {invalidateMyPermissionQueries} from '@/shared/queries/permissions.queries';
import {getRoleLabel} from '@/shared/util/role-utils';
import {useQueryClient} from '@tanstack/react-query';
import {Trash2Icon} from 'lucide-react';
import {useMemo, useState} from 'react';

// Derive from the generated GraphQL enum so a new server-side role appears here without a client change.
const WORKSPACE_ROLES = Object.values(WorkspaceRole);

// Typed against the client's scope allowlist rather than left as a bare string, so a typo here is a compile error
// instead of a silently-always-false permission check.
const MEMBER_MANAGE_SCOPE: WorkspaceScopeType = 'WORKSPACE_MEMBER_MANAGE';

const WorkspaceUsers = () => {
    const [actionError, setActionError] = useState<string | null>(null);
    const [inviteDialogOpen, setInviteDialogOpen] = useState(false);
    const [pendingRemovalMember, setPendingRemovalMember] = useState<{email: string; userId: string} | null>(null);
    const [splittingUserId, setSplittingUserId] = useState<string | null>(null);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const isTenantAdmin = useIsTenantAdmin();

    const queryClient = useQueryClient();

    const workspaceId = String(currentWorkspaceId);

    // Gated on the scope rather than on ROLE_ADMIN: that is what makes this page reachable by a workspace admin who
    // is not a tenant admin, which is the whole point of it existing.
    const {data: developmentScopesData, isLoading: developmentScopesLoading} = useMyWorkspaceScopesQuery({
        environment: EnvironmentEnum.Development,
        workspaceId,
    });
    const {data: stagingScopesData, isLoading: stagingScopesLoading} = useMyWorkspaceScopesQuery({
        environment: EnvironmentEnum.Staging,
        workspaceId,
    });
    const {data: productionScopesData, isLoading: productionScopesLoading} = useMyWorkspaceScopesQuery({
        environment: EnvironmentEnum.Production,
        workspaceId,
    });

    const memberManageEnvironments = useMemo(() => {
        const scopesByEnvironment: Record<EnvironmentEnum, string[]> = {
            [EnvironmentEnum.Development]: developmentScopesData?.myWorkspaceScopes ?? [],
            [EnvironmentEnum.Production]: productionScopesData?.myWorkspaceScopes ?? [],
            [EnvironmentEnum.Staging]: stagingScopesData?.myWorkspaceScopes ?? [],
        };

        return ENVIRONMENT_ORDER.filter((environment) =>
            scopesByEnvironment[environment].includes(MEMBER_MANAGE_SCOPE)
        );
    }, [developmentScopesData, productionScopesData, stagingScopesData]);

    const canManageMembers = memberManageEnvironments.length === ENVIRONMENT_ORDER.length;

    const scopesLoading = developmentScopesLoading || productionScopesLoading || stagingScopesLoading;

    const {data: usersData, isLoading: usersLoading} = useWorkspaceUsersQuery({workspaceId});

    // Memoised because addableUsers depends on it: a fresh [] literal each render would rebuild that list every time.
    const workspaceUsers = useMemo(() => usersData?.workspaceUsers ?? [], [usersData]);

    const members = useMemo(() => toWorkspaceMembers(workspaceUsers), [workspaceUsers]);

    // Listing every account in the tenant is ROLE_ADMIN-only, and deliberately so — it exposes the whole
    // organisation's user list. A workspace admin therefore gets no picker; adding a colleague who already has an
    // account is the invite-by-email path above, which reuses the existing account rather than provisioning a
    // second one.
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

        // A membership mutation can move the operator's own access, not only the target's -- removing yourself,
        // demoting yourself, or trading your built-in role for a custom one. The member list refetching does not
        // re-answer `myWorkspaceScopes`, so without this the page keeps rendering Invite/Remove against a cached
        // permission answer the server no longer agrees with.
        invalidateMyPermissionQueries(queryClient);
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

    const handleRemoveConfirm = () => {
        if (!pendingRemovalMember) {
            return;
        }

        setActionError(null);
        removeWorkspaceUserMutation.mutate({userId: pendingRemovalMember.userId, workspaceId});
        setPendingRemovalMember(null);
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
                {memberManageEnvironments.length > 0 ? (
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

                        <Table className="table-fixed">
                            <TableHeader>
                                <TableRow>
                                    <TableHead className="w-1/4">Email</TableHead>

                                    <TableHead className="w-1/6">Name</TableHead>

                                    <TableHead>Role</TableHead>

                                    <TableHead className="w-16" />
                                </TableRow>
                            </TableHeader>

                            <TableBody>
                                {members.map((member) => (
                                    <TableRow key={member.userId}>
                                        <TableCell className="truncate align-top leading-9">
                                            {member.user?.email}
                                        </TableCell>

                                        <TableCell className="truncate align-top leading-9">
                                            {[member.user?.firstName, member.user?.lastName].filter(Boolean).join(' ')}
                                        </TableCell>

                                        <TableCell className="align-top">
                                            {member.inherited && (
                                                <span className="text-sm leading-9 text-muted-foreground">
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
                                                    manageableEnvironments={memberManageEnvironments}
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
                                                    <div className="flex items-center gap-2">
                                                        <span className="w-32 shrink-0 text-sm text-muted-foreground">
                                                            All environments
                                                        </span>

                                                        <Select
                                                            disabled={!canManageMembers}
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
                                                                    <SelectItem
                                                                        key={workspaceRole}
                                                                        value={workspaceRole}
                                                                    >
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
                                                    </div>

                                                    {splittingUserId === member.userId ? (
                                                        <div className="ml-[8.5rem] space-y-1">
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
                                                                    {memberManageEnvironments.map((environment) => (
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
                                                            className="ml-[8.5rem] text-xs text-muted-foreground hover:text-foreground"
                                                            onClick={() => setSplittingUserId(member.userId)}
                                                            type="button"
                                                        >
                                                            Use per-environment roles
                                                        </button>
                                                    )}
                                                </div>
                                            )}
                                        </TableCell>

                                        <TableCell className="align-top">
                                            {/*
                                              An inherited entry has no membership row to remove — the access comes
                                              from tenant admin, so revoking it is a tenant-level act, not a
                                              workspace one.
                                            */}

                                            {!member.inherited && canManageMembers && (
                                                <Button
                                                    aria-label={`Remove ${member.user?.email || `user ${member.userId}`} from workspace`}
                                                    icon={<Trash2Icon className="text-destructive" />}
                                                    onClick={() =>
                                                        setPendingRemovalMember({
                                                            email: member.user?.email || `user ${member.userId}`,
                                                            userId: member.userId,
                                                        })
                                                    }
                                                    variant="ghost"
                                                />
                                            )}
                                        </TableCell>
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>

                        {pendingRemovalMember && (
                            <AlertDialog open={true}>
                                <AlertDialogContent>
                                    <AlertDialogHeader>
                                        <AlertDialogTitle>
                                            Remove {pendingRemovalMember.email} from the workspace?
                                        </AlertDialogTitle>

                                        <AlertDialogDescription>
                                            They will lose access to this workspace in every environment. Their account
                                            is not deleted.
                                        </AlertDialogDescription>
                                    </AlertDialogHeader>

                                    <AlertDialogFooter>
                                        <AlertDialogCancel onClick={() => setPendingRemovalMember(null)}>
                                            Cancel
                                        </AlertDialogCancel>

                                        <AlertDialogAction className="bg-red-600" onClick={handleRemoveConfirm}>
                                            Remove
                                        </AlertDialogAction>
                                    </AlertDialogFooter>
                                </AlertDialogContent>
                            </AlertDialog>
                        )}
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
