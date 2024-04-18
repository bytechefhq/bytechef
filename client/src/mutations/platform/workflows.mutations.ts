import {UpdateWorkflowRequest, WorkflowApi, WorkflowModel} from '@/middleware/platform/configuration';
import {useMutation} from '@tanstack/react-query';

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
