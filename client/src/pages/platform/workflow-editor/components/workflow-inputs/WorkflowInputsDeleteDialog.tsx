import DeleteAlertDialog from '@/components/DeleteAlertDialog';
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
}: WorkflowInputsDeleteDialogProps) => {
    const currentInput = workflowInputs?.[currentInputIndex];

    return (
        <DeleteAlertDialog
            description="This action cannot be undone. This will permanently delete the input."
            onCancel={closeDeleteDialog}
            onDelete={() => currentInput && deleteWorkflowInput(currentInput)}
            open={isDeleteDialogOpen}
        />
    );
};

export default WorkflowInputsDeleteDialog;
