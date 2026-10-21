import ApiKeysContent from '@/ee/shared/components/api-keys/ApiKeysContent';
import {
    ApiKeysProvider,
    CreateApiKeyMutationProps,
    SimpleMutationProps,
} from '@/ee/shared/components/api-keys/providers/apiKeysProvider';
import {useHasEmbeddedScope} from '@/shared/hooks/useHasEmbeddedScope';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
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

const ApiKeys = () => {
    const environmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const canCreateApiKey = useHasEmbeddedScope('API_KEY_CREATE');
    const canDeleteApiKey = useHasEmbeddedScope('API_KEY_DELETE');
    const canViewApiKeys = useHasEmbeddedScope('API_KEY_VIEW');

    const queryClient = useQueryClient();

    if (!canViewApiKeys) {
        return (
            <LayoutContainer header={<Header centerTitle position="main" title="API Keys" />} leftSidebarOpen={false}>
                <div className="p-4 text-sm text-muted-foreground">
                    You do not have permission to view the API keys of this environment.
                </div>
            </LayoutContainer>
        );
    }

    return (
        <ApiKeysProvider
            value={{
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
                            queryClient.invalidateQueries({queryKey: EMBEDDED_API_KEYS_QUERY_KEY});

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
                            queryClient.invalidateQueries({queryKey: EMBEDDED_API_KEYS_QUERY_KEY});

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
                            queryClient.invalidateQueries({queryKey: EMBEDDED_API_KEYS_QUERY_KEY});

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
            }}
        >
            <ApiKeysContent
                canCreate={canCreateApiKey}
                canDelete={canDeleteApiKey}
                canEdit={canCreateApiKey}
                description="Do not share your API key with others or expose it in the browser or other client-side code."
                title="API Keys"
            />
        </ApiKeysProvider>
    );
};

export default ApiKeys;
