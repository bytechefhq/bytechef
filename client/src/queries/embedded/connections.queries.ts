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
    connectionList: (filters: GetConnectionsRequest) => [...ConnectionKeys.connections, filters],
    connectionTags: ['integrationConnectionTags'] as const,
    connections: ['integrationConnections'] as const,
};

export const useGetConnectionsQuery = (filters: GetConnectionsRequest, enabledCondition?: boolean) =>
    useQuery<ConnectionModel[], Error>({
        queryKey: ConnectionKeys.connectionList(filters),
        queryFn: () => new ConnectionApi().getConnections(filters),
        enabled: false || enabledCondition,
    });

export const useGetConnectionTagsQuery = () =>
    useQuery<TagModel[], Error>({
        queryKey: ConnectionKeys.connectionTags,
        queryFn: () => new ConnectionTagApi().getConnectionTags(),
    });
