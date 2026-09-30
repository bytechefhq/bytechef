import AlertDialog from '@/components/AlertDialog';

interface DeleteWorkflowAlertDialogProps {
    onClose: () => void;
    onDelete: () => void;
}

const DeleteWorkflowAlertDialog = ({onClose, onDelete}: DeleteWorkflowAlertDialogProps) => (
    <AlertDialog
        description="This action cannot be undone. This will permanently delete the workflow."
        onCancel={onClose}
        onConfirm={onDelete}
        open
    />
);

export default DeleteWorkflowAlertDialog;
