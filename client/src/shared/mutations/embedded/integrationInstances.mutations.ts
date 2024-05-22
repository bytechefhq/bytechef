import {EnableIntegrationInstanceRequest, IntegrationInstanceApi} from '@/shared/middleware/embedded/configuration';
import {useMutation} from '@tanstack/react-query';

interface EnableIntegrationInstanceMutationProps {
    onError?: (error: Error, variables: EnableIntegrationInstanceRequest) => void;
    onSuccess?: (result: void, variables: EnableIntegrationInstanceRequest) => void;
}

export const useEnableIntegrationInstanceMutation = (mutationProps?: EnableIntegrationInstanceMutationProps) =>
    useMutation<void, Error, EnableIntegrationInstanceRequest>({
        mutationFn: (enableIntegrationInstanceRequest: EnableIntegrationInstanceRequest) => {
            return new IntegrationInstanceApi().enableIntegrationInstance(enableIntegrationInstanceRequest);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
