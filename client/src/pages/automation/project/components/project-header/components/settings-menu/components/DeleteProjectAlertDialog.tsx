import AlertDialog from '@/components/AlertDialog';

interface DeleteProjectAlertDialogProps {
    isPending?: boolean;
    onClose: () => void;
    onDelete: () => void;
}

const DeleteProjectAlertDialog = ({isPending, onClose, onDelete}: DeleteProjectAlertDialogProps) => (
    <AlertDialog
        ariaLabel="Confirm Project Deletion"
        description="This action cannot be undone. This will permanently delete the project and workflows it contains."
        isPending={isPending}
        onCancel={onClose}
        onConfirm={onDelete}
        open
    />
);

export default DeleteProjectAlertDialog;
