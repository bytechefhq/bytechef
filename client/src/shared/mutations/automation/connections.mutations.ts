import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {Connection, ConnectionApi} from '@/shared/middleware/automation/configuration';
import {
    ConnectionCredentialStoreType,
    useDisconnectConnectionMutation as useDisconnectConnectionGraphQL,
    useRegisterExistingConnectionMutation as useRegisterExistingConnectionGraphQL,
    useUpdateConnectionCredentialsMutation as useUpdateConnectionCredentialsGraphQL,
} from '@/shared/middleware/graphql';
import {useMutation} from '@tanstack/react-query';

interface CreateConnectionMutationProps {
    onSuccess?: (result: number, variables: Connection) => void;
    onError?: (error: Error, variables: Connection) => void;
}

export const useCreateConnectionMutation = (mutationProps?: CreateConnectionMutationProps) => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    return useMutation<number, Error, Connection>({
        mutationFn: (connection: Connection) => {
            return new ConnectionApi().createConnection({
                connection: {
                    ...connection,
                    workspaceId: currentWorkspaceId,
                },
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
};

interface RegisterExistingConnectionMutationInputI {
    componentName: string;
    connectionVersion: number;
    credentialRef: string;
    credentialStoreType: ConnectionCredentialStoreType;
    environmentId: string;
    name: string;
}

interface RegisterExistingConnectionMutationProps {
    onError?: (error: Error) => void;
    onSuccess?: (connectionId: number) => void;
}

export const useRegisterExistingConnectionMutation = (mutationProps?: RegisterExistingConnectionMutationProps) => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const graphqlMutation = useRegisterExistingConnectionGraphQL();

    return useMutation<number, Error, RegisterExistingConnectionMutationInputI>({
        mutationFn: async (input) => {
            const result = await graphqlMutation.mutateAsync({
                input: {
                    ...input,
                    workspaceId: String(currentWorkspaceId!),
                },
            });

            return result.registerExistingConnection;
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
};

interface DeleteConnectionMutationProps {
    onSuccess?: () => void;
    onError?: (error: Error, id: number) => void;
}

export const useDeleteConnectionMutation = (mutationProps?: DeleteConnectionMutationProps) =>
    useMutation<void, Error, number>({
        mutationFn: (id: number) => {
            return new ConnectionApi().deleteConnection({
                id,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface DisconnectConnectionMutationProps {
    onSuccess?: () => void;
    onError?: (error: Error) => void;
}

export const useDisconnectConnectionMutation = (mutationProps?: DisconnectConnectionMutationProps) => {
    const graphqlMutation = useDisconnectConnectionGraphQL();

    return useMutation<boolean, Error, number>({
        mutationFn: async (connectionId: number) => {
            const result = await graphqlMutation.mutateAsync({
                connectionId: connectionId.toString(),
            });

            return result.disconnectConnection;
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
};

interface UpdateConnectionCredentialsMutationProps {
    onError?: (error: Error, variables: Connection) => void;
    onSuccess?: (result: void, variables: Connection) => void;
}

/**
 * Submits replacement credentials for an existing connection. Deliberately shaped like
 * `useUpdateConnectionMutation` — same `Connection` variables, same `void` result — so `ConnectionDialog` can accept
 * either through a prop of the same type. Only `id`, `parameters` and `version` are read: the server replaces the
 * connection's authorization parameters wholesale and ignores everything else on the object.
 */
export const useUpdateConnectionCredentialsMutation = (mutationProps?: UpdateConnectionCredentialsMutationProps) => {
    const graphqlMutation = useUpdateConnectionCredentialsGraphQL();

    return useMutation<void, Error, Connection>({
        mutationFn: async (connection: Connection) => {
            await graphqlMutation.mutateAsync({
                input: {
                    connectionId: String(connection.id!),
                    parameters: connection.parameters,
                    version: connection.version!,
                },
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
};

interface UpdateConnectionMutationProps {
    onSuccess?: (result: void, variables: Connection) => void;
    onError?: (error: Error, variables: Connection) => void;
}

export const useUpdateConnectionMutation = (mutationProps?: UpdateConnectionMutationProps) =>
    useMutation<void, Error, Connection>({
        mutationFn: (connection: Connection) => {
            return new ConnectionApi().updateConnection({
                id: connection.id!,
                updateConnectionRequest: {
                    name: connection.name,
                    tags: connection.tags!,
                    version: connection.version!,
                },
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
