/* eslint-disable sort-keys */
import {
    Connection,
    ConnectionApi,
    ConnectionTagApi,
    GetConnectionsRequest,
    Tag,
} from '@/shared/middleware/embedded/connection';
import {useQuery} from '@tanstack/react-query';

export const ConnectionKeys = {
    connection: (id: number) => [...ConnectionKeys.connections, id],
    connectionTags: ['embedded_connectionTags'],
    connections: ['embedded_connections'],
    filteredConnections: (filters: GetConnectionsRequest) => [...ConnectionKeys.connections, filters],
};

export const useGetConnectionsQuery = (filters: GetConnectionsRequest, enabled?: boolean) =>
    useQuery<Connection[], Error>({
        queryKey: ConnectionKeys.filteredConnections(filters),
        queryFn: () => new ConnectionApi().getConnections(filters),
        enabled: enabled === undefined ? true : enabled,
    });

export const useGetConnectionTagsQuery = () =>
    useQuery<Tag[], Error>({
        queryKey: ConnectionKeys.connectionTags,
        queryFn: () => new ConnectionTagApi().getConnectionTags(),
    });
