import {
    DeleteClusterElementParameter200Response,
    DeleteClusterElementParameterOperationRequest,
    DeleteWorkflowNodeParameterRequest,
    UpdateClusterElementParameterOperationRequest,
    UpdateWorkflowNodeParameterOperationRequest,
    WorkflowNodeParameterApi,
} from '@/shared/middleware/platform/configuration';
import {
    ClusterElementDynamicPropertyKeys,
    WorkflowNodeDynamicPropertyKeys,
} from '@/shared/queries/platform/workflowNodeDynamicProperties.queries';
import {WorkflowNodeOptionKeys} from '@/shared/queries/platform/workflowNodeOptions.queries';
import {QueryClient, useMutation, useQueryClient} from '@tanstack/react-query';

const clusterElementOptionsQueryKey = ['clusterElementOptions'] as const;

// Dependent queries read their inputs from the persisted workflow on the backend, so any
// parameter write can shift their results. Invalidate here to force a refetch instead of
// serving cached (possibly empty) responses keyed by a since-stale dependency snapshot.
function invalidateDependentQueries(queryClient: QueryClient) {
    queryClient.invalidateQueries({queryKey: WorkflowNodeDynamicPropertyKeys.workflowNodeDynamicProperties});
    queryClient.invalidateQueries({queryKey: ClusterElementDynamicPropertyKeys.clusterElementDynamicProperties});
    queryClient.invalidateQueries({queryKey: WorkflowNodeOptionKeys.workflowNodeOptions});
    queryClient.invalidateQueries({queryKey: WorkflowNodeOptionKeys.clusterElementNodeOptions});
    queryClient.invalidateQueries({queryKey: clusterElementOptionsQueryKey});
}

interface DeleteWorkflowNodeParameterProps {
    onSuccess?: (
        result: DeleteClusterElementParameter200Response,
        variables: DeleteWorkflowNodeParameterRequest
    ) => void;
    onError?: (error: Error, variables: DeleteWorkflowNodeParameterRequest) => void;
}

export const useDeleteWorkflowNodeParameterMutation = (mutationProps?: DeleteWorkflowNodeParameterProps) => {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (request: DeleteWorkflowNodeParameterRequest) => {
            return new WorkflowNodeParameterApi().deleteWorkflowNodeParameter(request);
        },
        onError: mutationProps?.onError,
        onSuccess: (result, variables) => {
            invalidateDependentQueries(queryClient);

            mutationProps?.onSuccess?.(result, variables);
        },
    });
};

interface DeleteClusterElementParameterProps {
    onSuccess?: (
        result: DeleteClusterElementParameter200Response,
        variables: DeleteClusterElementParameterOperationRequest
    ) => void;
    onError?: (error: Error, variables: DeleteClusterElementParameterOperationRequest) => void;
}

export const useDeleteClusterElementParameterMutation = (mutationProps?: DeleteClusterElementParameterProps) => {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (request: DeleteClusterElementParameterOperationRequest) => {
            return new WorkflowNodeParameterApi().deleteClusterElementParameter(request);
        },
        onError: mutationProps?.onError,
        onSuccess: (result, variables) => {
            invalidateDependentQueries(queryClient);

            mutationProps?.onSuccess?.(result, variables);
        },
    });
};

interface UpdateWorkflowNodeParameterProps {
    onSuccess?: (
        result: DeleteClusterElementParameter200Response,
        variables: UpdateWorkflowNodeParameterOperationRequest
    ) => void;
    onError?: (error: Error, variables: UpdateWorkflowNodeParameterOperationRequest) => void;
}

export const useUpdateWorkflowNodeParameterMutation = (mutationProps?: UpdateWorkflowNodeParameterProps) => {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (request: UpdateWorkflowNodeParameterOperationRequest) => {
            return new WorkflowNodeParameterApi().updateWorkflowNodeParameter(request);
        },
        onError: mutationProps?.onError,
        onSuccess: (result, variables) => {
            invalidateDependentQueries(queryClient);

            mutationProps?.onSuccess?.(result, variables);
        },
    });
};

interface UpdateClusterElementParameterProps {
    onSuccess?: (
        result: DeleteClusterElementParameter200Response,
        variables: UpdateClusterElementParameterOperationRequest
    ) => void;
    onError?: (error: Error, variables: UpdateClusterElementParameterOperationRequest) => void;
}

export const useUpdateClusterElementParameterMutation = (mutationProps?: UpdateClusterElementParameterProps) => {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (request: UpdateClusterElementParameterOperationRequest) => {
            return new WorkflowNodeParameterApi().updateClusterElementParameter(request);
        },
        onError: mutationProps?.onError,
        onSuccess: (result, variables) => {
            invalidateDependentQueries(queryClient);

            mutationProps?.onSuccess?.(result, variables);
        },
    });
};
