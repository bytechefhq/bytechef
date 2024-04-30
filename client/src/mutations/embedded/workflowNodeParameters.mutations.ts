import {
    DeleteWorkflowNodeParameter200ResponseModel,
    DeleteWorkflowNodeParameterRequest,
    UpdateWorkflowNodeParameter200ResponseModel,
    UpdateWorkflowNodeParameterRequest,
    WorkflowNodeParameterApi,
} from '@/middleware/embedded/configuration';
import {useMutation} from '@tanstack/react-query';

interface DeleteWorkflowNodeParameterProps {
    onSuccess?: (
        result: DeleteWorkflowNodeParameter200ResponseModel,
        variables: DeleteWorkflowNodeParameterRequest
    ) => void;
    onError?: (error: Error, variables: DeleteWorkflowNodeParameterRequest) => void;
}

export const useDeleteWorkflowNodeParameterMutation = (mutationProps?: DeleteWorkflowNodeParameterProps) =>
    useMutation({
        mutationFn: (request: DeleteWorkflowNodeParameterRequest) => {
            return new WorkflowNodeParameterApi().deleteWorkflowNodeParameter(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface UpdateWorkflowNodeParameterProps {
    onSuccess?: (
        result: UpdateWorkflowNodeParameter200ResponseModel,
        variables: UpdateWorkflowNodeParameterRequest
    ) => void;
    onError?: (error: Error, variables: UpdateWorkflowNodeParameterRequest) => void;
}

export const useUpdateWorkflowNodeParameterMutation = (mutationProps?: UpdateWorkflowNodeParameterProps) =>
    useMutation({
        mutationFn: (request: UpdateWorkflowNodeParameterRequest) => {
            return new WorkflowNodeParameterApi().updateWorkflowNodeParameter(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
