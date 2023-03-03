import {useMutation} from '@tanstack/react-query';
import {
    ConnectionModel,
    ConnectionsApi,
    UpdateConnectionTagsRequest,
} from '../middleware/connection';

type CreateConnectionMutationProps = {
    onSuccess?: (result: ConnectionModel, variables: ConnectionModel) => void;
    onError?: (error: object, variables: ConnectionModel) => void;
};

export const useCreateConnectionMutation = (
    mutationProps?: CreateConnectionMutationProps
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

type DeleteConnectionMutationProps = {
    onSuccess?: () => void;
    onError?: (error: object, id: number) => void;
};

export const useDeleteConnectionMutation = (
    mutationProps?: DeleteConnectionMutationProps
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

type UpdateConnectionTagsMutationProps = {
    onSuccess?: (result: void, variables: UpdateConnectionTagsRequest) => void;
    onError?: (error: object, variables: UpdateConnectionTagsRequest) => void;
};

export const useUpdateConnectionTagsMutation = (
    mutationProps?: UpdateConnectionTagsMutationProps
) =>
    useMutation({
        mutationFn: (request: UpdateConnectionTagsRequest) => {
            return new ConnectionsApi().updateConnectionTags(request);
        },
        onSuccess: mutationProps?.onSuccess,
        onError: mutationProps?.onError,
    });
