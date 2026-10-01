import AlertDialog from '@/components/AlertDialog';

interface DeleteAgentAlertDialogProps {
    agentTitle: string;
    onClose: () => void;
    onDelete: () => void;
}

const DeleteAgentAlertDialog = ({agentTitle, onClose, onDelete}: DeleteAgentAlertDialogProps) => (
    <AlertDialog
        ariaLabel="Confirm Agent Deletion"
        description={`This action cannot be undone. This will permanently delete the agent ${agentTitle}.`}
        onCancel={onClose}
        onConfirm={onDelete}
        open
    />
);

export default DeleteAgentAlertDialog;
