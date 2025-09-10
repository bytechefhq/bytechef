import {Connection, ConnectionApi} from '@/ee/shared/middleware/embedded/configuration';
import {useMutation} from '@tanstack/react-query';

interface CreateConnectionMutationProps {
    onSuccess?: (result: number, variables: Connection) => void;
    onError?: (error: Error, variables: Connection) => void;
}

export const getCreateConnectedUserProjectWorkflowConnection =
    (connectedUserId: number, workflowUuid: string) => (mutationProps?: CreateConnectionMutationProps) =>
        useMutation<number, Error, Connection>({
            mutationFn: (connection: Connection) => {
                return new ConnectionApi().createConnectedUserProjectWorkflowConnection({
                    connectedUserId,
                    connection,
                    workflowUuid,
                });
            },
            onError: mutationProps?.onError,
            onSuccess: mutationProps?.onSuccess,
        });

export const useCreateConnectionMutation = (mutationProps?: CreateConnectionMutationProps) =>
    useMutation<number, Error, Connection>({
        mutationFn: (connection: Connection) => {
            return new ConnectionApi().createConnection({
                connection,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

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
