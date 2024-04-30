/* eslint-disable sort-keys */
import {
    ConnectionApi,
    ConnectionModel,
    ConnectionTagApi,
    GetConnectionsRequest,
} from '@/middleware/embedded/connection';
import {TagModel} from '@/middleware/platform/connection';
import {useQuery} from '@tanstack/react-query';

export const ConnectionKeys = {
    connection: (id: number) => [...ConnectionKeys.connections, id],
    connectionTags: ['integrationConnectionTags'],
    connections: ['integrationConnections'],
    filteredConnections: (filters: GetConnectionsRequest) => [...ConnectionKeys.connections, filters],
};

export const useGetConnectionsQuery = (filters: GetConnectionsRequest, enabled?: boolean) =>
    useQuery<ConnectionModel[], Error>({
        queryKey: ConnectionKeys.filteredConnections(filters),
        queryFn: () => new ConnectionApi().getConnections(filters),
        enabled: enabled === undefined ? true : enabled,
    });

export const useGetConnectionTagsQuery = () =>
    useQuery<TagModel[], Error>({
        queryKey: ConnectionKeys.connectionTags,
        queryFn: () => new ConnectionTagApi().getConnectionTags(),
    });
