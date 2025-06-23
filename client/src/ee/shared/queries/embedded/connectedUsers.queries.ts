import {GetConnectedUsersRequest, Page} from '@/ee/shared/middleware/embedded/connected-user';
import {ConnectedUserApi} from '@/ee/shared/middleware/embedded/connected-user/apis/ConnectedUserApi';
import {ConnectedUser} from '@/ee/shared/middleware/embedded/connected-user/models/ConnectedUser';
import {GetWorkflowExecutionsPageRequest} from '@/shared/middleware/automation/workflow/execution';

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
    useQuery<ConnectedUser, Error>({
        queryKey: ConnectedUserKeys.connectedUser(id),
        queryFn: () => new ConnectedUserApi().getConnectedUser({id}),
        enabled: enabled === undefined ? true : enabled,
    });

export const useGetConnectedUsersQuery = (request: GetConnectedUsersRequest) =>
    useQuery<Page, Error>({
        queryKey: ConnectedUserKeys.filteredConnectedUsers(request),
        queryFn: () => new ConnectedUserApi().getConnectedUsers(request),
    });
