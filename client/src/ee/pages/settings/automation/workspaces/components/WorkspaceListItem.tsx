import Button from '@/components/Button/Button';
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
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import WorkspaceDialog from '@/ee/pages/settings/automation/workspaces/components/WorkspaceDialog';
import WorkspaceUsersDialog from '@/ee/pages/settings/automation/workspaces/components/WorkspaceUsersDialog';
import {useDeleteWorkspaceMutation} from '@/ee/shared/mutations/automation/workspaces.mutations';
import {Workspace} from '@/shared/middleware/automation/configuration';
import {WorkspaceKeys} from '@/shared/queries/automation/workspaces.queries';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {useQueryClient} from '@tanstack/react-query';
import {EllipsisVerticalIcon} from 'lucide-react';
import {useState} from 'react';

interface WorkspaceListItemProps {
    workspace: Workspace;
}

/**
 * A row of the tenant-wide workspace list. There is deliberately no per-row permission check here: the enclosing
 * `/automation/settings/workspaces` route is `PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN]}`, so the only user
 * who ever reaches this component is a tenant admin, and a tenant admin is an admin of every workspace by definition.
 * Per-row role checks would also cost one permission query per workspace, each with a distinct workspaceId that
 * react-query cannot dedupe, for an answer that is unconditionally true here.
 *
 * Cross-workspace administration is what makes this screen admin-only; the member-management path for a workspace admin
 * who is not a tenant admin is `settings/users`, which gates on the `WORKSPACE_MEMBER_MANAGE` scope. If this route is
 * ever opened to non-admins, gating has to be added back as scope checks (`useHasWorkspaceScope`), never as role checks:
 * a custom-role membership resolves to no built-in role at all and would be denied.
 */
const WorkspaceListItem = ({workspace}: WorkspaceListItemProps) => {
    const [showEditDialog, setShowEditDialog] = useState(false);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [showWorkspaceUsersDialog, setShowWorkspaceUsersDialog] = useState(false);

    const account = useAuthenticationStore((state) => state.account);

    const queryClient = useQueryClient();

    const deleteWorkspaceMutation = useDeleteWorkspaceMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: WorkspaceKeys.workspaces,
            });

            if (account) {
                queryClient.refetchQueries({
                    queryKey: WorkspaceKeys.userWorkspaces(account.id!),
                });
            }
        },
    });

    // Names the workspace the row belongs to. The copy used to read "This will permanently delete the connection" on a
    // dialog that deletes a workspace and every project, connection and deployment inside it.
    const deleteConfirmationMessage =
        `This action cannot be undone. This will permanently delete the workspace ${workspace.name} ` +
        `and everything in it.`;

    const handleAlertDeleteDialogClick = () => {
        if (workspace.id) {
            deleteWorkspaceMutation.mutate(workspace.id);

            setShowDeleteDialog(false);
        }
    };

    return (
        <li className="mb-2 rounded border border-border/50" key={workspace.id}>
            <div className="flex items-center justify-between rounded-md bg-surface-neutral-primary px-3 py-3 hover:bg-surface-neutral-primary-hover">
                <div className="flex-1">
                    <span className="text-base font-semibold">{workspace.name}</span>
                </div>

                <div className="flex items-center justify-end gap-x-6">
                    {workspace.createdDate && (
                        <Tooltip>
                            <TooltipTrigger className="flex items-center text-sm text-content-neutral-secondary">
                                <span className="text-xs">
                                    {`Created at ${workspace.createdDate?.toLocaleDateString()} ${workspace.createdDate?.toLocaleTimeString()}`}
                                </span>
                            </TooltipTrigger>

                            <TooltipContent>Created Date</TooltipContent>
                        </Tooltip>
                    )}

                    <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                            <Button
                                aria-label={`Workspace actions for ${workspace.name}`}
                                icon={<EllipsisVerticalIcon className="size-4 hover:cursor-pointer" />}
                                size="icon"
                                variant="ghost"
                            />
                        </DropdownMenuTrigger>

                        <DropdownMenuContent align="end">
                            <DropdownMenuItem onClick={() => setShowEditDialog(true)}>Edit</DropdownMenuItem>

                            <DropdownMenuItem onClick={() => setShowWorkspaceUsersDialog(true)}>
                                Members
                            </DropdownMenuItem>

                            <DropdownMenuSeparator />

                            <DropdownMenuItem className="text-destructive" onClick={() => setShowDeleteDialog(true)}>
                                Delete
                            </DropdownMenuItem>
                        </DropdownMenuContent>
                    </DropdownMenu>
                </div>
            </div>

            {showDeleteDialog && (
                <AlertDialog open={showDeleteDialog}>
                    <AlertDialogContent>
                        <AlertDialogHeader>
                            <AlertDialogTitle>Delete this workspace?</AlertDialogTitle>

                            <AlertDialogDescription>{deleteConfirmationMessage}</AlertDialogDescription>
                        </AlertDialogHeader>

                        <AlertDialogFooter>
                            <AlertDialogCancel onClick={() => setShowDeleteDialog(false)}>Cancel</AlertDialogCancel>

                            <AlertDialogAction className="bg-destructive" onClick={handleAlertDeleteDialogClick}>
                                Delete
                            </AlertDialogAction>
                        </AlertDialogFooter>
                    </AlertDialogContent>
                </AlertDialog>
            )}

            {showEditDialog && <WorkspaceDialog onClose={() => setShowEditDialog(false)} workspace={workspace} />}

            {showWorkspaceUsersDialog && (
                <WorkspaceUsersDialog
                    onClose={() => setShowWorkspaceUsersDialog(false)}
                    open={showWorkspaceUsersDialog}
                    workspaceId={workspace.id!}
                />
            )}
        </li>
    );
};

export default WorkspaceListItem;
