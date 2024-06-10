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
import {Button} from '@/components/ui/button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import WorkspaceDialog from '@/pages/settings/automation/workspaces/components/WorkspaceDialog';
import {WorkspaceModel} from '@/shared/middleware/automation/configuration';
import {useDeleteWorkspaceMutation} from '@/shared/mutations/automation/workspaces.mutations';
import {WorkspaceKeys} from '@/shared/queries/automation/workspaces.queries';
import {useQueryClient} from '@tanstack/react-query';
import {EllipsisVerticalIcon} from 'lucide-react';
import {useState} from 'react';

interface WorkspaceListItemProps {
    workspace: WorkspaceModel;
}

const WorkspaceListItem = ({workspace}: WorkspaceListItemProps) => {
    const [showEditDialog, setShowEditDialog] = useState(false);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);

    const queryClient = useQueryClient();

    const deleteWorkspaceMutation = useDeleteWorkspaceMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: WorkspaceKeys.workspaces,
            });
        },
    });

    const handleAlertDeleteDialogClick = () => {
        if (workspace.id) {
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
                            <span>
                                {`${workspace.createdDate?.toLocaleDateString()} ${workspace.createdDate?.toLocaleTimeString()}`}
                            </span>
                        </TooltipTrigger>

                        <TooltipContent>Created Date</TooltipContent>
                    </Tooltip>
                )}

                <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                        <Button size="icon" variant="ghost">
                            <EllipsisVerticalIcon className="size-4 hover:cursor-pointer" />
                        </Button>
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

            {showEditDialog && <WorkspaceDialog onClose={() => setShowEditDialog(false)} workspace={workspace} />}
        </li>
    );
};

export default WorkspaceListItem;
