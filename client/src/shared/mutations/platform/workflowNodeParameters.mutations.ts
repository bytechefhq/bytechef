import {
    DeleteClusterElementParameterRequest,
    DeleteWorkflowNodeParameter200Response,
    DeleteWorkflowNodeParameterOperationRequest,
    UpdateClusterElementParameterRequest,
    UpdateWorkflowNodeParameter200Response,
    UpdateWorkflowNodeParameterOperationRequest,
    WorkflowNodeParameterApi,
} from '@/shared/middleware/platform/configuration';
import {useMutation} from '@tanstack/react-query';

interface DeleteWorkflowNodeParameterProps {
    onSuccess?: (
        result: DeleteWorkflowNodeParameter200Response,
        variables: DeleteWorkflowNodeParameterOperationRequest
    ) => void;
    onError?: (error: Error, variables: DeleteWorkflowNodeParameterOperationRequest) => void;
}

export const useDeleteWorkflowNodeParameterMutation = (mutationProps?: DeleteWorkflowNodeParameterProps) =>
    useMutation({
        mutationFn: (request: DeleteWorkflowNodeParameterOperationRequest) => {
            return new WorkflowNodeParameterApi().deleteWorkflowNodeParameter(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface DeleteClusterElementParameterProps {
    onSuccess?: (
        result: DeleteWorkflowNodeParameter200Response,
        variables: DeleteClusterElementParameterRequest
    ) => void;
    onError?: (error: Error, variables: DeleteClusterElementParameterRequest) => void;
}

export const useDeleteClusterElementParameterMutation = (mutationProps?: DeleteClusterElementParameterProps) =>
    useMutation({
        mutationFn: (request: DeleteClusterElementParameterRequest) => {
            return new WorkflowNodeParameterApi().deleteClusterElementParameter(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface UpdateWorkflowNodeParameterProps {
    onSuccess?: (
        result: UpdateWorkflowNodeParameter200Response,
        variables: UpdateWorkflowNodeParameterOperationRequest
    ) => void;
    onError?: (error: Error, variables: UpdateWorkflowNodeParameterOperationRequest) => void;
}

export const useUpdateWorkflowNodeParameterMutation = (mutationProps?: UpdateWorkflowNodeParameterProps) =>
    useMutation({
        mutationFn: (request: UpdateWorkflowNodeParameterOperationRequest) => {
            return new WorkflowNodeParameterApi().updateWorkflowNodeParameter(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface UpdateClusterElementParameterProps {
    onSuccess?: (
        result: UpdateWorkflowNodeParameter200Response,
        variables: UpdateClusterElementParameterRequest
    ) => void;
    onError?: (error: Error, variables: UpdateClusterElementParameterRequest) => void;
}

export const useUpdateClusterElementParameterMutation = (mutationProps?: UpdateClusterElementParameterProps) =>
    useMutation({
        mutationFn: (request: UpdateClusterElementParameterRequest) => {
            return new WorkflowNodeParameterApi().updateClusterElementParameter(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
