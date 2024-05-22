import {
    EnableIntegrationInstanceWorkflowRequest,
    IntegrationInstanceApi,
} from '@/shared/middleware/embedded/configuration';
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
