import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {Connection, ConnectionApi} from '@/shared/middleware/automation/configuration';
import {
    ConnectionCredentialStoreType,
    useDisconnectConnectionMutation as useDisconnectConnectionGraphQL,
    useRegisterExistingConnectionMutation as useRegisterExistingConnectionGraphQL,
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
