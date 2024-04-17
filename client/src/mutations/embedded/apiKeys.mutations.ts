import {ApiKeyApi, ApiKeyModel, type CreateApiKey200ResponseModel} from '@/middleware/embedded/user';
import {useMutation} from '@tanstack/react-query';

interface CreateApiKeyMutationProps {
    onError?: (error: Error, variables: ApiKeyModel) => void;
    onSuccess?: (result: CreateApiKey200ResponseModel, variables: ApiKeyModel) => void;
}

export const useCreateApiKeyMutation = (mutationProps?: CreateApiKeyMutationProps) =>
    useMutation<CreateApiKey200ResponseModel, Error, ApiKeyModel>({
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
