import ApiKeysContent from '@/ee/shared/components/api-keys/ApiKeysContent';
import {
    ApiKeysProvider,
    CreateApiKeyMutationProps,
    SimpleMutationProps,
} from '@/ee/shared/components/api-keys/providers/apiKeysProvider';
import {
    ApiKey,
    CreateApiKeyMutation,
    ModeType,
    useApiKeysQuery,
    useCreateApiKeyMutation,
    useDeleteApiKeyMutation,
    useUpdateApiKeyMutation,
} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {UseMutationResult, UseQueryResult, useQueryClient} from '@tanstack/react-query';

const ApiKeys = () => {
    const environmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const queryClient = useQueryClient();

    return (
        <ApiKeysProvider
            value={{
                useApiKeysQuery: () => {
                    const {
                        data: {apiKeys} = {},
                        error,
                        isLoading,
                    } = useApiKeysQuery({
                        environmentId: `${environmentId}`,
                        type: ModeType.Embedded,
                    });

                    return {
                        data: apiKeys,
                        error,
                        isLoading,
                    } as UseQueryResult<ApiKey[], Error>;
                },
                useCreateApiKeyMutation: (props?: CreateApiKeyMutationProps) => {
                    const createApiKeyMutation = useCreateApiKeyMutation({
                        onSuccess: (data: CreateApiKeyMutation) => {
                            queryClient.invalidateQueries({
                                queryKey: ['apiKeys'],
                            });

                            props?.onSuccess?.(data.createApiKey);
                        },
                    }) as UseMutationResult<unknown, Error>;

                    return {
                        mutate: (apiKey: ApiKey) => {
                            createApiKeyMutation.mutate({
                                ...apiKey,
                                environmentId: environmentId,
                                type: ModeType.Embedded,
                            });
                        },
                    } as UseMutationResult<unknown, Error>;
                },

                useDeleteApiKeyMutation: (props?: SimpleMutationProps) => {
                    const deleteApiKeyMutation = useDeleteApiKeyMutation({
                        onSuccess: (data) => {
                            queryClient.invalidateQueries({
                                queryKey: ['apiKeys'],
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

                useUpdateApiKeyMutation: <TVariables = unknown,>(props?: SimpleMutationProps) =>
                    useUpdateApiKeyMutation({
                        onSuccess: (data) => {
                            queryClient.invalidateQueries({
                                queryKey: ['apiKeys'],
                            });

                            props?.onSuccess?.(data.updateApiKey);
                        },
                    }) as unknown as UseMutationResult<unknown, Error, TVariables>,
            }}
        >
            <ApiKeysContent
                description="Do not share your API key with others or expose it in the browser or other client-side code."
                title="API Keys"
            />
        </ApiKeysProvider>
    );
};

export default ApiKeys;
