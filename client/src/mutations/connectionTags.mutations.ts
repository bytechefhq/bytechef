import {
    ConnectionTagApi,
    UpdateConnectionTagsRequest,
} from '@/middleware/helios/connection';
import {useMutation} from '@tanstack/react-query';

type UpdateConnectionTagsMutationProps = {
    onSuccess?: (result: void, variables: UpdateConnectionTagsRequest) => void;
    onError?: (error: Error, variables: UpdateConnectionTagsRequest) => void;
};

export const useUpdateConnectionTagsMutation = (
    mutationProps?: UpdateConnectionTagsMutationProps
) =>
    useMutation<void, Error, UpdateConnectionTagsRequest>({
        mutationFn: (request: UpdateConnectionTagsRequest) => {
            return new ConnectionTagApi().updateConnectionTags(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
