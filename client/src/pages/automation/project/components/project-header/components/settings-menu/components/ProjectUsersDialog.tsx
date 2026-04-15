import {Command, CommandEmpty, CommandGroup, CommandInput, CommandItem, CommandList} from '@/components/ui/command';
import {Dialog, DialogContent, DialogHeader, DialogTitle} from '@/components/ui/dialog';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import {useHasProjectScope} from '@/shared/hooks/useHasProjectScope';
import {useLoadProjectPermissions} from '@/shared/hooks/useLoadProjectPermissions';
import {
    ProjectRole,
    useAddProjectUserMutation,
    useProjectUsersQuery,
    useRemoveProjectUserMutation,
    useUpdateProjectUserRoleMutation,
    useUsersQuery,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {CheckIcon, PlusIcon, Trash2Icon, XIcon} from 'lucide-react';
import {useState} from 'react';

// Derive from the generated GraphQL enum so new server-side roles are picked up automatically.
const PROJECT_ROLES = Object.values(ProjectRole);

interface ProjectUsersDialogProps {
    onClose: () => void;
    open: boolean;
    projectId: number;
}

const ProjectUsersDialog = ({onClose, open, projectId}: ProjectUsersDialogProps) => {
    const [addingUser, setAddingUser] = useState(false);
    const [selectedUserId, setSelectedUserId] = useState<string | null>(null);
    const [selectedRole, setSelectedRole] = useState<ProjectRole>(ProjectRole.Editor);
    const [userSearchOpen, setUserSearchOpen] = useState(false);

    const queryClient = useQueryClient();

    // Ensure the permission store is primed before useHasProjectScope consults it. Idempotent across parents that
    // already primed.
    useLoadProjectPermissions(projectId);

    const canManageMembers = useHasProjectScope(projectId, 'PROJECT_MANAGE_USERS');

    const {data: usersData} = useProjectUsersQuery({projectId: String(projectId)}, {enabled: open});

    const projectUsers = usersData?.projectUsers ?? [];

    const {data: allUsersData} = useUsersQuery({pageNumber: 0, pageSize: 100}, {enabled: open && addingUser});

    const allUsers = (allUsersData?.users?.content ?? []).filter(
        (user): user is NonNullable<typeof user> => user != null && user.id != null
    );

    const existingUserIds = new Set(projectUsers.map((projectUser) => projectUser.userId));

    const availableUsers = allUsers.filter((user) => !existingUserIds.has(user.id!));

    const invalidateProjectUsers = () => {
        queryClient.invalidateQueries({queryKey: ['ProjectUsers']});
    };

    const addUserMutation = useAddProjectUserMutation({onSuccess: invalidateProjectUsers});

    const updateRoleMutation = useUpdateProjectUserRoleMutation({onSuccess: invalidateProjectUsers});

    const removeUserMutation = useRemoveProjectUserMutation({onSuccess: invalidateProjectUsers});

    const handleAddConfirm = () => {
        if (selectedUserId) {
            addUserMutation.mutate({
                projectId: String(projectId),
                role: selectedRole,
                userId: selectedUserId,
            });

            setAddingUser(false);
            setSelectedUserId(null);
            setSelectedRole(ProjectRole.Editor);
        }
    };

    const handleAddCancel = () => {
        setAddingUser(false);
        setSelectedUserId(null);
        setSelectedRole(ProjectRole.Editor);
    };

    const selectedUserLabel = availableUsers.find((user) => user.id === selectedUserId)?.email ?? 'Search users...';

    return (
        <Dialog onOpenChange={onClose} open={open}>
            <DialogContent className="max-w-2xl">
                <DialogHeader>
                    <DialogTitle>Project Members</DialogTitle>
                </DialogHeader>

                <Table>
                    <TableHeader>
                        <TableRow>
                            <TableHead>User</TableHead>

                            <TableHead>Role</TableHead>

                            <TableHead>Added</TableHead>

                            {canManageMembers && <TableHead className="w-12" />}
                        </TableRow>
                    </TableHeader>

                    <TableBody>
                        {projectUsers.map((projectUser) => (
                            <TableRow key={projectUser.id}>
                                <TableCell>
                                    <div>
                                        <div className="text-sm font-medium">
                                            {projectUser.user?.email || `User ${projectUser.userId}`}
                                        </div>

                                        {projectUser.user?.firstName && (
                                            <div className="text-xs text-muted-foreground">
                                                {projectUser.user.firstName} {projectUser.user.lastName}
                                            </div>
                                        )}
                                    </div>
                                </TableCell>

                                <TableCell>
                                    {canManageMembers ? (
                                        <Select
                                            onValueChange={(role) =>
                                                updateRoleMutation.mutate({
                                                    projectId: String(projectId),
                                                    role: role as ProjectRole,
                                                    userId: projectUser.userId,
                                                })
                                            }
                                            value={projectUser.projectRole ?? undefined}
                                        >
                                            <SelectTrigger aria-label="Project role" className="w-32">
                                                <SelectValue />
                                            </SelectTrigger>

                                            <SelectContent>
                                                {PROJECT_ROLES.map((role) => (
                                                    <SelectItem key={role} value={role}>
                                                        {role}
                                                    </SelectItem>
                                                ))}
                                            </SelectContent>
                                        </Select>
                                    ) : (
                                        projectUser.projectRole
                                    )}
                                </TableCell>

                                <TableCell>
                                    {projectUser.createdDate
                                        ? new Date(projectUser.createdDate).toLocaleDateString()
                                        : '-'}
                                </TableCell>

                                {canManageMembers && (
                                    <TableCell>
                                        <button
                                            aria-label="Remove user from project"
                                            className="text-destructive hover:text-destructive/80"
                                            onClick={() =>
                                                removeUserMutation.mutate({
                                                    projectId: String(projectId),
                                                    userId: projectUser.userId,
                                                })
                                            }
                                        >
                                            <Trash2Icon className="size-4" />
                                        </button>
                                    </TableCell>
                                )}
                            </TableRow>
                        ))}

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
                                                                value={user.email ?? ''}
                                                            >
                                                                <div>
                                                                    <div className="text-sm">{user.email ?? ''}</div>

                                                                    {user.firstName != null && (
                                                                        <div className="text-xs text-muted-foreground">
                                                                            {user.firstName} {user.lastName ?? ''}
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
                                        onValueChange={(role) => setSelectedRole(role as ProjectRole)}
                                        value={selectedRole}
                                    >
                                        <SelectTrigger aria-label="Project role for new user" className="w-32">
                                            <SelectValue />
                                        </SelectTrigger>

                                        <SelectContent>
                                            {PROJECT_ROLES.map((role) => (
                                                <SelectItem key={role} value={role}>
                                                    {role}
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

                                {canManageMembers && <TableCell />}
                            </TableRow>
                        )}
                    </TableBody>
                </Table>

                {canManageMembers && !addingUser && (
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

export default ProjectUsersDialog;
