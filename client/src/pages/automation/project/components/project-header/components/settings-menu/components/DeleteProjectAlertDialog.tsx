import AlertDialog from '@/components/AlertDialog';

const DeleteProjectAlertDialog = ({onClose, onDelete}: {onClose: () => void; onDelete: () => void}) => (
    <AlertDialog
        ariaLabel="Confirm Project Deletion"
        description="This action cannot be undone. This will permanently delete the project and workflows it contains."
        onCancel={onClose}
        onConfirm={onDelete}
        open
    />
);

export default DeleteProjectAlertDialog;
