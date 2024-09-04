import {
    AdminApiKeyApi,
    AdminApiKeyModel,
    type CreateAdminApiKey200ResponseModel,
} from '@/shared/middleware/platform/user';
import {useMutation} from '@tanstack/react-query';

interface CreateAdminApiKeyMutationProps {
    onError?: (error: Error, variables: AdminApiKeyModel) => void;
    onSuccess?: (result: CreateAdminApiKey200ResponseModel, variables: AdminApiKeyModel) => void;
}

export const useCreateAdminApiKeyMutation = (mutationProps?: CreateAdminApiKeyMutationProps) =>
    useMutation<CreateAdminApiKey200ResponseModel, Error, AdminApiKeyModel>({
        mutationFn: (adminApiKeyModel: AdminApiKeyModel) => {
            return new AdminApiKeyApi().createAdminApiKey({
                adminApiKeyModel,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface DeleteAdminApiKeyMutationProps {
    onError?: (error: Error, variables: number) => void;
    onSuccess?: (result: void, variables: number) => void;
}

export const useDeleteAdminApiKeyMutation = (mutationProps?: DeleteAdminApiKeyMutationProps) =>
    useMutation<void, Error, number>({
        mutationFn: (id: number) => {
            return new AdminApiKeyApi().deleteAdminApiKey({
                id,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface UpdateAdminApiKeyMutationProps {
    onError?: (error: Error, variables: AdminApiKeyModel) => void;
    onSuccess?: (result: AdminApiKeyModel, variables: AdminApiKeyModel) => void;
}

export const useUpdateAdminApiKeyMutation = (mutationProps?: UpdateAdminApiKeyMutationProps) =>
    useMutation<AdminApiKeyModel, Error, AdminApiKeyModel>({
        mutationFn: (adminApiKeyModel: AdminApiKeyModel) => {
            return new AdminApiKeyApi().updateAdminApiKey({
                adminApiKeyModel,
                id: adminApiKeyModel.id!,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
