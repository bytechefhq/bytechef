import {
    IntegrationInstanceConfigurationTagApi,
    UpdateIntegrationInstanceConfigurationTagsRequest,
    UpdateIntegrationTagsRequest,
} from '@/shared/middleware/embedded/configuration';
import {useMutation} from '@tanstack/react-query';

interface UpdateIntegrationInstanceTagsMutationProps {
    onSuccess?: (result: void, variables: UpdateIntegrationInstanceConfigurationTagsRequest) => void;
    onError?: (error: Error, variables: UpdateIntegrationInstanceConfigurationTagsRequest) => void;
}

export const useUpdateIntegrationInstanceTagsMutation = (mutationProps?: UpdateIntegrationInstanceTagsMutationProps) =>
    useMutation({
        mutationFn: (request: UpdateIntegrationTagsRequest) => {
            return new IntegrationInstanceConfigurationTagApi().updateIntegrationInstanceConfigurationTags(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
