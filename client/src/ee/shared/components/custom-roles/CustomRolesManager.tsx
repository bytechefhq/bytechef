import Button from '@/components/Button/Button';
import {Input} from '@/components/Input/Input';
import {Checkbox} from '@/components/ui/checkbox';
import {
    Dialog,
    DialogClose,
    DialogCloseButton,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {
    useCreateCustomRoleMutation,
    useCustomRolesQuery,
    useDeleteCustomRoleMutation,
    usePermissionScopesQuery,
    useUpdateCustomRoleMutation,
} from '@/shared/middleware/graphql';
import {getRoleLabel} from '@/shared/util/role-utils';
import {useQueryClient} from '@tanstack/react-query';
import {PencilIcon, Trash2Icon} from 'lucide-react';
import {useState} from 'react';

const CustomRolesManager = () => {
    const [actionError, setActionError] = useState<string | null>(null);
    const [description, setDescription] = useState('');
    const [dialogOpen, setDialogOpen] = useState(false);
    const [editingRoleId, setEditingRoleId] = useState<string | null>(null);
    const [name, setName] = useState('');
    const [selectedScopes, setSelectedScopes] = useState<string[]>([]);

    const queryClient = useQueryClient();

    // Served from the same PermissionScopeProvider registry the write path validates against, so this editor cannot
    // offer a name the server would reject, nor omit one a module contributed after the client was written.
    const {data: permissionScopesData} = usePermissionScopesQuery({});

    const permissionScopes = permissionScopesData?.permissionScopes ?? [];

    // Every custom role in the tenant — roles are tenant-global and assignable in any workspace.
    const {data: rolesData} = useCustomRolesQuery({});

    const customRoles = rolesData?.customRoles ?? [];

    const resetForm = () => {
        setActionError(null);
        setDescription('');
        setEditingRoleId(null);
        setName('');
        setSelectedScopes([]);
    };

    const invalidateCustomRoles = () => {
        resetForm();
        setDialogOpen(false);
        queryClient.invalidateQueries({queryKey: ['CustomRoles']});
    };

    const onActionError = (error: Error) => setActionError(error.message);

    const createCustomRoleMutation = useCreateCustomRoleMutation({
        onError: onActionError,
        onSuccess: invalidateCustomRoles,
    });

    const updateCustomRoleMutation = useUpdateCustomRoleMutation({
        onError: onActionError,
        onSuccess: invalidateCustomRoles,
    });

    const deleteCustomRoleMutation = useDeleteCustomRoleMutation({
        onError: onActionError,
        onSuccess: invalidateCustomRoles,
    });

    const handleScopeToggle = (scope: string) => {
        setSelectedScopes((current) =>
            current.includes(scope) ? current.filter((selected) => selected !== scope) : [...current, scope]
        );
    };

    const handleSubmit = () => {
        if (!name || selectedScopes.length === 0) {
            return;
        }

        if (editingRoleId) {
            updateCustomRoleMutation.mutate({
                id: editingRoleId,
                input: {description, name, scopes: selectedScopes},
            });
        } else {
            createCustomRoleMutation.mutate({
                input: {description, name, scopes: selectedScopes},
            });
        }
    };

    const handleEdit = (customRole: (typeof customRoles)[number]) => {
        setActionError(null);
        setEditingRoleId(String(customRole.id));
        setName(customRole.name);
        setDescription(customRole.description ?? '');
        setSelectedScopes([...customRole.scopes]);
        setDialogOpen(true);
    };

    const handleCreate = () => {
        resetForm();
        setDialogOpen(true);
    };

    // Closing discards whatever was typed. Keeping it would mean the next "Create Role" reopens someone's abandoned
    // edit, and worse, reopens it still carrying editingRoleId — so Save would overwrite a role they meant to leave.
    const handleDialogOpenChange = (open: boolean) => {
        setDialogOpen(open);

        if (!open) {
            resetForm();
        }
    };

    return (
        <LayoutContainer
            header={
                <Header
                    centerTitle
                    description="Roles apply to every workspace in the tenant and can be assigned to any member."
                    position="main"
                    right={<Button onClick={handleCreate}>Create Role</Button>}
                    title="Custom Roles"
                />
            }
            leftSidebarOpen={false}
        >
            <div className="w-full space-y-6 p-4">
                {actionError && (
                    <div className="rounded-md border border-destructive/50 p-3 text-sm text-destructive" role="alert">
                        {actionError}
                    </div>
                )}

                <Dialog onOpenChange={handleDialogOpenChange} open={dialogOpen}>
                    <DialogContent className="max-w-3xl">
                        <DialogHeader>
                            <DialogTitle>{editingRoleId ? 'Edit Role' : 'Create Role'}</DialogTitle>

                            <DialogCloseButton />
                        </DialogHeader>

                        <div className="space-y-4">
                            <div className="space-y-2">
                                <label className="text-sm font-medium">Name</label>

                                <Input
                                    onChange={(event) => setName(event.target.value)}
                                    placeholder="Role name"
                                    value={name}
                                />
                            </div>

                            <div className="space-y-2">
                                <label className="text-sm font-medium">Description</label>

                                <Input
                                    onChange={(event) => setDescription(event.target.value)}
                                    placeholder="Description (optional)"
                                    value={description}
                                />
                            </div>

                            <fieldset className="space-y-2 border-0 p-0">
                                <legend className="text-sm font-medium">Permissions</legend>

                                <div className="grid max-h-72 grid-cols-2 gap-x-6 gap-y-2 overflow-y-auto pt-1">
                                    {permissionScopes.map((scope) => (
                                        <label className="flex items-center gap-2 text-sm" key={scope}>
                                            <Checkbox
                                                checked={selectedScopes.includes(scope)}
                                                onCheckedChange={() => handleScopeToggle(scope)}
                                            />

                                            {getRoleLabel(scope)}
                                        </label>
                                    ))}
                                </div>
                            </fieldset>
                        </div>

                        <DialogFooter>
                            <DialogClose asChild>
                                <Button variant="outline">Cancel</Button>
                            </DialogClose>

                            <Button disabled={!name || selectedScopes.length === 0} onClick={handleSubmit}>
                                {editingRoleId ? 'Save' : 'Create'}
                            </Button>
                        </DialogFooter>
                    </DialogContent>
                </Dialog>

                <Table>
                    <TableHeader>
                        <TableRow>
                            <TableHead>Name</TableHead>

                            <TableHead>Permissions</TableHead>

                            <TableHead />
                        </TableRow>
                    </TableHeader>

                    <TableBody>
                        {customRoles.map((customRole) => (
                            <TableRow key={customRole.id}>
                                <TableCell>
                                    <div>{customRole.name}</div>

                                    {customRole.description && (
                                        <div className="text-xs text-muted-foreground">{customRole.description}</div>
                                    )}
                                </TableCell>

                                <TableCell className="text-xs">
                                    {customRole.scopes.map(getRoleLabel).join(', ')}
                                </TableCell>

                                <TableCell className="text-right">
                                    <div className="flex items-center justify-end gap-1">
                                        <Button
                                            icon={<PencilIcon />}
                                            onClick={() => handleEdit(customRole)}
                                            variant="ghost"
                                        />

                                        <Button
                                            icon={<Trash2Icon className="text-destructive" />}
                                            onClick={() =>
                                                deleteCustomRoleMutation.mutate({
                                                    id: String(customRole.id),
                                                })
                                            }
                                            variant="ghost"
                                        />
                                    </div>
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </div>
        </LayoutContainer>
    );
};

export default CustomRolesManager;
