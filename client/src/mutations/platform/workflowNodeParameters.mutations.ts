import {
    type UpdateWorkflowNodeParameter200ResponseModel,
    UpdateWorkflowNodeParameterRequest,
    WorkflowNodeParameterApi,
} from '@/middleware/platform/configuration';
import {useMutation} from '@tanstack/react-query';

interface SaveWorkflowNodeTestOutputProps {
    onSuccess?: (
        result: UpdateWorkflowNodeParameter200ResponseModel,
        variables: UpdateWorkflowNodeParameterRequest
    ) => void;
    onError?: (error: Error, variables: UpdateWorkflowNodeParameterRequest) => void;
}

export const useUpdateWorkflowNodeParameterMutation = (mutationProps?: SaveWorkflowNodeTestOutputProps) =>
    useMutation({
        mutationFn: (request: UpdateWorkflowNodeParameterRequest) => {
            return new WorkflowNodeParameterApi().updateWorkflowNodeParameter(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
