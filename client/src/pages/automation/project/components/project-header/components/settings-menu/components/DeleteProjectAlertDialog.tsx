import DeleteAlertDialog from '@/components/DeleteAlertDialog';

const DeleteProjectAlertDialog = ({onClose, onDelete}: {onClose: () => void; onDelete: () => void}) => (
    <DeleteAlertDialog
        ariaLabel="Confirm Project Deletion"
        description="This action cannot be undone. This will permanently delete the project and workflows it contains."
        onCancel={onClose}
        onDelete={onDelete}
        open
    />
);

export default DeleteProjectAlertDialog;
