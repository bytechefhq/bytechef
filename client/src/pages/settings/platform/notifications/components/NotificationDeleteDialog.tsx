import DeleteAlertDialog from '@/components/DeleteAlertDialog';
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
    <DeleteAlertDialog
        description="This action cannot be undone."
        onCancel={closeDeleteDialog}
        onDelete={() => selectedNotification && handleDeleteNotification(selectedNotification.id!)}
        open={isDeleteDialogOpen}
        title={`Delete ${selectedNotification?.name} notification?`}
    />
);

export default NotificationDeleteDialog;
