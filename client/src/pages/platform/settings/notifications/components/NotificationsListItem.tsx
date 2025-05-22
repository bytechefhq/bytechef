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
import {useDeleteNotificationMutation} from '@/ee/mutations/notifications.mutations';
import {NotificationKeys} from '@/ee/queries/notifications.queries';
import NotificationDialog from '@/pages/platform/settings/notifications/components/NotificationDialog';
import {Notification} from '@/shared/middleware/platform/configuration';
import {useQueryClient} from '@tanstack/react-query';
import {EllipsisVerticalIcon} from 'lucide-react';
import {useState} from 'react';

const NotificationsListItem = ({notification}: {notification: Notification}) => {
    const [showEditDialog, setShowEditDialog] = useState(false);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);

    const queryClient = useQueryClient();

    const deleteNotificationMutation = useDeleteNotificationMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: NotificationKeys,
            });
        },
    });

    const handleAlertDeleteDialogClick = () => {
        if (notification.id) {
            deleteNotificationMutation.mutate(notification.id);

            setShowDeleteDialog(false);
        }
    };

    return (
        <li className="relative flex items-center justify-between px-2 py-5 hover:bg-gray-50" key={notification.id}>
            <div className="flex-1">
                <span className="text-base">{notification.name}</span>
            </div>

            <div className="flex-1">
                <span className="text-base">
                    {notification.notificationEvents?.map((notificationEvent) => notificationEvent.type).join(', ')}
                </span>
            </div>

            <div className="flex justify-end gap-x-6">
                {notification.lastModifiedDate && notification.lastModifiedBy && (
                    <Tooltip>
                        <TooltipTrigger className="flex items-center text-sm text-gray-500">
                            <span className="text-xs">
                                {`Last modified at ${notification.lastModifiedDate?.toLocaleDateString()} ${notification.lastModifiedDate?.toLocaleTimeString()} by ${notification.lastModifiedBy}`}
                            </span>
                        </TooltipTrigger>

                        <TooltipContent>Last Modified</TooltipContent>
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
                        <AlertDialogTitle>Are you sure you want to delete a notification?</AlertDialogTitle>

                        <AlertDialogDescription>This action cannot be undone.</AlertDialogDescription>
                    </AlertDialogHeader>

                    <AlertDialogFooter>
                        <AlertDialogCancel onClick={() => setShowDeleteDialog(false)}>Cancel</AlertDialogCancel>

                        <AlertDialogAction className="bg-destructive" onClick={handleAlertDeleteDialogClick}>
                            Delete
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>

            {showEditDialog && (
                <NotificationDialog notification={notification} onClose={() => setShowEditDialog(false)} />
            )}
        </li>
    );
};

export default NotificationsListItem;
