import {
    DeleteIntegrationInstanceRequest,
    EnableIntegrationInstanceRequest,
    IntegrationInstanceApi,
    ProvisionIntegrationInstanceOperationRequest,
} from '@/ee/shared/middleware/embedded/configuration';
import {useMutation} from '@tanstack/react-query';

interface DeleteIntegrationInstanceMutationProps {
    onError?: (error: Error, variables: DeleteIntegrationInstanceRequest) => void;
    onSuccess?: (result: void, variables: DeleteIntegrationInstanceRequest) => void;
}

interface EnableIntegrationInstanceMutationProps {
    onError?: (error: Error, variables: EnableIntegrationInstanceRequest) => void;
    onSuccess?: (result: void, variables: EnableIntegrationInstanceRequest) => void;
}

interface ProvisionIntegrationInstanceMutationProps {
    onError?: (error: Error, variables: ProvisionIntegrationInstanceOperationRequest) => void;
    onSuccess?: (result: number, variables: ProvisionIntegrationInstanceOperationRequest) => void;
}

export const useDeleteIntegrationInstanceMutation = (mutationProps?: DeleteIntegrationInstanceMutationProps) =>
    useMutation<void, Error, DeleteIntegrationInstanceRequest>({
        mutationFn: (deleteIntegrationInstanceRequest: DeleteIntegrationInstanceRequest) => {
            return new IntegrationInstanceApi().deleteIntegrationInstance(deleteIntegrationInstanceRequest);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

export const useEnableIntegrationInstanceMutation = (mutationProps?: EnableIntegrationInstanceMutationProps) =>
    useMutation<void, Error, EnableIntegrationInstanceRequest>({
        mutationFn: (enableIntegrationInstanceRequest: EnableIntegrationInstanceRequest) => {
            return new IntegrationInstanceApi().enableIntegrationInstance(enableIntegrationInstanceRequest);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

export const useProvisionIntegrationInstanceMutation = (mutationProps?: ProvisionIntegrationInstanceMutationProps) =>
    useMutation<number, Error, ProvisionIntegrationInstanceOperationRequest>({
        mutationFn: (provisionIntegrationInstanceRequest: ProvisionIntegrationInstanceOperationRequest) => {
            return new IntegrationInstanceApi().provisionIntegrationInstance(provisionIntegrationInstanceRequest);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
