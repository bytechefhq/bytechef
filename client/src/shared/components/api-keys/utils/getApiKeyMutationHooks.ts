import {
    ApiKeysProviderStateI,
    CreateApiKeyMutationProps,
    SimpleMutationProps,
} from '@/shared/components/api-keys/providers/apiKeysProvider';
import {
    ApiKey,
    CreateApiKeyMutation,
    PlatformType,
    useCreateApiKeyMutation,
    useDeleteApiKeyMutation,
    useUpdateApiKeyMutation,
} from '@/shared/middleware/graphql';
import {QueryClient, UseMutationResult} from '@tanstack/react-query';

interface ApiKeyMutationHooksOptionsI {
    environmentId: number;
    queryClient: QueryClient;
    queryKey: string[];
    type?: PlatformType;
}

type ApiKeyMutationHooksType = Pick<
    ApiKeysProviderStateI,
    'useCreateApiKeyMutation' | 'useDeleteApiKeyMutation' | 'useUpdateApiKeyMutation'
>;

export const getApiKeyMutationHooks = ({
    environmentId,
    queryClient,
    queryKey,
    type,
}: ApiKeyMutationHooksOptionsI): ApiKeyMutationHooksType => ({
    useCreateApiKeyMutation: (props?: CreateApiKeyMutationProps) => {
        const createApiKeyMutation = useCreateApiKeyMutation({
            onSuccess: (data: CreateApiKeyMutation) => {
                void queryClient.invalidateQueries({queryKey});

                props?.onSuccess?.(data.createApiKey);
            },
        }) as UseMutationResult<unknown, Error>;

        return {
            mutate: (apiKey: ApiKey) => {
                createApiKeyMutation.mutate({
                    ...apiKey,
                    environmentId,
                    ...(type && {type}),
                });
            },
        } as UseMutationResult<unknown, Error>;
    },

    useDeleteApiKeyMutation: (props?: SimpleMutationProps) => {
        const deleteApiKeyMutation = useDeleteApiKeyMutation({
            onSuccess: (data) => {
                void queryClient.invalidateQueries({queryKey});

                props?.onSuccess?.(data.deleteApiKey);
            },
        });

        return {
            isPending: deleteApiKeyMutation.isPending,
            mutate: ({apiKeyId}: {apiKeyId: string}) => {
                deleteApiKeyMutation.mutate({id: apiKeyId});
            },
        } as UseMutationResult<unknown, Error>;
    },

    useUpdateApiKeyMutation: (props?: SimpleMutationProps) =>
        useUpdateApiKeyMutation({
            onSuccess: (data) => {
                void queryClient.invalidateQueries({queryKey});

                props?.onSuccess?.(data.updateApiKey);
            },
        }) as unknown as UseMutationResult<unknown, Error>,
});
