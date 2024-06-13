import {GetWorkflowExecutionsPageRequest} from '@/shared/middleware/automation/workflow/execution';
import {GetConnectedUsersRequest, PageModel} from '@/shared/middleware/embedded/connected-user';
import {ConnectedUserApi} from '@/shared/middleware/embedded/connected-user/apis/ConnectedUserApi';
import {ConnectedUserModel} from '@/shared/middleware/embedded/connected-user/models/ConnectedUserModel';

/* eslint-disable sort-keys */
import {useQuery} from '@tanstack/react-query';

export const ConnectedUserKeys = {
    filteredConnectedUsers: (request: GetWorkflowExecutionsPageRequest) => [
        ...ConnectedUserKeys.connectedUsers,
        request,
    ],
    connectedUser: (id: number) => ['connectedUsers', id] as const,
    connectedUsers: ['connectedUsers'] as const,
};

export const useGetConnectedUserQuery = (id: number, enabled?: boolean) =>
    useQuery<ConnectedUserModel, Error>({
        queryKey: ConnectedUserKeys.connectedUser(id),
        queryFn: () => new ConnectedUserApi().getConnectedUser({id}),
        enabled: enabled === undefined ? true : enabled,
    });

export const useGetConnectedUsersQuery = (request: GetConnectedUsersRequest) =>
    useQuery<PageModel, Error>({
        queryKey: ConnectedUserKeys.filteredConnectedUsers(request),
        queryFn: () => new ConnectedUserApi().getConnectedUsers(request),
    });
