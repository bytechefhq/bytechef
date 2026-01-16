import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from '@/components/ui/alert-dialog';

interface WorkflowTestRunLeaveDialogProps {
    onCancel: () => void;
    onConfirm: () => void;
    open: boolean;
}

const WorkflowTestRunLeaveDialog = ({onCancel, onConfirm, open}: WorkflowTestRunLeaveDialogProps) => (
    <AlertDialog open={open}>
        <AlertDialogContent>
            <AlertDialogHeader>
                <AlertDialogTitle>Workflow is running</AlertDialogTitle>

                <AlertDialogDescription>
                    A test run is currently in progress. Do you really want to leave this page? The workflow execution
                    will be stopped.
                </AlertDialogDescription>
            </AlertDialogHeader>

            <AlertDialogFooter>
                <AlertDialogCancel onClick={onCancel}>Cancel</AlertDialogCancel>

                <AlertDialogAction
                    className="bg-surface-brand-primary text-content-onsurface-primary hover:bg-surface-brand-primary-hover active:bg-surface-brand-primary-active"
                    onClick={onConfirm}
                >
                    Confirm
                </AlertDialogAction>
            </AlertDialogFooter>
        </AlertDialogContent>
    </AlertDialog>
);

export default WorkflowTestRunLeaveDialog;
