import ApiKeysContent from '@/ee/shared/components/api-keys/ApiKeysContent';
import {
    ApiKeysProvider,
    CreateApiKeyMutationProps,
    SimpleMutationProps,
} from '@/ee/shared/components/api-keys/providers/apiKeysProvider';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {
    ApiKey,
    useCreateWorkspaceApiKeyMutation,
    useDeleteWorkspaceApiKeyMutation,
    useUpdateWorkspaceApiKeyMutation,
    useWorkspaceApiKeysQuery,
} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {UseMutationResult, UseQueryResult, useQueryClient} from '@tanstack/react-query';

const WorkspaceApiKeys = () => {
    const environmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const workspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const queryClient = useQueryClient();

    return (
        <ApiKeysProvider
            value={{
                useApiKeysQuery: () => {
                    const {
                        data: {workspaceApiKeys} = {},
                        error,
                        isLoading,
                    } = useWorkspaceApiKeysQuery({
                        environmentId: `${environmentId}`,
                        workspaceId: `${workspaceId}`,
                    });

                    return {
                        data: workspaceApiKeys,
                        error,
                        isLoading,
                    } as UseQueryResult<ApiKey[], Error>;
                },
                useCreateApiKeyMutation: (props?: CreateApiKeyMutationProps) => {
                    const createWorkspaceApiKeyMutation = useCreateWorkspaceApiKeyMutation({
                        onError: (error) => {
                            props?.onError?.(error as unknown as Error);
                        },
                        onSuccess: (data) => {
                            queryClient.invalidateQueries({queryKey: ['workspaceApiKeys']});

                            props?.onSuccess?.(data.createWorkspaceApiKey);
                        },
                    }) as UseMutationResult<unknown, Error>;

                    return {
                        mutate: (apiKey: ApiKey) => {
                            createWorkspaceApiKeyMutation.mutate({
                                ...apiKey,
                                environmentId: environmentId,
                                workspaceId: workspaceId,
                            });
                        },
                    } as UseMutationResult<unknown, Error>;
                },

                useDeleteApiKeyMutation: (props?: SimpleMutationProps) => {
                    const deleteWorkspaceApiKeyMutation = useDeleteWorkspaceApiKeyMutation({
                        onError: (error) => {
                            props?.onError?.(error as unknown as Error);
                        },
                        onSuccess: (data) => {
                            queryClient.invalidateQueries({queryKey: ['workspaceApiKeys']});

                            props?.onSuccess?.(data.deleteWorkspaceApiKey);
                        },
                    });

                    return {
                        mutate: ({apiKeyId}: {apiKeyId: string}) => {
                            deleteWorkspaceApiKeyMutation.mutate({apiKeyId});
                        },
                    } as UseMutationResult<unknown, Error>;
                },

                useUpdateApiKeyMutation: (props?: SimpleMutationProps) => {
                    const updateWorkspaceApiKeyMutation = useUpdateWorkspaceApiKeyMutation({
                        onError: (error) => {
                            props?.onError?.(error as unknown as Error);
                        },
                        onSuccess: (data) => {
                            queryClient.invalidateQueries({queryKey: ['workspaceApiKeys']});

                            props?.onSuccess?.(data.updateWorkspaceApiKey);
                        },
                    });

                    return {
                        mutate: (apiKey: ApiKey) => {
                            updateWorkspaceApiKeyMutation.mutate({
                                apiKeyId: apiKey.id!,
                                name: apiKey.name!,
                            });
                        },
                    } as UseMutationResult<unknown, Error>;
                },
            }}
        >
            <ApiKeysContent
                description="Do not share your API key with others or expose it in the browser or other client-side code."
                title="API Keys"
            />
        </ApiKeysProvider>
    );
};

export default WorkspaceApiKeys;
