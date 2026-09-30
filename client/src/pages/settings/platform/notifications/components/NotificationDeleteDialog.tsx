import Button from '@/components/Button/Button';
import DestructiveAlertDialogAction from '@/components/DestructiveAlertDialogAction';
import {
    AlertDialog,
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
                    className="absolute top-0 right-2"
                    icon={<XIcon />}
                    onClick={closeDeleteDialog}
                    size="icon"
                    variant="ghost"
                />
            </AlertDialogHeader>

            <AlertDialogFooter>
                <AlertDialogCancel onClick={closeDeleteDialog}>Cancel</AlertDialogCancel>

                <DestructiveAlertDialogAction
                    onClick={() => selectedNotification && handleDeleteNotification(selectedNotification.id!)}
                >
                    Delete
                </DestructiveAlertDialogAction>
            </AlertDialogFooter>
        </AlertDialogContent>
    </AlertDialog>
);

export default NotificationDeleteDialog;
