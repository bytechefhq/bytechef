import {
    DeleteClusterElementParameter200Response,
    DeleteClusterElementParameterOperationRequest,
    DeleteWorkflowNodeParameterOperationRequest,
    UpdateClusterElementParameterOperationRequest,
    UpdateWorkflowNodeParameterOperationRequest,
    WorkflowNodeParameterApi,
} from '@/shared/middleware/platform/configuration';
import {useMutation} from '@tanstack/react-query';

interface DeleteWorkflowNodeParameterProps {
    onSuccess?: (
        result: DeleteClusterElementParameter200Response,
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
        result: DeleteClusterElementParameter200Response,
        variables: DeleteClusterElementParameterOperationRequest
    ) => void;
    onError?: (error: Error, variables: DeleteClusterElementParameterOperationRequest) => void;
}

export const useDeleteClusterElementParameterMutation = (mutationProps?: DeleteClusterElementParameterProps) =>
    useMutation({
        mutationFn: (request: DeleteClusterElementParameterOperationRequest) => {
            return new WorkflowNodeParameterApi().deleteClusterElementParameter(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface UpdateWorkflowNodeParameterProps {
    onSuccess?: (
        result: DeleteClusterElementParameter200Response,
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
        result: DeleteClusterElementParameter200Response,
        variables: UpdateClusterElementParameterOperationRequest
    ) => void;
    onError?: (error: Error, variables: UpdateClusterElementParameterOperationRequest) => void;
}

export const useUpdateClusterElementParameterMutation = (mutationProps?: UpdateClusterElementParameterProps) =>
    useMutation({
        mutationFn: (request: UpdateClusterElementParameterOperationRequest) => {
            return new WorkflowNodeParameterApi().updateClusterElementParameter(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
