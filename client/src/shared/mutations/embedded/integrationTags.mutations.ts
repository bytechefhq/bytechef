import {IntegrationTagApi, UpdateIntegrationTagsRequest} from '@/shared/middleware/embedded/configuration';
import {useMutation} from '@tanstack/react-query';

interface UpdateIntegrationTagsMutationProps {
    onSuccess?: (result: void, variables: UpdateIntegrationTagsRequest) => void;
    onError?: (error: Error, variables: UpdateIntegrationTagsRequest) => void;
}

export const useUpdateIntegrationTagsMutation = (mutationProps?: UpdateIntegrationTagsMutationProps) =>
    useMutation({
        mutationFn: (request: UpdateIntegrationTagsRequest) => {
            return new IntegrationTagApi().updateIntegrationTags(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
