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
import {WorkflowInput} from '@/shared/middleware/platform/configuration';

interface WorkflowInputsDeleteDialogProps {
    closeDeleteDialog: () => void;
    currentInputIndex: number;
    deleteWorkflowInput: (input: WorkflowInput) => void;
    isDeleteDialogOpen: boolean;
    workflowInputs: WorkflowInput[];
}

const WorkflowInputsDeleteDialog = ({
    closeDeleteDialog,
    currentInputIndex,
    deleteWorkflowInput,
    isDeleteDialogOpen,
    workflowInputs,
}: WorkflowInputsDeleteDialogProps) => (
    <AlertDialog open={isDeleteDialogOpen}>
        <AlertDialogContent>
            <AlertDialogHeader>
                <AlertDialogTitle>Are you absolutely sure?</AlertDialogTitle>

                <AlertDialogDescription>
                    This action cannot be undone. This will permanently delete the input.
                </AlertDialogDescription>
            </AlertDialogHeader>

            <AlertDialogFooter>
                <AlertDialogCancel onClick={() => closeDeleteDialog()}>Cancel</AlertDialogCancel>

                {workflowInputs?.[currentInputIndex] && (
                    <AlertDialogAction
                        className="bg-surface-destructive-primary shadow-none hover:bg-surface-destructive-primary-hover active:bg-surface-destructive-primary-active"
                        onClick={() => deleteWorkflowInput(workflowInputs![currentInputIndex])}
                    >
                        Delete
                    </AlertDialogAction>
                )}
            </AlertDialogFooter>
        </AlertDialogContent>
    </AlertDialog>
);

export default WorkflowInputsDeleteDialog;
