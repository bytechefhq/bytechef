import {Command, CommandEmpty, CommandGroup, CommandInput, CommandItem, CommandList} from '@/components/ui/command';
import {Dialog, DialogContent, DialogHeader, DialogTitle} from '@/components/ui/dialog';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import {
    WorkspaceRole,
    useAddWorkspaceUserMutation,
    useRemoveWorkspaceUserMutation,
    useUpdateWorkspaceUserRoleMutation,
    useUsersQuery,
    useWorkspaceUsersQuery,
} from '@/shared/middleware/graphql';
import {invalidateMyPermissionQueries} from '@/shared/queries/permissions.queries';
import {getRoleLabel} from '@/shared/util/role-utils';
import {useQueryClient} from '@tanstack/react-query';
import {CheckIcon, PlusIcon, Trash2Icon, XIcon} from 'lucide-react';
import {useState} from 'react';

// Derive from the generated GraphQL enum so new server-side roles are picked up automatically. The values are the
// string names (e.g., 'ADMIN') which matches both the select value and the backend contract.
const WORKSPACE_ROLES = Object.values(WorkspaceRole);

interface WorkspaceUsersDialogProps {
    onClose: () => void;
    open: boolean;
    workspaceId: number;
}

/**
 * Tenant-admin member editor for an arbitrary workspace, opened from the admin-only workspace list. It carries no
 * permission gate of its own for the reason documented on `WorkspaceListItem`: only a tenant admin reaches it, and a
 * tenant admin administers every workspace. Should this ever be mounted somewhere a non-admin can reach, gate it on the
 * `WORKSPACE_MEMBER_MANAGE` scope via `useHasWorkspaceScope` — a role check would deny a custom-role member who holds
 * exactly that scope, because `getMyWorkspaceRole` answers null for a custom-role membership.
 */
const WorkspaceUsersDialog = ({onClose, open, workspaceId}: WorkspaceUsersDialogProps) => {
    const [addingUser, setAddingUser] = useState(false);
    const [selectedUserId, setSelectedUserId] = useState<string | null>(null);
    const [selectedRole, setSelectedRole] = useState<WorkspaceRole>(WorkspaceRole.Editor);
    const [userSearchOpen, setUserSearchOpen] = useState(false);

    const queryClient = useQueryClient();

    const {data: usersData} = useWorkspaceUsersQuery({workspaceId: String(workspaceId)}, {enabled: open});

    const workspaceUsers = usersData?.workspaceUsers ?? [];

    const {data: allUsersData} = useUsersQuery({pageNumber: 0, pageSize: 100}, {enabled: open && addingUser});

    const allUsers = (allUsersData?.users?.content ?? []).filter(
        (user): user is NonNullable<typeof user> => user != null && user.id != null
    );

    const existingUserIds = new Set(workspaceUsers.map((workspaceUser) => workspaceUser.userId));

    const availableUsers = allUsers.filter((user) => !existingUserIds.has(user.id!));

    const invalidateWorkspaceUsers = () => {
        queryClient.invalidateQueries({queryKey: ['WorkspaceUsers']});

        invalidateMyPermissionQueries(queryClient);
    };

    const addUserMutation = useAddWorkspaceUserMutation({onSuccess: invalidateWorkspaceUsers});

    const updateRoleMutation = useUpdateWorkspaceUserRoleMutation({onSuccess: invalidateWorkspaceUsers});

    const removeUserMutation = useRemoveWorkspaceUserMutation({onSuccess: invalidateWorkspaceUsers});

    const handleAddConfirm = () => {
        if (selectedUserId) {
            addUserMutation.mutate({
                role: selectedRole,
                userId: selectedUserId,
                workspaceId: String(workspaceId),
            });

            setAddingUser(false);
            setSelectedUserId(null);
            setSelectedRole(WorkspaceRole.Editor);
        }
    };

    const handleAddCancel = () => {
        setAddingUser(false);
        setSelectedUserId(null);
        setSelectedRole(WorkspaceRole.Editor);
    };

    const selectedUserLabel = availableUsers.find((user) => user.id === selectedUserId)?.email ?? 'Search users...';

    return (
        <Dialog onOpenChange={onClose} open={open}>
            <DialogContent className="max-w-2xl">
                <DialogHeader>
                    <DialogTitle>Workspace Members</DialogTitle>
                </DialogHeader>

                <Table>
                    <TableHeader>
                        <TableRow>
                            <TableHead>User</TableHead>

                            <TableHead>Role</TableHead>

                            <TableHead>Added</TableHead>

                            <TableHead className="w-12" />
                        </TableRow>
                    </TableHeader>

                    <TableBody>
                        {workspaceUsers.map((workspaceUser) => {
                            // An inherited entry is synthesized from the tenant-admin authority and has no membership
                            // row behind it, so its `id` is null -- keying on that collides every inherited entry onto
                            // one React key. A row scoped to one environment likewise has no workspace-wide row for
                            // these controls to act on: the select would resolve an arbitrary environment's row and
                            // edit that one silently. Both are read-only here; per-environment roles and tenant-admin
                            // membership are managed on the Users page and at tenant level respectively.
                            const rowLocked = workspaceUser.inherited || workspaceUser.environment != null;

                            // A per-environment row carrying a custom role has a null workspaceRole; the custom role's
                            // name is not in this query, so say which kind of role it is rather than rendering 'null'.
                            const environmentRoleLabel = workspaceUser.workspaceRole
                                ? getRoleLabel(workspaceUser.workspaceRole)
                                : 'Custom role';

                            const lockedRoleLabel = workspaceUser.inherited
                                ? `${getRoleLabel(WorkspaceRole.Admin)} — inherited from tenant admin`
                                : `${environmentRoleLabel} in ${workspaceUser.environment}`;

                            return (
                                <TableRow
                                    key={
                                        workspaceUser.id ??
                                        `inherited-${workspaceUser.userId}-${workspaceUser.environment ?? 'workspace'}`
                                    }
                                >
                                    <TableCell>
                                        <div>
                                            <div className="text-sm font-medium">
                                                {workspaceUser.user?.email || `User ${workspaceUser.userId}`}
                                            </div>

                                            {workspaceUser.user?.firstName && (
                                                <div className="text-xs text-muted-foreground">
                                                    {workspaceUser.user.firstName} {workspaceUser.user.lastName}
                                                </div>
                                            )}
                                        </div>
                                    </TableCell>

                                    <TableCell>
                                        {rowLocked ? (
                                            <span className="text-sm text-muted-foreground">{lockedRoleLabel}</span>
                                        ) : (
                                            <Select
                                                onValueChange={(role) =>
                                                    updateRoleMutation.mutate({
                                                        role: role as WorkspaceRole,
                                                        userId: workspaceUser.userId,
                                                        workspaceId: String(workspaceId),
                                                    })
                                                }
                                                value={workspaceUser.workspaceRole ?? undefined}
                                            >
                                                <SelectTrigger aria-label="Workspace role" className="w-32">
                                                    <SelectValue />
                                                </SelectTrigger>

                                                <SelectContent>
                                                    {WORKSPACE_ROLES.map((role) => (
                                                        <SelectItem key={role} value={role}>
                                                            {getRoleLabel(role)}
                                                        </SelectItem>
                                                    ))}
                                                </SelectContent>
                                            </Select>
                                        )}
                                    </TableCell>

                                    <TableCell>
                                        {workspaceUser.createdDate
                                            ? new Date(workspaceUser.createdDate).toLocaleDateString()
                                            : '-'}
                                    </TableCell>

                                    <TableCell>
                                        {!rowLocked && (
                                            <button
                                                aria-label={`Remove ${workspaceUser.user?.email || `user ${workspaceUser.userId}`} from workspace`}
                                                className="text-destructive hover:text-destructive/80"
                                                onClick={() =>
                                                    removeUserMutation.mutate({
                                                        userId: workspaceUser.userId,
                                                        workspaceId: String(workspaceId),
                                                    })
                                                }
                                                type="button"
                                            >
                                                <Trash2Icon className="size-4" />
                                            </button>
                                        )}
                                    </TableCell>
                                </TableRow>
                            );
                        })}

                        {addingUser && (
                            <TableRow>
                                <TableCell>
                                    <Popover onOpenChange={setUserSearchOpen} open={userSearchOpen}>
                                        <PopoverTrigger asChild>
                                            <button
                                                aria-label="Select user to add"
                                                className="w-full rounded-md border px-3 py-2 text-left text-sm"
                                            >
                                                {selectedUserId ? selectedUserLabel : 'Search users...'}
                                            </button>
                                        </PopoverTrigger>

                                        <PopoverContent align="start" className="w-64 p-0">
                                            <Command>
                                                <CommandInput placeholder="Search by email..." />

                                                <CommandList>
                                                    <CommandEmpty>No users found.</CommandEmpty>

                                                    <CommandGroup>
                                                        {availableUsers.map((user) => (
                                                            <CommandItem
                                                                key={user.id}
                                                                onSelect={() => {
                                                                    setSelectedUserId(user.id!);
                                                                    setUserSearchOpen(false);
                                                                }}
                                                                value={user.email || ''}
                                                            >
                                                                <div>
                                                                    <div className="text-sm">{user.email || ''}</div>

                                                                    {user.firstName != null && (
                                                                        <div className="text-xs text-muted-foreground">
                                                                            {user.firstName} {user.lastName || ''}
                                                                        </div>
                                                                    )}
                                                                </div>
                                                            </CommandItem>
                                                        ))}
                                                    </CommandGroup>
                                                </CommandList>
                                            </Command>
                                        </PopoverContent>
                                    </Popover>
                                </TableCell>

                                <TableCell>
                                    <Select
                                        onValueChange={(role) => setSelectedRole(role as WorkspaceRole)}
                                        value={selectedRole}
                                    >
                                        <SelectTrigger aria-label="Workspace role for new user" className="w-32">
                                            <SelectValue />
                                        </SelectTrigger>

                                        <SelectContent>
                                            {WORKSPACE_ROLES.map((role) => (
                                                <SelectItem key={role} value={role}>
                                                    {getRoleLabel(role)}
                                                </SelectItem>
                                            ))}
                                        </SelectContent>
                                    </Select>
                                </TableCell>

                                <TableCell>
                                    <div className="flex gap-1">
                                        <button
                                            aria-label="Confirm add user"
                                            className="rounded p-1 hover:bg-accent disabled:opacity-50"
                                            disabled={!selectedUserId}
                                            onClick={handleAddConfirm}
                                        >
                                            <CheckIcon className="size-4" />
                                        </button>

                                        <button
                                            aria-label="Cancel adding user"
                                            className="rounded p-1 hover:bg-accent"
                                            onClick={handleAddCancel}
                                        >
                                            <XIcon className="size-4" />
                                        </button>
                                    </div>
                                </TableCell>

                                <TableCell />
                            </TableRow>
                        )}
                    </TableBody>
                </Table>

                {!addingUser && (
                    <button
                        className="flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
                        onClick={() => setAddingUser(true)}
                    >
                        <PlusIcon className="size-4" />
                        Add user
                    </button>
                )}
            </DialogContent>
        </Dialog>
    );
};

export default WorkspaceUsersDialog;
