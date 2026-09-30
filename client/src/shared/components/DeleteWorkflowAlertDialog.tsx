import AlertDialog from '@/components/AlertDialog';

interface DeleteWorkflowAlertDialogProps {
    isPending?: boolean;
    onClose: () => void;
    onDelete: () => void;
}

const DeleteWorkflowAlertDialog = ({isPending, onClose, onDelete}: DeleteWorkflowAlertDialogProps) => (
    <AlertDialog
        description="This action cannot be undone. This will permanently delete the workflow."
        isPending={isPending}
        onCancel={onClose}
        onConfirm={onDelete}
        open
    />
);

export default DeleteWorkflowAlertDialog;
