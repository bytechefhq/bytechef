import {ApiKeyApi, ApiKeyModel} from '@/middleware/platform/user';
import {useMutation} from '@tanstack/react-query';

interface CreateApiKeyMutationProps {
    onError?: (error: Error, variables: ApiKeyModel) => void;
    onSuccess?: (result: ApiKeyModel, variables: ApiKeyModel) => void;
}

export const useCreateApiKeyMutation = (mutationProps?: CreateApiKeyMutationProps) =>
    useMutation<ApiKeyModel, Error, ApiKeyModel>({
        mutationFn: (apiKeyModel: ApiKeyModel) => {
            return new ApiKeyApi().createApiKey({
                apiKeyModel,
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
    onError?: (error: Error, variables: ApiKeyModel) => void;
    onSuccess?: (result: ApiKeyModel, variables: ApiKeyModel) => void;
}

export const useUpdateApiKeyMutation = (mutationProps?: UpdateApiKeyMutationProps) =>
    useMutation<ApiKeyModel, Error, ApiKeyModel>({
        mutationFn: (apiKeyModel: ApiKeyModel) => {
            return new ApiKeyApi().updateApiKey({
                apiKeyModel,
                id: apiKeyModel.id!,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
