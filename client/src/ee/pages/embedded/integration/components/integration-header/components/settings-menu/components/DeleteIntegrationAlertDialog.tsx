import AlertDialog from '@/components/AlertDialog';

interface DeleteIntegrationAlertDialogProps {
    isPending?: boolean;
    onClose: () => void;
    onDelete: () => void;
}

const DeleteIntegrationAlertDialog = ({isPending, onClose, onDelete}: DeleteIntegrationAlertDialogProps) => (
    <AlertDialog
        ariaLabel="Confirm Integration Deletion"
        description="This action cannot be undone. This will permanently delete the integration and workflows it contains."
        isPending={isPending}
        onCancel={onClose}
        onConfirm={onDelete}
        open
    />
);

export default DeleteIntegrationAlertDialog;
