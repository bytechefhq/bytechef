import {useMutation} from '@tanstack/react-query';
import {
    ConnectionModel,
    ConnectionsApi,
    PutConnectionTagsRequest,
} from '../middleware/connection';

type ConnectionMutationProps = {
    onSuccess?: (result: ConnectionModel, variables: ConnectionModel) => void;
    onError?: (error: object, variables: ConnectionModel) => void;
};

type ConnectionMutationDeleteProps = {
    onSuccess?: () => void;
    onError?: (error: object, id: number) => void;
};

export const useConnectionCreateMutation = (
    mutationProps?: ConnectionMutationProps
) =>
    useMutation({
        mutationFn: (connectionModel: ConnectionModel) => {
            return new ConnectionsApi().postConnection({
                connectionModel,
            });
        },
        onSuccess: mutationProps?.onSuccess,
        onError: mutationProps?.onError,
    });

export const useConnectionDeleteMutation = (
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
    onSuccess?: (result: void, variables: PutConnectionTagsRequest) => void;
    onError?: (error: object, variables: PutConnectionTagsRequest) => void;
};

export const useConnectionTagsMutation = (
    mutationProps?: ConnectionTagsMutationProps
) =>
    useMutation({
        mutationFn: (request: PutConnectionTagsRequest) => {
            return new ConnectionsApi().putConnectionTags(request);
        },
        onSuccess: mutationProps?.onSuccess,
        onError: mutationProps?.onError,
    });
