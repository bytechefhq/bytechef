import {ApiKey, ApiKeyApi, type CreateApiKey200Response} from '@/shared/middleware/platform/api-key';
import {useMutation} from '@tanstack/react-query';

interface CreateApiKeyMutationProps {
    onError?: (error: Error, variables: ApiKey) => void;
    onSuccess?: (result: CreateApiKey200Response, variables: ApiKey) => void;
}

export const useCreateApiKeyMutation = (mutationProps?: CreateApiKeyMutationProps) =>
    useMutation<CreateApiKey200Response, Error, ApiKey>({
        mutationFn: (apiKey: ApiKey) => {
            return new ApiKeyApi().createApiKey({
                apiKey,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface DeleteApiKeyMutationProps {
    onError?: (error: Error, variables: number) => void;
    onSuccess?: (result: void, variables: number) => void;
}

export const useDeleteApiKeyMutation = (mutationProps?: DeleteApiKeyMutationProps) =>
    useMutation<void, Error, number>({
        mutationFn: (id: number) => {
            return new ApiKeyApi().deleteApiKey({
                id,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface UpdateApiKeyMutationProps {
    onError?: (error: Error, variables: ApiKey) => void;
    onSuccess?: (result: void, variables: ApiKey) => void;
}

export const useUpdateApiKeyMutation = (mutationProps?: UpdateApiKeyMutationProps) =>
    useMutation<void, Error, ApiKey>({
        mutationFn: (apiKey: ApiKey) => {
            return new ApiKeyApi().updateApiKey({
                apiKey,
                id: apiKey.id!,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
