import DeleteAlertDialog from '@/components/DeleteAlertDialog';

const DeleteIntegrationAlertDialog = ({onClose, onDelete}: {onClose: () => void; onDelete: () => void}) => (
    <DeleteAlertDialog
        ariaLabel="Confirm Integration Deletion"
        description="This action cannot be undone. This will permanently delete the integration and workflows it contains."
        onCancel={onClose}
        onDelete={onDelete}
        open
    />
);

export default DeleteIntegrationAlertDialog;
