import {
    ConnectedUser,
    ConnectedUserApi,
    ConnectedUserImportResult,
    CreateConnectedUserOperationRequest,
    DeleteConnectedUserRequest,
    EnableConnectedUserRequest,
    ImportConnectedUsersRequest,
} from '@/ee/shared/middleware/embedded/connected-user';
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

interface CreateConnectedUserMutationProps {
    onError?: (error: Error, variables: CreateConnectedUserOperationRequest) => void;
    onSuccess?: (result: ConnectedUser, variables: CreateConnectedUserOperationRequest) => void;
}

export const useCreateConnectedUserMutation = (mutationProps?: CreateConnectedUserMutationProps) =>
    useMutation<ConnectedUser, Error, CreateConnectedUserOperationRequest>({
        mutationFn: (createConnectedUserOperationRequest: CreateConnectedUserOperationRequest) => {
            return new ConnectedUserApi().createConnectedUser(createConnectedUserOperationRequest);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface ImportConnectedUsersMutationProps {
    onError?: (error: Error, variables: ImportConnectedUsersRequest) => void;
    onSuccess?: (result: ConnectedUserImportResult, variables: ImportConnectedUsersRequest) => void;
}

export const useImportConnectedUsersMutation = (mutationProps?: ImportConnectedUsersMutationProps) =>
    useMutation<ConnectedUserImportResult, Error, ImportConnectedUsersRequest>({
        mutationFn: (importConnectedUsersRequest: ImportConnectedUsersRequest) => {
            return new ConnectedUserApi().importConnectedUsers(importConnectedUsersRequest);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
