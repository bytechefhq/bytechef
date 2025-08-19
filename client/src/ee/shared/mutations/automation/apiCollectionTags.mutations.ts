import {ApiCollectionTagApi, UpdateApiCollectionTagsRequest} from '@/ee/shared/middleware/automation/api-platform';
import {useMutation} from '@tanstack/react-query';

interface UpdateApiCollectionTagsMutationProps {
    onSuccess?: (result: void, variables: UpdateApiCollectionTagsRequest) => void;
    onError?: (error: Error, variables: UpdateApiCollectionTagsRequest) => void;
}

export const useUpdateApiCollectionTagsMutation = (mutationProps?: UpdateApiCollectionTagsMutationProps) =>
    useMutation<void, Error, UpdateApiCollectionTagsRequest>({
        mutationFn: (request: UpdateApiCollectionTagsRequest) => {
            return new ApiCollectionTagApi().updateApiCollectionTags(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
