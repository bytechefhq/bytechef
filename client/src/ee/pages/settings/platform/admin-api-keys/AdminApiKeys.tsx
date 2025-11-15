import ApiKeysContent from '@/ee/shared/components/api-keys/ApiKeysContent';
import {
    ApiKeysProvider,
    CreateApiKeyMutationProps,
    SimpleMutationProps,
} from '@/ee/shared/components/api-keys/providers/apiKeysProvider';
import {
    ApiKey,
    CreateApiKeyMutation,
    useAdminApiKeysQuery,
    useCreateApiKeyMutation,
    useDeleteApiKeyMutation,
    useUpdateApiKeyMutation,
} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {UseMutationResult, UseQueryResult, useQueryClient} from '@tanstack/react-query';

const AdminApiKeys = () => {
    const environmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const queryClient = useQueryClient();

    return (
        <ApiKeysProvider
            value={{
                useApiKeysQuery: () => {
                    const {
                        data: {adminApiKeys} = {},
                        error,
                        isLoading,
                    } = useAdminApiKeysQuery({
                        environmentId: `${environmentId}`,
                    });

                    return {
                        data: adminApiKeys,
                        error,
                        isLoading,
                    } as UseQueryResult<ApiKey[], Error>;
                },
                useCreateApiKeyMutation: (props?: CreateApiKeyMutationProps) => {
                    const createApiKeyMutation = useCreateApiKeyMutation({
                        onSuccess: (data: CreateApiKeyMutation) => {
                            queryClient.invalidateQueries({
                                queryKey: ['adminApiKeys'],
                            });

                            props?.onSuccess?.(data.createApiKey);
                        },
                    }) as UseMutationResult<unknown, Error>;

                    return {
                        mutate: (apiKey: ApiKey) => {
                            createApiKeyMutation.mutate({
                                ...apiKey,
                                environmentId: environmentId,
                            });
                        },
                    } as UseMutationResult<unknown, Error>;
                },

                useDeleteApiKeyMutation: (props?: SimpleMutationProps) => {
                    const deleteApiKeyMutation = useDeleteApiKeyMutation({
                        onSuccess: (data) => {
                            queryClient.invalidateQueries({
                                queryKey: ['adminApiKeys'],
                            });

                            props?.onSuccess?.(data.deleteApiKey);
                        },
                    });

                    return {
                        mutate: ({apiKeyId}: {apiKeyId: string}) => {
                            deleteApiKeyMutation.mutate({id: apiKeyId});
                        },
                    } as UseMutationResult<unknown, Error>;
                },

                useUpdateApiKeyMutation: (props?: SimpleMutationProps) =>
                    useUpdateApiKeyMutation({
                        onSuccess: (data) => {
                            queryClient.invalidateQueries({
                                queryKey: ['adminApiKeys'],
                            });

                            props?.onSuccess?.(data.updateApiKey);
                        },
                    }) as unknown as UseMutationResult<unknown, Error>,
            }}
        >
            <ApiKeysContent
                description="Use Admin keys for programmatic administration of your account. Do not share your API key with others or expose it in the browser or other client-side code."
                title="Admin API Keys"
            />
        </ApiKeysProvider>
    );
};

export default AdminApiKeys;
