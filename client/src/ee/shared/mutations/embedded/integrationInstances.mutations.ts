import {
    DeleteIntegrationInstanceRequest,
    EnableIntegrationInstanceRequest,
    IntegrationInstanceApi,
    ProvisionIntegrationInstanceOperationRequest,
    UpdateIntegrationInstanceCredentialsRequest,
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

interface UpdateIntegrationInstanceCredentialsMutationProps {
    onError?: (error: Error, variables: UpdateIntegrationInstanceCredentialsRequest) => void;
    onSuccess?: (result: void, variables: UpdateIntegrationInstanceCredentialsRequest) => void;
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

export const useUpdateIntegrationInstanceCredentialsMutation = (
    mutationProps?: UpdateIntegrationInstanceCredentialsMutationProps
) =>
    useMutation<void, Error, UpdateIntegrationInstanceCredentialsRequest>({
        mutationFn: (updateIntegrationInstanceCredentialsRequest: UpdateIntegrationInstanceCredentialsRequest) => {
            return new IntegrationInstanceApi().updateIntegrationInstanceCredentials(
                updateIntegrationInstanceCredentialsRequest
            );
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
