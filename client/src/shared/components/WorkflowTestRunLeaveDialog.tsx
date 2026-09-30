import AlertDialog from '@/components/AlertDialog';

interface WorkflowTestRunLeaveDialogProps {
    onCancel: () => void;
    onConfirm: () => void;
    open: boolean;
}

const WorkflowTestRunLeaveDialog = ({onCancel, onConfirm, open}: WorkflowTestRunLeaveDialogProps) => (
    <AlertDialog
        confirmButtonVariant="default"
        confirmLabel="Confirm"
        description="A test run is currently in progress. Do you really want to leave this page? The workflow execution will be stopped."
        onCancel={onCancel}
        onConfirm={onConfirm}
        open={open}
        title="Workflow is running"
    />
);

export default WorkflowTestRunLeaveDialog;
