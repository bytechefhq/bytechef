import {
    DeleteWorkflowRequest,
    UpdateWorkflowRequest,
    WorkflowApi,
    WorkflowModel,
} from '@/shared/middleware/embedded/configuration';
import {useMutation} from '@tanstack/react-query';

interface DeleteWorkflowMutationProps {
    onSuccess?: (result: void, variables: DeleteWorkflowRequest) => void;
    onError?: (error: Error, variables: DeleteWorkflowRequest) => void;
}

export const useDeleteWorkflowMutation = (mutationProps?: DeleteWorkflowMutationProps) =>
    useMutation({
        mutationFn: (request: DeleteWorkflowRequest) => {
            return new WorkflowApi().deleteWorkflow(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface UpdateWorkflowMutationProps {
    onSuccess?: (result: WorkflowModel, variables: UpdateWorkflowRequest) => void;
    onError?: (error: Error, variables: UpdateWorkflowRequest) => void;
}

export const useUpdateWorkflowMutation = (mutationProps?: UpdateWorkflowMutationProps) =>
    useMutation({
        mutationFn: (request: UpdateWorkflowRequest) => {
            return new WorkflowApi().updateWorkflow(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
