import DeleteAlertDialog from '@/components/DeleteAlertDialog';

interface DeleteWorkflowAlertDialogProps {
    onClose: () => void;
    onDelete: () => void;
}

const DeleteWorkflowAlertDialog = ({onClose, onDelete}: DeleteWorkflowAlertDialogProps) => (
    <DeleteAlertDialog
        description="This action cannot be undone. This will permanently delete the workflow."
        onCancel={onClose}
        onDelete={onDelete}
        open
    />
);

export default DeleteWorkflowAlertDialog;
