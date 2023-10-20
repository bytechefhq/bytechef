import {useMutation} from '@tanstack/react-query';
import {
    ConnectionModel,
    ConnectionsApi,
    UpdateConnectionTagsRequest,
} from '../middleware/connection';

type ConnectionMutationProps = {
    onSuccess?: (result: ConnectionModel, variables: ConnectionModel) => void;
    onError?: (error: object, variables: ConnectionModel) => void;
};

type ConnectionMutationDeleteProps = {
    onSuccess?: () => void;
    onError?: (error: object, id: number) => void;
};

export const useCreateConnectionMutation = (
    mutationProps?: ConnectionMutationProps
) =>
    useMutation({
        mutationFn: (connectionModel: ConnectionModel) => {
            return new ConnectionsApi().createConnection({
                connectionModel,
            });
        },
        onSuccess: mutationProps?.onSuccess,
        onError: mutationProps?.onError,
    });

export const useDeleteConnectionMutation = (
    mutationProps?: ConnectionMutationDeleteProps
) =>
    useMutation({
        mutationFn: (id: number) => {
            return new ConnectionsApi().deleteConnection({
                id,
            });
        },
        onSuccess: mutationProps?.onSuccess,
        onError: mutationProps?.onError,
    });

type ConnectionTagsMutationProps = {
    onSuccess?: (result: void, variables: UpdateConnectionTagsRequest) => void;
    onError?: (error: object, variables: UpdateConnectionTagsRequest) => void;
};

export const useConnectionTagsMutation = (
    mutationProps?: ConnectionTagsMutationProps
) =>
    useMutation({
        mutationFn: (request: UpdateConnectionTagsRequest) => {
            return new ConnectionsApi().updateConnectionTags(request);
        },
        onSuccess: mutationProps?.onSuccess,
        onError: mutationProps?.onError,
    });
