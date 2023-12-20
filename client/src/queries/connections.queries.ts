/* eslint-disable sort-keys */
import {
    ConnectionApi,
    ConnectionModel,
    ConnectionTagApi,
    GetConnectionsRequest,
    TagModel,
} from '@/middleware/helios/connection';
import {useQuery} from '@tanstack/react-query';

export const ConnectionKeys = {
    connection: (id: number) => [...ConnectionKeys.connections, id],
    connectionList: (filters: GetConnectionsRequest) => [...ConnectionKeys.connections, filters],
    connectionTags: ['connectionTags'] as const,
    connections: ['connections'] as const,
};

export const useGetConnectionsQuery = (filters: GetConnectionsRequest, enabled?: boolean) =>
    useQuery<ConnectionModel[], Error>({
        queryKey: ConnectionKeys.connectionList(filters),
        queryFn: () => new ConnectionApi().getConnections(filters),
        enabled: enabled === undefined ? true : enabled,
    });

export const useGetConnectionTagsQuery = () =>
    useQuery<TagModel[], Error>({
        queryKey: ConnectionKeys.connectionTags,
        queryFn: () => new ConnectionTagApi().getConnectionTags(),
    });
