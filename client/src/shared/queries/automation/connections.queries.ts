/* eslint-disable sort-keys */
import {
    Connection,
    ConnectionApi,
    ConnectionTagApi,
    GetWorkspaceConnectionsRequest,
    Tag,
} from '@/shared/middleware/automation/connection';
import {useQuery} from '@tanstack/react-query';

export const ConnectionKeys = {
    connection: (id: number) => [...ConnectionKeys.connections, id],
    connectionTags: ['automation_connectionTags'],
    connections: ['automation_connections'],
    filteredConnections: (filters: {
        id?: number;
        componentName?: string;
        connectionVersion?: number;
        tagId?: number;
    }) => [...ConnectionKeys.connections, filters],
};

export const useGetConnectionTagsQuery = () =>
    useQuery<Tag[], Error>({
        queryKey: ConnectionKeys.connectionTags,
        queryFn: () => new ConnectionTagApi().getConnectionTags(),
    });

export const useGetWorkspaceConnectionsQuery = (request: GetWorkspaceConnectionsRequest, enabled?: boolean) =>
    useQuery<Connection[], Error>({
        queryKey: ConnectionKeys.filteredConnections(request),
        queryFn: () => new ConnectionApi().getWorkspaceConnections(request),
        enabled: enabled === undefined ? true : enabled,
    });
