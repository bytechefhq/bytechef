import AlertDialog from '@/components/AlertDialog';

const DeleteIntegrationAlertDialog = ({onClose, onDelete}: {onClose: () => void; onDelete: () => void}) => (
    <AlertDialog
        ariaLabel="Confirm Integration Deletion"
        description="This action cannot be undone. This will permanently delete the integration and workflows it contains."
        onCancel={onClose}
        onConfirm={onDelete}
        open
    />
);

export default DeleteIntegrationAlertDialog;
