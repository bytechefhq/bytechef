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
import {Notification} from '@/shared/middleware/platform/notification';
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

                <Button
                    aria-label="Close"
                    className="absolute right-2 top-0"
                    icon={<XIcon />}
                    onClick={closeDeleteDialog}
                    size="icon"
                    variant="ghost"
                />
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
