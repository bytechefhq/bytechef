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
import {useHasWorkspaceRole} from '@/shared/hooks/useHasWorkspaceRole';
import {useLoadWorkspacePermissions} from '@/shared/hooks/useLoadWorkspacePermissions';
import {Workspace} from '@/shared/middleware/automation/configuration';
import {WorkspaceRole} from '@/shared/middleware/graphql';
import {WorkspaceKeys} from '@/shared/queries/automation/workspaces.queries';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {useQueryClient} from '@tanstack/react-query';
import {EllipsisVerticalIcon} from 'lucide-react';
import {useState} from 'react';

interface WorkspaceListItemProps {
    workspace: Workspace;
}

const WorkspaceListItem = ({workspace}: WorkspaceListItemProps) => {
    const [showEditDialog, setShowEditDialog] = useState(false);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [showWorkspaceUsersDialog, setShowWorkspaceUsersDialog] = useState(false);

    const account = useAuthenticationStore((state) => state.account);

    // Prime the permission store so useHasWorkspaceRole below has a populated role for this workspace. The hook is
    // idempotent — multiple WorkspaceListItems calling it in parallel share the same query.
    useLoadWorkspacePermissions(workspace.id);

    const canManageMembers = useHasWorkspaceRole(workspace.id, WorkspaceRole.Admin);
    const canViewMembers = useHasWorkspaceRole(workspace.id, WorkspaceRole.Viewer);

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

    const handleAlertDeleteDialogClick = () => {
        if (canManageMembers && workspace.id) {
            deleteWorkspaceMutation.mutate(workspace.id);

            setShowDeleteDialog(false);
        }
    };

    return (
        <li className="relative flex items-center justify-between px-2 py-5 hover:bg-gray-50" key={workspace.id}>
            <div className="flex-1">
                <span className="text-base">{workspace.name}</span>
            </div>

            <div className="flex justify-end gap-x-6">
                {workspace.createdDate && (
                    <Tooltip>
                        <TooltipTrigger className="flex items-center text-sm text-gray-500">
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
                            icon={<EllipsisVerticalIcon className="size-4 hover:cursor-pointer" />}
                            size="icon"
                            variant="ghost"
                        />
                    </DropdownMenuTrigger>

                    <DropdownMenuContent align="end">
                        {canManageMembers && (
                            <DropdownMenuItem onClick={() => setShowEditDialog(true)}>Edit</DropdownMenuItem>
                        )}

                        {canViewMembers && (
                            <DropdownMenuItem onClick={() => setShowWorkspaceUsersDialog(true)}>
                                Members
                            </DropdownMenuItem>
                        )}

                        {canManageMembers && <DropdownMenuSeparator />}

                        {canManageMembers && (
                            <DropdownMenuItem className="text-destructive" onClick={() => setShowDeleteDialog(true)}>
                                Delete
                            </DropdownMenuItem>
                        )}
                    </DropdownMenuContent>
                </DropdownMenu>
            </div>

            {canManageMembers && showDeleteDialog && (
                <AlertDialog open={showDeleteDialog}>
                    <AlertDialogContent>
                        <AlertDialogHeader>
                            <AlertDialogTitle>Are you absolutely sure?</AlertDialogTitle>

                            <AlertDialogDescription>
                                This action cannot be undone. This will permanently delete the connection.
                            </AlertDialogDescription>
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
