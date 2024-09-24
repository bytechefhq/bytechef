import {ApiCollectionEndpoint, ApiCollectionEndpointApi} from '@/middleware/automation/api-platform';
import {useMutation} from '@tanstack/react-query';

interface CreateApiCollectionEndpointMutationProps {
    onError?: (error: Error, variables: ApiCollectionEndpoint) => void;
    onSuccess?: (result: ApiCollectionEndpoint, variables: ApiCollectionEndpoint) => void;
}

export const useCreateApiCollectionEndpointMutation = (mutationProps?: CreateApiCollectionEndpointMutationProps) =>
    useMutation<ApiCollectionEndpoint, Error, ApiCollectionEndpoint>({
        mutationFn: (apiCollectionEndpoint: ApiCollectionEndpoint) => {
            return new ApiCollectionEndpointApi().createApiCollectionEndpoint({
                apiCollectionEndpoint,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface DeleteApiEndpointMutationProps {
    onError?: (error: Error, variables: number) => void;
    onSuccess?: (result: void, variables: number) => void;
}

export const useDeleteApiCollectionEndpointMutation = (mutationProps?: DeleteApiEndpointMutationProps) =>
    useMutation<void, Error, number>({
        mutationFn: (id: number) => {
            return new ApiCollectionEndpointApi().deleteApiCollectionEndpoint({
                id,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface UpdateApiCollectionEndpointMutationProps {
    onError?: (error: Error, variables: ApiCollectionEndpoint) => void;
    onSuccess?: (result: ApiCollectionEndpoint, variables: ApiCollectionEndpoint) => void;
}

export const useUpdateApiCollectionEndpointMutation = (mutationProps?: UpdateApiCollectionEndpointMutationProps) =>
    useMutation<ApiCollectionEndpoint, Error, ApiCollectionEndpoint>({
        mutationFn: (apiCollectionEndpoint: ApiCollectionEndpoint) => {
            return new ApiCollectionEndpointApi().updateApiCollectionEndpoint({
                apiCollectionEndpoint,
                id: apiCollectionEndpoint.id!,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
