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
import WorkspaceUserEnvironmentRoles from '@/ee/pages/settings/automation/users/components/WorkspaceUserEnvironmentRoles';
import {
    ENVIRONMENT_LABELS,
    ENVIRONMENT_ORDER,
    toWorkspaceMembers,
} from '@/ee/pages/settings/automation/users/util/workspace-environment-roles';
import EmbeddedAddMemberDialog from '@/ee/pages/settings/embedded/members/components/EmbeddedAddMemberDialog';
import {EmbeddedScopeType, UNION_ENVIRONMENT_ID, useHasEmbeddedScope} from '@/shared/hooks/useHasEmbeddedScope';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {
    EnvironmentEnum,
    WorkspaceRole,
    useAddEmbeddedUserByEmailMutation,
    useEmbeddedUsersQuery,
    useMyEmbeddedScopesQuery,
    useRemoveEmbeddedUserEnvironmentRoleMutation,
    useRemoveEmbeddedUserMutation,
    useSetEmbeddedUserEnvironmentRoleMutation,
    useUpdateEmbeddedUserRoleMutation,
} from '@/shared/middleware/graphql';
import {invalidateMyEmbeddedScopeQueries} from '@/shared/queries/permissions.queries';
import {getRoleLabel} from '@/shared/util/role-utils';
import {useQueryClient} from '@tanstack/react-query';
import {Trash2Icon} from 'lucide-react';
import {useMemo, useState} from 'react';

const EMBEDDED_ROLES = Object.values(WorkspaceRole);

const MEMBER_MANAGE_SCOPE: EmbeddedScopeType = 'EMBEDDED_MEMBER_MANAGE';

const EmbeddedMembers = () => {
    const [addDialogOpen, setAddDialogOpen] = useState(false);
    const [pendingRemovalMember, setPendingRemovalMember] = useState<{email: string; userId: string} | null>(null);
    const [splittingUserId, setSplittingUserId] = useState<string | null>(null);

    const queryClient = useQueryClient();

    const memberViewGranted = useHasEmbeddedScope('EMBEDDED_VIEW', UNION_ENVIRONMENT_ID);

    const {data: developmentScopesData, isLoading: developmentScopesLoading} = useMyEmbeddedScopesQuery({
        environment: EnvironmentEnum.Development,
    });
    const {data: stagingScopesData, isLoading: stagingScopesLoading} = useMyEmbeddedScopesQuery({
        environment: EnvironmentEnum.Staging,
    });
    const {data: productionScopesData, isLoading: productionScopesLoading} = useMyEmbeddedScopesQuery({
        environment: EnvironmentEnum.Production,
    });

    const {data: embeddedUsersData, isLoading: embeddedUsersLoading} = useEmbeddedUsersQuery(undefined, {
        enabled: memberViewGranted,
    });

    const memberManageEnvironments = useMemo(() => {
        const scopesByEnvironment: Record<EnvironmentEnum, string[]> = {
            [EnvironmentEnum.Development]: developmentScopesData?.myEmbeddedScopes ?? [],
            [EnvironmentEnum.Production]: productionScopesData?.myEmbeddedScopes ?? [],
            [EnvironmentEnum.Staging]: stagingScopesData?.myEmbeddedScopes ?? [],
        };

        return ENVIRONMENT_ORDER.filter((environment) =>
            scopesByEnvironment[environment].includes(MEMBER_MANAGE_SCOPE)
        );
    }, [developmentScopesData, productionScopesData, stagingScopesData]);

    const canManageMembers = memberManageEnvironments.length === ENVIRONMENT_ORDER.length;

    const scopesLoading = developmentScopesLoading || productionScopesLoading || stagingScopesLoading;

    const members = useMemo(
        () =>
            toWorkspaceMembers(
                (embeddedUsersData?.embeddedUsers ?? []).map((embeddedUser) => ({
                    customRoleId: embeddedUser.customRoleId,
                    environment: embeddedUser.environment,
                    id: embeddedUser.id,
                    inherited: embeddedUser.inherited,
                    user: embeddedUser.user,
                    userId: embeddedUser.userId,
                    workspaceRole: embeddedUser.embeddedRole,
                }))
            ),
        [embeddedUsersData]
    );

    const refreshMembers = () => {
        invalidateMyEmbeddedScopeQueries(queryClient);

        void queryClient.invalidateQueries({queryKey: ['EmbeddedUsers']});
    };

    const addEmbeddedUserByEmailMutation = useAddEmbeddedUserByEmailMutation({
        onSuccess: () => {
            setAddDialogOpen(false);
            refreshMembers();
        },
    });

    const updateEmbeddedUserRoleMutation = useUpdateEmbeddedUserRoleMutation({onSuccess: refreshMembers});

    const removeEmbeddedUserMutation = useRemoveEmbeddedUserMutation({onSuccess: refreshMembers});

    const setEnvironmentRoleMutation = useSetEmbeddedUserEnvironmentRoleMutation({onSuccess: refreshMembers});

    const removeEnvironmentRoleMutation = useRemoveEmbeddedUserEnvironmentRoleMutation({onSuccess: refreshMembers});

    const handleAdd = (email: string, role: WorkspaceRole) => {
        addEmbeddedUserByEmailMutation.mutate({email, role});
    };

    const handleRoleChange = (userId: string, role: WorkspaceRole) => {
        updateEmbeddedUserRoleMutation.mutate({role, userId});
    };

    const handleEnvironmentRoleChange = (userId: string, environment: EnvironmentEnum, role: WorkspaceRole) => {
        setSplittingUserId(null);

        setEnvironmentRoleMutation.mutate({environment, role, userId});
    };

    const handleEnvironmentRoleRemove = (userId: string, environment: EnvironmentEnum) => {
        removeEnvironmentRoleMutation.mutate({environment, userId});
    };

    const handleRemoveConfirm = () => {
        if (!pendingRemovalMember) {
            return;
        }

        removeEmbeddedUserMutation.mutate({userId: pendingRemovalMember.userId});
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
                            <Button onClick={() => setAddDialogOpen(true)}>Add member</Button>
                        ) : undefined
                    }
                    title="Members"
                />
            }
            leftSidebarOpen={false}
        >
            <PageLoader loading={scopesLoading || embeddedUsersLoading}>
                {memberViewGranted ? (
                    <div className="w-full space-y-6 p-4">
                        <EmbeddedAddMemberDialog
                            onAdd={handleAdd}
                            onOpenChange={setAddDialogOpen}
                            open={addDialogOpen}
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
                                                    {getRoleLabel(WorkspaceRole.Admin)} - inherited from tenant admin
                                                </span>
                                            )}

                                            {!member.inherited && member.environmentRoles.length > 0 && (
                                                <WorkspaceUserEnvironmentRoles
                                                    environmentRoles={member.environmentRoles}
                                                    manageableEnvironments={memberManageEnvironments}
                                                    onRemove={(environment) =>
                                                        handleEnvironmentRoleRemove(member.userId, environment)
                                                    }
                                                    onRoleChange={(environment, role) =>
                                                        handleEnvironmentRoleChange(
                                                            member.userId,
                                                            environment,
                                                            role as WorkspaceRole
                                                        )
                                                    }
                                                    onlyRoleRemovalDescription={(roleLabel) =>
                                                        `This is their only environment role, so it becomes an embedded-wide role. They will keep the ${roleLabel} role in every environment.`
                                                    }
                                                    rolePlaceholder="Custom role"
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
                                                                handleRoleChange(member.userId, value as WorkspaceRole)
                                                            }
                                                            value={member.implicitRow?.workspaceRole ?? undefined}
                                                        >
                                                            <SelectTrigger className="w-44">
                                                                <SelectValue placeholder="Custom role" />
                                                            </SelectTrigger>

                                                            <SelectContent>
                                                                {EMBEDDED_ROLES.map((embeddedRole) => (
                                                                    <SelectItem key={embeddedRole} value={embeddedRole}>
                                                                        {getRoleLabel(embeddedRole)}
                                                                    </SelectItem>
                                                                ))}
                                                            </SelectContent>
                                                        </Select>
                                                    </div>

                                                    {canManageMembers &&
                                                        (splittingUserId === member.userId ? (
                                                            <div className="ml-[8.5rem] space-y-1">
                                                                <Select
                                                                    onValueChange={(environment) =>
                                                                        handleEnvironmentRoleChange(
                                                                            member.userId,
                                                                            environment as EnvironmentEnum,
                                                                            (member.implicitRow
                                                                                ?.workspaceRole as WorkspaceRole) ??
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

                                                                <p className="text-xs text-muted-foreground">
                                                                    They will lose access to every other environment
                                                                    until you grant it.
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
                                                        ))}
                                                </div>
                                            )}
                                        </TableCell>

                                        <TableCell className="align-top">
                                            {!member.inherited && canManageMembers && (
                                                <Button
                                                    aria-label={`Remove ${member.user?.email || `user ${member.userId}`} from embedded`}
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
                                            Remove {pendingRemovalMember.email} from embedded?
                                        </AlertDialogTitle>

                                        <AlertDialogDescription>
                                            They will lose access to embedded in every environment. Their account is not
                                            deleted.
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
                        You do not have permission to view the members of embedded.
                    </div>
                )}
            </PageLoader>
        </LayoutContainer>
    );
};

export default EmbeddedMembers;
