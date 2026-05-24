import {
    DeleteIntegrationInstanceRequest,
    EnableIntegrationInstanceRequest,
    IntegrationInstanceApi,
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
