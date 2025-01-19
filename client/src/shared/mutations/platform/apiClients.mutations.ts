import {ApiClient, ApiClientApi, CreateApiClient200Response} from '@/ee/shared/middleware/automation/api-platform';
import {useMutation} from '@tanstack/react-query';

interface CreateApiClientMutationProps {
    onError?: (error: Error, variables: ApiClient) => void;
    onSuccess?: (result: CreateApiClient200Response, variables: ApiClient) => void;
}

export const useCreateApiClientMutation = (mutationProps?: CreateApiClientMutationProps) =>
    useMutation<CreateApiClient200Response, Error, ApiClient>({
        mutationFn: (apiClient: ApiClient) => {
            return new ApiClientApi().createApiClient({
                apiClient,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface DeleteApiClientMutationProps {
    onError?: (error: Error, variables: number) => void;
    onSuccess?: (result: void, variables: number) => void;
}

export const useDeleteApiClientMutation = (mutationProps?: DeleteApiClientMutationProps) =>
    useMutation<void, Error, number>({
        mutationFn: (id: number) => {
            return new ApiClientApi().deleteApiClient({
                id,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface UpdateApiClientMutationProps {
    onError?: (error: Error, variables: ApiClient) => void;
    onSuccess?: (result: void, variables: ApiClient) => void;
}

export const useUpdateApiClientMutation = (mutationProps?: UpdateApiClientMutationProps) =>
    useMutation<void, Error, ApiClient>({
        mutationFn: (apiClient: ApiClient) => {
            return new ApiClientApi().updateApiClient({
                apiClient,
                id: apiClient.id!,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
