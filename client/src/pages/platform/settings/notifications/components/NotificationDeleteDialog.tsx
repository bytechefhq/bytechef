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
import {Notification} from '@/shared/middleware/platform/configuration';
import {XIcon} from 'lucide-react';

interface NotificationDeleteDialogProps {
    closeDeleteDialog: () => void;
    handleDeleteNotification: (notificationId: number) => void;
    isDeleteDialogOpen: boolean;
    selectedNotification: Notification;
}

const NotificationDeleteDialog = ({
    closeDeleteDialog,
    handleDeleteNotification,
    isDeleteDialogOpen,
    selectedNotification,
}: NotificationDeleteDialogProps) => (
    <AlertDialog open={isDeleteDialogOpen}>
        <AlertDialogContent onEscapeKeyDown={closeDeleteDialog}>
            <AlertDialogHeader>
                <AlertDialogTitle>{`Delete ${selectedNotification?.name} notification?`}</AlertDialogTitle>

                <AlertDialogDescription>This action cannot be undone.</AlertDialogDescription>

                <Button className="absolute right-2 top-0" onClick={closeDeleteDialog} size="icon" variant="ghost">
                    <XIcon />

                    <span className="sr-only">Close</span>
                </Button>
            </AlertDialogHeader>

            <AlertDialogFooter>
                <AlertDialogCancel onClick={closeDeleteDialog}>Cancel</AlertDialogCancel>

                <AlertDialogAction
                    className="bg-surface-destructive-primary hover:bg-surface-destructive-primary-hover"
                    onClick={() => selectedNotification && handleDeleteNotification(selectedNotification.id!)}
                >
                    Delete
                </AlertDialogAction>
            </AlertDialogFooter>
        </AlertDialogContent>
    </AlertDialog>
);

export default NotificationDeleteDialog;
