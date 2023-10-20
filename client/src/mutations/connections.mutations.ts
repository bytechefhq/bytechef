import {useMutation} from '@tanstack/react-query';
import {
    ConnectionModel,
    ConnectionsApi,
    UpdateConnectionTagsRequest,
} from '../middleware/connection';

type CreateConnectionMutationProps = {
    onSuccess?: (result: ConnectionModel, variables: ConnectionModel) => void;
    onError?: (error: Error, variables: ConnectionModel) => void;
};

export const useCreateConnectionMutation = (
    mutationProps?: CreateConnectionMutationProps
) =>
    useMutation<ConnectionModel, Error, ConnectionModel>({
        mutationFn: (connectionModel: ConnectionModel) => {
            return new ConnectionsApi().createConnection({
                connectionModel,
            });
        },
        onSuccess: mutationProps?.onSuccess,
        onError: mutationProps?.onError,
    });

type DeleteConnectionMutationProps = {
    onSuccess?: () => void;
    onError?: (error: Error, id: number) => void;
};

export const useDeleteConnectionMutation = (
    mutationProps?: DeleteConnectionMutationProps
) =>
    useMutation<void, Error, number>({
        mutationFn: (id: number) => {
            return new ConnectionsApi().deleteConnection({
                id,
            });
        },
        onSuccess: mutationProps?.onSuccess,
        onError: mutationProps?.onError,
    });

type UpdateConnectionMutationProps = {
    onSuccess?: (result: ConnectionModel, variables: ConnectionModel) => void;
    onError?: (error: Error, variables: ConnectionModel) => void;
};

export const useUpdateConnectionMutation = (
    mutationProps?: UpdateConnectionMutationProps
) =>
    useMutation<ConnectionModel, Error, ConnectionModel>({
        mutationFn: (connection: ConnectionModel) => {
            return new ConnectionsApi().updateConnection({
                connectionModel: connection,
                id: connection.id!,
            });
        },
        onSuccess: mutationProps?.onSuccess,
        onError: mutationProps?.onError,
    });

type UpdateConnectionTagsMutationProps = {
    onSuccess?: (result: void, variables: UpdateConnectionTagsRequest) => void;
    onError?: (error: object, variables: UpdateConnectionTagsRequest) => void;
};

export const useUpdateConnectionTagsMutation = (
    mutationProps?: UpdateConnectionTagsMutationProps
) =>
    useMutation<void, Error, UpdateConnectionTagsRequest>({
        mutationFn: (request: UpdateConnectionTagsRequest) => {
            return new ConnectionsApi().updateConnectionTags(request);
        },
        onSuccess: mutationProps?.onSuccess,
        onError: mutationProps?.onError,
    });
