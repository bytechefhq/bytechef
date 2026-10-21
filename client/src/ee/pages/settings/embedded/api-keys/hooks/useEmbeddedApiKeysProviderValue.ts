import {
    ApiKeysProviderStateI,
    CreateApiKeyMutationProps,
    SimpleMutationProps,
} from '@/shared/components/api-keys/providers/apiKeysProvider';
import {
    ApiKey,
    useCreateEmbeddedApiKeyMutation,
    useDeleteEmbeddedApiKeyMutation,
    useEmbeddedApiKeysQuery,
    useUpdateEmbeddedApiKeyMutation,
} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {UseMutationResult, UseQueryResult, useQueryClient} from '@tanstack/react-query';
import {toast} from 'sonner';

const EMBEDDED_API_KEYS_QUERY_KEY = ['EmbeddedApiKeys'];

const useEmbeddedApiKeysProviderValue = (): ApiKeysProviderStateI => {
    const environmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const queryClient = useQueryClient();

    return {
        useApiKeysQuery: () => {
            const {
                data: {embeddedApiKeys} = {},
                error,
                isLoading,
            } = useEmbeddedApiKeysQuery({
                environmentId: `${environmentId}`,
            });

            return {
                data: embeddedApiKeys?.filter((apiKey) => apiKey !== null),
                error,
                isLoading,
            } as UseQueryResult<ApiKey[], Error>;
        },
        useCreateApiKeyMutation: (props?: CreateApiKeyMutationProps) => {
            const createEmbeddedApiKeyMutation = useCreateEmbeddedApiKeyMutation({
                onError: (error) => {
                    props?.onError?.(error as unknown as Error);
                },
                onSuccess: (data) => {
                    void queryClient.invalidateQueries({queryKey: EMBEDDED_API_KEYS_QUERY_KEY});

                    if (!data.createEmbeddedApiKey) {
                        toast.error('The API key was not created. Please try again.');

                        props?.onError?.(new Error('The API key was not created.'));

                        return;
                    }

                    props?.onSuccess?.(data.createEmbeddedApiKey);
                },
            });

            return {
                mutate: (apiKey: ApiKey) => {
                    createEmbeddedApiKeyMutation.mutate({
                        input: {
                            environmentId: `${environmentId}`,
                            name: apiKey.name!,
                        },
                    });
                },
            } as UseMutationResult<unknown, Error>;
        },
        useDeleteApiKeyMutation: (props?: SimpleMutationProps) => {
            const deleteEmbeddedApiKeyMutation = useDeleteEmbeddedApiKeyMutation({
                onError: (error) => {
                    props?.onError?.(error as unknown as Error);
                },
                onSuccess: (data) => {
                    void queryClient.invalidateQueries({queryKey: EMBEDDED_API_KEYS_QUERY_KEY});

                    props?.onSuccess?.(data.deleteEmbeddedApiKey);
                },
            });

            return {
                isPending: deleteEmbeddedApiKeyMutation.isPending,
                mutate: ({apiKeyId}: {apiKeyId: string}) => {
                    deleteEmbeddedApiKeyMutation.mutate({id: apiKeyId});
                },
            } as UseMutationResult<unknown, Error>;
        },
        useUpdateApiKeyMutation: (props?: SimpleMutationProps) => {
            const updateEmbeddedApiKeyMutation = useUpdateEmbeddedApiKeyMutation({
                onError: (error) => {
                    props?.onError?.(error as unknown as Error);
                },
                onSuccess: (data) => {
                    void queryClient.invalidateQueries({queryKey: EMBEDDED_API_KEYS_QUERY_KEY});

                    props?.onSuccess?.(data.updateEmbeddedApiKey);
                },
            });

            return {
                mutate: (apiKey: ApiKey) => {
                    updateEmbeddedApiKeyMutation.mutate({
                        input: {
                            id: apiKey.id!,
                            name: apiKey.name!,
                        },
                    });
                },
            } as UseMutationResult<unknown, Error>;
        },
    };
};

export default useEmbeddedApiKeysProviderValue;
