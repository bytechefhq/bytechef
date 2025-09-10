import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {Connection, ConnectionApi} from '@/shared/middleware/automation/configuration';
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
