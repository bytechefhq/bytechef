import Button from '@/components/Button/Button';
import DeleteAlertDialog from '@/components/DeleteAlertDialog';
import EmptyList from '@/components/EmptyList';
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
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import BuiltInRoles from '@/ee/shared/components/custom-roles/components/BuiltInRoles';
import PermissionScopeSummary from '@/ee/shared/components/custom-roles/components/PermissionScopeSummary';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {
    useCreateCustomRoleMutation,
    useCustomRolesQuery,
    useDeleteCustomRoleMutation,
    usePermissionScopeGroupsQuery,
    useUpdateCustomRoleMutation,
} from '@/shared/middleware/graphql';
import {invalidateMyPermissionQueries} from '@/shared/queries/permissions.queries';
import {getRoleLabel, getScopeActionLabel} from '@/shared/util/role-utils';
import {useQueryClient} from '@tanstack/react-query';
import {Loader2Icon, PencilIcon, ShieldCheckIcon, Trash2Icon} from 'lucide-react';
import {useId, useState} from 'react';

const BUILT_IN_ROLES_TAB = 'built-in';
const CUSTOM_ROLES_TAB = 'custom';

const CustomRolesManager = () => {
    const [actionError, setActionError] = useState<string | null>(null);
    const [activeTab, setActiveTab] = useState(BUILT_IN_ROLES_TAB);
    const [description, setDescription] = useState('');
    const [dialogOpen, setDialogOpen] = useState(false);
    const [editingRoleId, setEditingRoleId] = useState<string | null>(null);
    const [name, setName] = useState('');
    const [pendingDeletionRoleId, setPendingDeletionRoleId] = useState<string | null>(null);
    const [selectedScopes, setSelectedScopes] = useState<string[]>([]);

    const descriptionInputId = useId();
    const nameInputId = useId();

    const queryClient = useQueryClient();

    // Served from the same PermissionScopeProvider registry the write path validates against, so this editor cannot
    // offer a name the server would reject, nor omit one a module contributed after the client was written. The
    // grouping is the registry's too — the module a scope belongs to is the enum declaring it, which a client cannot
    // recover from the scope name (WORKSPACE_MEMBER_MANAGE belongs to Workspace, not to a "Workspace Member" module).
    const {data: permissionScopeGroupsData} = usePermissionScopeGroupsQuery({});

    const permissionScopeGroups = permissionScopeGroupsData?.permissionScopeGroups ?? [];

    // Every custom role in the tenant — roles are tenant-global and assignable in any workspace.
    const {data: rolesData, isLoading: customRolesLoading} = useCustomRolesQuery({});

    const customRoles = rolesData?.customRoles ?? [];

    // Resolved from the id rather than stashing the whole role in state, so the confirmation cannot name a role the
    // refetched list no longer contains -- deleting it elsewhere simply closes the dialog.
    const pendingDeletionRole = customRoles.find((customRole) => String(customRole.id) === pendingDeletionRoleId);

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
        setPendingDeletionRoleId(null);
        queryClient.invalidateQueries({queryKey: ['CustomRoles']});

        // Editing a role changes the scope set of everyone holding it, the operator included, and their own scopes are
        // cached separately from this list. Without this the controls gated on them stay as they were until a reload.
        invalidateMyPermissionQueries(queryClient);
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

    // Deleting a role is not undoable and silently strips its scopes from every member assigned it, so it gets the same
    // confirmation every other destructive action in settings has rather than firing on the first click.
    const handleDeleteConfirm = () => {
        if (!pendingDeletionRoleId) {
            return;
        }

        deleteCustomRoleMutation.mutate({id: pendingDeletionRoleId});

        setPendingDeletionRoleId(null);
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
                    description="What a workspace member may do. Built-in roles cover the common tiers; custom roles cover the rest."
                    position="main"
                    right={
                        activeTab === CUSTOM_ROLES_TAB &&
                        customRoles.length > 0 && <Button onClick={handleCreate}>Create Role</Button>
                    }
                    title="Roles"
                />
            }
            leftSidebarOpen={false}
        >
            <div className="flex w-4/5 flex-col">
                {actionError && (
                    <div
                        className="m-4 rounded-md border border-destructive/50 p-3 text-sm text-destructive"
                        role="alert"
                    >
                        {actionError}
                    </div>
                )}

                <Dialog onOpenChange={handleDialogOpenChange} open={dialogOpen}>
                    <DialogContent className="max-w-3xl">
                        <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                            <DialogTitle>{editingRoleId ? 'Edit Role' : 'Create Role'}</DialogTitle>

                            <DialogCloseButton />
                        </DialogHeader>

                        <div className="space-y-4">
                            <div className="space-y-2">
                                <label className="text-sm font-medium" htmlFor={nameInputId}>
                                    Name
                                </label>

                                <Input
                                    id={nameInputId}
                                    onChange={(event) => setName(event.target.value)}
                                    placeholder="Role name"
                                    value={name}
                                />
                            </div>

                            <div className="space-y-2">
                                <label className="text-sm font-medium" htmlFor={descriptionInputId}>
                                    Description
                                </label>

                                <Input
                                    id={descriptionInputId}
                                    onChange={(event) => setDescription(event.target.value)}
                                    placeholder="Description (optional)"
                                    value={description}
                                />
                            </div>

                            <fieldset className="space-y-2 border-0 p-0">
                                <legend className="text-sm font-medium">Permissions</legend>

                                <div className="max-h-72 space-y-4 overflow-y-auto pt-1">
                                    {permissionScopeGroups.map((permissionScopeGroup) => (
                                        <div key={permissionScopeGroup.name}>
                                            <h4 className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">
                                                {getRoleLabel(permissionScopeGroup.name)}
                                            </h4>

                                            <div className="grid grid-cols-2 gap-x-6 gap-y-2 pt-2">
                                                {permissionScopeGroup.scopes.map((scope) => (
                                                    <label className="flex items-center gap-2 text-sm" key={scope}>
                                                        <Checkbox
                                                            checked={selectedScopes.includes(scope)}
                                                            onCheckedChange={() => handleScopeToggle(scope)}
                                                        />

                                                        {getScopeActionLabel(permissionScopeGroup.name, scope)}
                                                    </label>
                                                ))}
                                            </div>
                                        </div>
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

                <Tabs className="flex flex-1 flex-col p-4" onValueChange={setActiveTab} value={activeTab}>
                    <TabsList>
                        <TabsTrigger value={BUILT_IN_ROLES_TAB}>Built-in Roles</TabsTrigger>

                        <TabsTrigger value={CUSTOM_ROLES_TAB}>Custom Roles</TabsTrigger>
                    </TabsList>

                    <TabsContent className="flex flex-1 flex-col pt-2" value={CUSTOM_ROLES_TAB}>
                        <p className="text-sm text-content-neutral-secondary">
                            Tenant-global: defined once, assignable to any member of any workspace.
                        </p>

                        {customRolesLoading ? (
                            <div className="my-auto flex items-center justify-center gap-2 text-sm text-muted-foreground">
                                <Loader2Icon aria-hidden className="size-4 animate-spin" />
                                Loading roles…
                            </div>
                        ) : customRoles.length === 0 ? (
                            <EmptyList
                                button={<Button onClick={handleCreate}>Create Role</Button>}
                                className="my-auto"
                                icon={<ShieldCheckIcon className="size-24 text-gray-300" />}
                                message="Create one to grant a member a set of permissions that no built-in role covers."
                                title="No Custom Roles"
                            />
                        ) : (
                            <div className="pt-3">
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
                                                        <div className="text-xs text-muted-foreground">
                                                            {customRole.description}
                                                        </div>
                                                    )}
                                                </TableCell>

                                                <TableCell>
                                                    <PermissionScopeSummary
                                                        permissionScopeGroups={permissionScopeGroups}
                                                        scopes={customRole.scopes}
                                                    />
                                                </TableCell>

                                                <TableCell className="text-right">
                                                    <div className="flex items-center justify-end gap-0">
                                                        <Button
                                                            aria-label={`Edit the ${customRole.name} role`}
                                                            icon={<PencilIcon />}
                                                            onClick={() => handleEdit(customRole)}
                                                            variant="ghost"
                                                        />

                                                        <Button
                                                            aria-label={`Delete the ${customRole.name} role`}
                                                            icon={<Trash2Icon className="text-destructive" />}
                                                            onClick={() =>
                                                                setPendingDeletionRoleId(String(customRole.id))
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
                        )}
                    </TabsContent>

                    <TabsContent className="pt-2" value={BUILT_IN_ROLES_TAB}>
                        <BuiltInRoles permissionScopeGroups={permissionScopeGroups} />
                    </TabsContent>
                </Tabs>

                <DeleteAlertDialog
                    onCancel={() => setPendingDeletionRoleId(null)}
                    onDelete={handleDeleteConfirm}
                    open={!!pendingDeletionRole}
                />
            </div>
        </LayoutContainer>
    );
};

export default CustomRolesManager;
