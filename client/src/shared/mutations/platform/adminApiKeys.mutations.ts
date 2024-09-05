import {AdminApiKey, AdminApiKeyApi, type CreateAdminApiKey200Response} from '@/shared/middleware/platform/user';
import {useMutation} from '@tanstack/react-query';

interface CreateAdminApiKeyMutationProps {
    onError?: (error: Error, variables: AdminApiKey) => void;
    onSuccess?: (result: CreateAdminApiKey200Response, variables: AdminApiKey) => void;
}

export const useCreateAdminApiKeyMutation = (mutationProps?: CreateAdminApiKeyMutationProps) =>
    useMutation<CreateAdminApiKey200Response, Error, AdminApiKey>({
        mutationFn: (adminApiKey: AdminApiKey) => {
            return new AdminApiKeyApi().createAdminApiKey({
                adminApiKey,
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
    onError?: (error: Error, variables: AdminApiKey) => void;
    onSuccess?: (result: AdminApiKey, variables: AdminApiKey) => void;
}

export const useUpdateAdminApiKeyMutation = (mutationProps?: UpdateAdminApiKeyMutationProps) =>
    useMutation<AdminApiKey, Error, AdminApiKey>({
        mutationFn: (adminApiKey: AdminApiKey) => {
            return new AdminApiKeyApi().updateAdminApiKey({
                adminApiKey,
                id: adminApiKey.id!,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
