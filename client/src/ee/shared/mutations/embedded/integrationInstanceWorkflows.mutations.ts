import {
    EnableIntegrationInstanceWorkflowRequest,
    IntegrationInstanceApi,
    UpdateIntegrationInstanceWorkflowOperationRequest,
} from '@/ee/shared/middleware/embedded/configuration';
import {useMutation} from '@tanstack/react-query';

interface EnableIntegrationInstanceWorkflowMutationProps {
    onSuccess?: (result: void, variables: EnableIntegrationInstanceWorkflowRequest) => void;
    onError?: (error: Error, variables: EnableIntegrationInstanceWorkflowRequest) => void;
}

export const useEnableIntegrationInstanceWorkflowMutation = (
    mutationProps: EnableIntegrationInstanceWorkflowMutationProps
) =>
    useMutation({
        mutationFn: (request: EnableIntegrationInstanceWorkflowRequest) => {
            return new IntegrationInstanceApi().enableIntegrationInstanceWorkflow(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface UpdateIntegrationInstanceWorkflowMutationProps {
    onSuccess?: (result: void, variables: UpdateIntegrationInstanceWorkflowOperationRequest) => void;
    onError?: (error: Error, variables: UpdateIntegrationInstanceWorkflowOperationRequest) => void;
}

export const useUpdateIntegrationInstanceWorkflowMutation = (
    mutationProps: UpdateIntegrationInstanceWorkflowMutationProps
) =>
    useMutation({
        mutationFn: (request: UpdateIntegrationInstanceWorkflowOperationRequest) => {
            return new IntegrationInstanceApi().updateIntegrationInstanceWorkflow(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
