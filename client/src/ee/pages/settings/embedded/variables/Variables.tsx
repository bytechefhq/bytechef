import VariablesContent from '@/ee/shared/components/variables/VariablesContent';
import {
    SimpleMutationProps,
    VariableFormValuesI,
    VariablesProvider,
} from '@/ee/shared/components/variables/providers/variablesProvider';
import {
    Variable,
    useCreateEmbeddedVariableMutation,
    useDeleteEmbeddedVariableMutation,
    useEmbeddedVariablesQuery,
    useUpdateEmbeddedVariableMutation,
} from '@/shared/middleware/graphql';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {UseMutationResult, UseQueryResult, useQueryClient} from '@tanstack/react-query';
import {useShallow} from 'zustand/react/shallow';

const TENANT_ADMIN_AUTHORITY = 'ROLE_ADMIN';

const Variables = () => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const {account} = useAuthenticationStore(useShallow((state) => ({account: state.account})));

    const queryClient = useQueryClient();

    const environmentId = `${currentEnvironmentId}`;

    const canManage = (account?.authorities ?? []).includes(TENANT_ADMIN_AUTHORITY);

    const invalidateEmbeddedVariables = () => queryClient.invalidateQueries({queryKey: ['embeddedVariables']});

    return (
        <VariablesProvider
            value={{
                canManage,
                useCreateVariableMutation: (props?: SimpleMutationProps) => {
                    const createEmbeddedVariableMutation = useCreateEmbeddedVariableMutation({
                        onError: (error) => props?.onError?.(error as unknown as Error),
                        onSuccess: (data) => {
                            invalidateEmbeddedVariables();

                            props?.onSuccess?.(data.createEmbeddedVariable);
                        },
                    }) as UseMutationResult<unknown, Error>;

                    return {
                        mutate: (input: VariableFormValuesI) => {
                            createEmbeddedVariableMutation.mutate({environmentId, input});
                        },
                    } as UseMutationResult<unknown, Error>;
                },

                useDeleteVariableMutation: (props?: SimpleMutationProps) => {
                    const deleteEmbeddedVariableMutation = useDeleteEmbeddedVariableMutation({
                        onError: (error) => props?.onError?.(error as unknown as Error),
                        onSuccess: (data) => {
                            invalidateEmbeddedVariables();

                            props?.onSuccess?.(data.deleteEmbeddedVariable);
                        },
                    });

                    return {
                        mutate: ({id}: {id: string}) => {
                            deleteEmbeddedVariableMutation.mutate({environmentId, id});
                        },
                    } as UseMutationResult<unknown, Error>;
                },

                useUpdateVariableMutation: (props?: SimpleMutationProps) => {
                    const updateEmbeddedVariableMutation = useUpdateEmbeddedVariableMutation({
                        onError: (error) => props?.onError?.(error as unknown as Error),
                        onSuccess: (data) => {
                            invalidateEmbeddedVariables();

                            props?.onSuccess?.(data.updateEmbeddedVariable);
                        },
                    });

                    return {
                        mutate: ({id, name, value}: {id: string} & VariableFormValuesI) => {
                            updateEmbeddedVariableMutation.mutate({environmentId, id, input: {name, value}});
                        },
                    } as UseMutationResult<unknown, Error>;
                },

                useVariablesQuery: () => {
                    const {
                        data: {embeddedVariables} = {},
                        error,
                        isLoading,
                    } = useEmbeddedVariablesQuery({environmentId});

                    return {
                        data: embeddedVariables,
                        error,
                        isLoading,
                    } as UseQueryResult<Variable[], Error>;
                },
            }}
        >
            <VariablesContent
                description="Reusable values every integration workflow can reference as ${vars.NAME}."
                title="Variables"
            />
        </VariablesProvider>
    );
};

export default Variables;
