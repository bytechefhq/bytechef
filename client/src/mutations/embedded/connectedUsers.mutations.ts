import {
    ConnectedUserApi,
    DeleteConnectedUserRequest,
    EnableConnectedUserRequest,
} from '@/middleware/embedded/configuration';
import {useMutation} from '@tanstack/react-query';

interface DeleteConnectedUserMutationProps {
    onError?: (error: Error, variables: DeleteConnectedUserRequest) => void;
    onSuccess?: (result: void, variables: DeleteConnectedUserRequest) => void;
}

export const useDeleteConnectedUserMutation = (mutationProps?: DeleteConnectedUserMutationProps) =>
    useMutation<void, Error, DeleteConnectedUserRequest>({
        mutationFn: (deleteConnectedUserRequest: DeleteConnectedUserRequest) => {
            return new ConnectedUserApi().deleteConnectedUser(deleteConnectedUserRequest);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface EnableConnectedUserMutationProps {
    onError?: (error: Error, variables: EnableConnectedUserRequest) => void;
    onSuccess?: (result: void, variables: EnableConnectedUserRequest) => void;
}

export const useEnableConnectedUserMutation = (mutationProps?: EnableConnectedUserMutationProps) =>
    useMutation<void, Error, EnableConnectedUserRequest>({
        mutationFn: (enableConnectedUserRequest: EnableConnectedUserRequest) => {
            return new ConnectedUserApi().enableConnectedUser(enableConnectedUserRequest);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
