import {
    GetWorkflowNodeOutputsRequest,
    WorkflowNodeOutputApi,
    WorkflowNodeOutputModel,
} from '@/middleware/platform/configuration';
import {useMutation} from '@tanstack/react-query';

interface UpdateWorkflowNodeOutputsMutationProps {
    onSuccess?: (result: WorkflowNodeOutputModel[]) => void;
    onError?: (error: Error) => void;
}

export const useUpdateWorkflowNodeOutputsMutation = (mutationProps?: UpdateWorkflowNodeOutputsMutationProps) =>
    useMutation({
        mutationFn: (request: GetWorkflowNodeOutputsRequest) =>
            new WorkflowNodeOutputApi().getWorkflowNodeOutputs(request),
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
