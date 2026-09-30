import AlertDialog from '@/components/AlertDialog';
import {Notification} from '@/shared/middleware/platform/notification';

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
    <AlertDialog
        description="This action cannot be undone."
        onCancel={closeDeleteDialog}
        onConfirm={() => selectedNotification && handleDeleteNotification(selectedNotification.id!)}
        open={isDeleteDialogOpen}
        title={`Delete ${selectedNotification?.name} notification?`}
    />
);

export default NotificationDeleteDialog;
