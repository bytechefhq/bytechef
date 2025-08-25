import {
    DeleteClusterElementParameter200Response,
    DeleteWorkflowNodeParameterRequest,
    UpdateClusterElementParameter200Response,
    UpdateWorkflowNodeParameterRequest,
    WorkflowNodeParameterApi,
} from '@/shared/middleware/platform/configuration';
import {useMutation} from '@tanstack/react-query';

interface DeleteWorkflowNodeParameterProps {
    onSuccess?: (
        result: DeleteClusterElementParameter200Response,
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
        result: UpdateClusterElementParameter200Response,
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
