/* eslint-disable sort-keys */
import {
    ConnectionApi,
    ConnectionModel,
    ConnectionTagApi,
    GetConnectionsRequest,
} from '@/middleware/automation/connection';
import {TagModel} from '@/middleware/platform/connection';
import {useQuery} from '@tanstack/react-query';

export const ConnectionKeys = {
    connection: (id: number) => [...ConnectionKeys.connections, id],
    connectionTags: ['connectionTags'] as const,
    connections: ['connections'] as const,
    filteredConnections: (filters: GetConnectionsRequest) => [...ConnectionKeys.connections, filters],
};

export const useGetConnectionsQuery = (request: GetConnectionsRequest, enabled?: boolean) =>
    useQuery<ConnectionModel[], Error>({
        queryKey: ConnectionKeys.filteredConnections(request),
        queryFn: () => new ConnectionApi().getConnections(request),
        enabled: enabled === undefined ? true : enabled,
    });

export const useGetConnectionTagsQuery = () =>
    useQuery<TagModel[], Error>({
        queryKey: ConnectionKeys.connectionTags,
        queryFn: () => new ConnectionTagApi().getConnectionTags(),
    });
