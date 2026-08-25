import VariablesContent from '@/ee/shared/components/variables/VariablesContent';
import {
    SimpleMutationProps,
    VariableFormValuesI,
    VariablesProvider,
} from '@/ee/shared/components/variables/providers/variablesProvider';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {
    Variable,
    useCreateWorkspaceVariableMutation,
    useDeleteWorkspaceVariableMutation,
    useMyWorkspaceScopesQuery,
    useUpdateWorkspaceVariableMutation,
    useWorkspaceVariablesQuery,
} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {UseMutationResult, UseQueryResult, useQueryClient} from '@tanstack/react-query';

const VARIABLE_MANAGE_SCOPE = 'VARIABLE_MANAGE';

const Variables = () => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const queryClient = useQueryClient();

    const environmentId = `${currentEnvironmentId}`;
    const workspaceId = `${currentWorkspaceId}`;

    // Gated on the scope rather than on ROLE_ADMIN: a workspace admin need not be a tenant admin, which is what
    // makes this page reachable by both (see WorkspaceUsers for the same precedent).
    const {data: scopesData} = useMyWorkspaceScopesQuery({workspaceId});

    const canManage = (scopesData?.myWorkspaceScopes ?? []).includes(VARIABLE_MANAGE_SCOPE);

    const invalidateWorkspaceVariables = () => queryClient.invalidateQueries({queryKey: ['workspaceVariables']});

    return (
        <VariablesProvider
            value={{
                canManage,
                useCreateVariableMutation: (props?: SimpleMutationProps) => {
                    const createWorkspaceVariableMutation = useCreateWorkspaceVariableMutation({
                        onError: (error) => props?.onError?.(error as unknown as Error),
                        onSuccess: (data) => {
                            invalidateWorkspaceVariables();

                            props?.onSuccess?.(data.createWorkspaceVariable);
                        },
                    }) as UseMutationResult<unknown, Error>;

                    return {
                        mutate: (input: VariableFormValuesI) => {
                            createWorkspaceVariableMutation.mutate({environmentId, input, workspaceId});
                        },
                    } as UseMutationResult<unknown, Error>;
                },

                useDeleteVariableMutation: (props?: SimpleMutationProps) => {
                    const deleteWorkspaceVariableMutation = useDeleteWorkspaceVariableMutation({
                        onError: (error) => props?.onError?.(error as unknown as Error),
                        onSuccess: (data) => {
                            invalidateWorkspaceVariables();

                            props?.onSuccess?.(data.deleteWorkspaceVariable);
                        },
                    });

                    return {
                        mutate: ({id}: {id: string}) => {
                            deleteWorkspaceVariableMutation.mutate({environmentId, id, workspaceId});
                        },
                    } as UseMutationResult<unknown, Error>;
                },

                useUpdateVariableMutation: (props?: SimpleMutationProps) => {
                    const updateWorkspaceVariableMutation = useUpdateWorkspaceVariableMutation({
                        onError: (error) => props?.onError?.(error as unknown as Error),
                        onSuccess: (data) => {
                            invalidateWorkspaceVariables();

                            props?.onSuccess?.(data.updateWorkspaceVariable);
                        },
                    });

                    return {
                        mutate: ({id, name, value}: {id: string} & VariableFormValuesI) => {
                            updateWorkspaceVariableMutation.mutate({
                                environmentId,
                                id,
                                input: {name, value},
                                workspaceId,
                            });
                        },
                    } as UseMutationResult<unknown, Error>;
                },

                useVariablesQuery: () => {
                    const {
                        data: {workspaceVariables} = {},
                        error,
                        isLoading,
                    } = useWorkspaceVariablesQuery({environmentId, workspaceId});

                    return {
                        data: workspaceVariables,
                        error,
                        isLoading,
                    } as UseQueryResult<Variable[], Error>;
                },
            }}
        >
            <VariablesContent
                description="Reusable values every workflow in this workspace can reference as ${vars.NAME}."
                title="Variables"
            />
        </VariablesProvider>
    );
};

export default Variables;
