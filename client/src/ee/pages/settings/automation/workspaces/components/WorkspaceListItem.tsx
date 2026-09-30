import Badge from '@/components/Badge/Badge';
import AlertDialog from '@/components/AlertDialog';
import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import WorkspaceDialog from '@/ee/pages/settings/automation/workspaces/components/WorkspaceDialog';
import {useDeleteWorkspaceMutation} from '@/ee/shared/mutations/automation/workspaces.mutations';
import {Workspace} from '@/shared/middleware/automation/configuration';
import {WorkspaceKeys} from '@/shared/queries/automation/workspaces.queries';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {useQueryClient} from '@tanstack/react-query';
import {EllipsisVerticalIcon} from 'lucide-react';
import {useState} from 'react';

interface WorkspaceListItemProps {
    isCurrentWorkspace: boolean;
    onOpen: (workspaceId: number) => void;
    workspace: Workspace;
}

const WorkspaceListItem = ({isCurrentWorkspace, onOpen, workspace}: WorkspaceListItemProps) => {
    const [showEditDialog, setShowEditDialog] = useState(false);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);

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

    const handleAlertDeleteDialogClick = () => {
        if (workspace.id) {
            deleteWorkspaceMutation.mutate(workspace.id);

            setShowDeleteDialog(false);
        }
    };

    const handleWorkspaceClick = () => {
        if (workspace.id) {
            onOpen(workspace.id);
        }
    };

    return (
        <li className="mb-2 rounded border border-border/50" key={workspace.id}>
            <div className="relative flex items-center justify-between rounded-md bg-surface-neutral-primary px-3 py-3 hover:bg-surface-neutral-primary-hover">
                <div className="flex flex-1 items-center gap-2">
                    <button
                        aria-current={isCurrentWorkspace || undefined}
                        className="cursor-pointer text-left text-base font-semibold after:absolute after:inset-0 after:rounded-md"
                        onClick={handleWorkspaceClick}
                        type="button"
                    >
                        {workspace.name}
                    </button>

                    {isCurrentWorkspace && <Badge label="Current" styleType="primary-outline" weight="semibold" />}
                </div>

                <div className="relative z-10 flex items-center justify-end gap-x-6">
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
                                aria-label="Workspace actions"
                                icon={<EllipsisVerticalIcon className="size-4 hover:cursor-pointer" />}
                                size="icon"
                                variant="ghost"
                            />
                        </DropdownMenuTrigger>

                        <DropdownMenuContent align="end">
                            <DropdownMenuItem onClick={() => setShowEditDialog(true)}>Edit</DropdownMenuItem>

                            <DropdownMenuSeparator />

                            <DropdownMenuItem className="text-destructive" onClick={() => setShowDeleteDialog(true)}>
                                Delete
                            </DropdownMenuItem>
                        </DropdownMenuContent>
                    </DropdownMenu>
                </div>
            </div>

            <AlertDialog
                description="This action cannot be undone. This will permanently delete the workspace."
                onCancel={() => setShowDeleteDialog(false)}
                onConfirm={handleAlertDeleteDialogClick}
                open={showDeleteDialog}
            />

            {showEditDialog && <WorkspaceDialog onClose={() => setShowEditDialog(false)} workspace={workspace} />}
        </li>
    );
};

export default WorkspaceListItem;
