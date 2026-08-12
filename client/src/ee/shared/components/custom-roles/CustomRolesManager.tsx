import Button from '@/components/Button/Button';
import {Input} from '@/components/Input/Input';
import {Checkbox} from '@/components/ui/checkbox';
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import {
    useCreateCustomRoleMutation,
    useCustomRolesQuery,
    useDeleteCustomRoleMutation,
    usePermissionScopesQuery,
    useUpdateCustomRoleMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {PencilIcon, Trash2Icon} from 'lucide-react';
import {useState} from 'react';

interface CustomRolesManagerPropsI {
    /**
     * The workspace whose roles are being managed, or `null` for the tenant-global tier. The same value is sent on
     * every mutation, which is what the server tiers authorization on — so this single prop decides both what the
     * list contains and what the caller is claiming to be.
     */
    workspaceId: string | null;
}

const CustomRolesManager = ({workspaceId}: CustomRolesManagerPropsI) => {
    const [actionError, setActionError] = useState<string | null>(null);
    const [description, setDescription] = useState('');
    const [editingRoleId, setEditingRoleId] = useState<string | null>(null);
    const [name, setName] = useState('');
    const [selectedScopes, setSelectedScopes] = useState<string[]>([]);

    const queryClient = useQueryClient();

    // Served from the same PermissionScopeProvider registry the write path validates against, so this editor cannot
    // offer a name the server would reject, nor omit one a module contributed after the client was written.
    const {data: permissionScopesData} = usePermissionScopesQuery({});

    const permissionScopes = permissionScopesData?.permissionScopes ?? [];

    // With a workspace: that workspace's roles plus the tenant-global ones. Without: every role in the tenant.
    const {data: rolesData} = useCustomRolesQuery({workspaceId});

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
                input: {description, name, scopes: selectedScopes, workspaceId},
            });
        } else {
            createCustomRoleMutation.mutate({
                input: {description, name, scopes: selectedScopes, workspaceId},
            });
        }
    };

    const handleEdit = (customRole: (typeof customRoles)[number]) => {
        setActionError(null);
        setEditingRoleId(String(customRole.id));
        setName(customRole.name);
        setDescription(customRole.description ?? '');
        setSelectedScopes([...customRole.scopes]);
    };

    /**
     * A role is editable from the tier that owns it, and only that tier. On a workspace page the global roles are
     * read-only (they may be in use by every other workspace); on the tenant page the workspace-owned ones are. The
     * server enforces the same rule, so this only avoids offering a control that would fail.
     */
    const isOwnedHere = (roleWorkspaceId: string | null | undefined) => (roleWorkspaceId ?? null) === workspaceId;

    return (
        <div className="w-full space-y-6 p-4">
            {actionError && (
                <div className="rounded-md border border-destructive/50 p-3 text-sm text-destructive" role="alert">
                    {actionError}
                </div>
            )}

            <fieldset className="space-y-2 border-0 p-0">
                <legend className="text-sm font-medium">
                    {editingRoleId ? 'Edit role' : workspaceId ? 'Create a role for this workspace' : 'Create a role'}
                </legend>

                <p className="text-xs text-muted-foreground">
                    {workspaceId
                        ? 'Roles created here belong to this workspace. Roles that apply everywhere are managed in tenant settings.'
                        : 'Roles created here apply to every workspace in the tenant and can be assigned anywhere.'}
                </p>

                <div className="flex items-center gap-2">
                    <Input
                        className="max-w-60"
                        onChange={(event) => setName(event.target.value)}
                        placeholder="Role name"
                        value={name}
                    />

                    <Input
                        className="max-w-80"
                        onChange={(event) => setDescription(event.target.value)}
                        placeholder="Description (optional)"
                        value={description}
                    />

                    <Button disabled={!name || selectedScopes.length === 0} onClick={handleSubmit}>
                        {editingRoleId ? 'Save' : 'Create'}
                    </Button>

                    {editingRoleId && (
                        <Button onClick={resetForm} variant="outline">
                            Cancel
                        </Button>
                    )}
                </div>

                <div className="flex flex-wrap gap-3 pt-2">
                    {permissionScopes.map((scope) => (
                        <label className="flex items-center gap-2 text-sm" key={scope}>
                            <Checkbox
                                checked={selectedScopes.includes(scope)}
                                onCheckedChange={() => handleScopeToggle(scope)}
                            />

                            {scope}
                        </label>
                    ))}
                </div>
            </fieldset>

            <Table>
                <TableHeader>
                    <TableRow>
                        <TableHead>Name</TableHead>

                        <TableHead>Scope</TableHead>

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

                            <TableCell>{customRole.workspaceId ? 'One workspace' : 'All workspaces'}</TableCell>

                            <TableCell className="text-xs">{customRole.scopes.join(', ')}</TableCell>

                            <TableCell>
                                {isOwnedHere(customRole.workspaceId) && (
                                    <div className="flex items-center gap-1">
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
                                                    workspaceId,
                                                })
                                            }
                                            variant="ghost"
                                        />
                                    </div>
                                )}
                            </TableCell>
                        </TableRow>
                    ))}
                </TableBody>
            </Table>
        </div>
    );
};

export default CustomRolesManager;
