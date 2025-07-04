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
import AppEventDialog from '@/ee/pages/embedded/app-events/components/AppEventDialog';
import {AppEvent} from '@/ee/shared/middleware/embedded/configuration';
import {useDeleteAppEventMutation} from '@/ee/shared/mutations/embedded/appEvents.mutations';
import {AppEventKeys} from '@/ee/shared/queries/embedded/appEvents.queries';
import {useQueryClient} from '@tanstack/react-query';
import {EllipsisVerticalIcon} from 'lucide-react';
import {useState} from 'react';

interface AppEventListItemProps {
    appEvent: AppEvent;
}

const AppEventListItem = ({appEvent}: AppEventListItemProps) => {
    const [showEditDialog, setShowEditDialog] = useState(false);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);

    const queryClient = useQueryClient();

    const deleteAppEventMutation = useDeleteAppEventMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: AppEventKeys.appEvents,
            });
        },
    });

    const handleAlertDeleteDialogClick = () => {
        if (appEvent.id) {
            deleteAppEventMutation.mutate(appEvent.id);

            setShowDeleteDialog(false);
        }
    };

    return (
        <li className="relative flex items-center justify-between px-2 py-5 hover:bg-gray-50" key={appEvent.id}>
            <div className="flex-1">
                <span className="text-base">{appEvent.name}</span>
            </div>

            <div className="flex justify-end gap-x-6">
                {appEvent.createdDate && (
                    <Tooltip>
                        <TooltipTrigger className="flex items-center text-sm text-gray-500">
                            <span>
                                {`${appEvent.createdDate?.toLocaleDateString()} ${appEvent.createdDate?.toLocaleTimeString()}`}
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

            {showEditDialog && <AppEventDialog appEvent={appEvent} onClose={() => setShowEditDialog(false)} />}
        </li>
    );
};

export default AppEventListItem;
