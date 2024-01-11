import {
    IntegrationInstanceTagApi,
    UpdateIntegrationInstanceTagsRequest,
    UpdateIntegrationTagsRequest,
} from '@/middleware/embedded/configuration';
import {useMutation} from '@tanstack/react-query';

interface UpdateIntegrationInstanceTagsMutationProps {
    onSuccess?: (result: void, variables: UpdateIntegrationInstanceTagsRequest) => void;
    onError?: (error: Error, variables: UpdateIntegrationInstanceTagsRequest) => void;
}

export const useUpdateIntegrationInstanceTagsMutation = (mutationProps?: UpdateIntegrationInstanceTagsMutationProps) =>
    useMutation({
        mutationFn: (request: UpdateIntegrationTagsRequest) => {
            return new IntegrationInstanceTagApi().updateIntegrationInstanceTags(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
