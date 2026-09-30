import AlertDialog from '@/components/AlertDialog';
import {Trash2Icon} from 'lucide-react';

interface WorkflowNodeDeleteAlertDialogProps {
    nodeLabel?: string;
    onCancel: () => void;
    onConfirm: () => void;
    open: boolean;
}

const WorkflowNodeDeleteAlertDialog = ({nodeLabel, onCancel, onConfirm, open}: WorkflowNodeDeleteAlertDialogProps) => (
    <AlertDialog
        cancelLabel="Keep node"
        confirmIcon={<Trash2Icon />}
        confirmLabel="Delete node"
        description="This action cannot be undone. This will permanently delete the node and properties it contains."
        onCancel={onCancel}
        onConfirm={onConfirm}
        open={open}
        title={nodeLabel ? `Delete node ${nodeLabel}?` : 'Delete node?'}
    />
);

export default WorkflowNodeDeleteAlertDialog;
