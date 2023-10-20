import {useQuery} from '@tanstack/react-query';
import {
    ConnectionModel,
    ConnectionsApi,
    TagModel,
} from '../middleware/connection';

export const ConnectionKeys = {
    connection: (id: number) => [...ConnectionKeys.connections, id],
    connectionList: (filters: {
        componentNames?: string[];
        tagIds?: number[];
    }) => [...ConnectionKeys.connections, filters],
    connectionTags: ['connectionTags'] as const,
    connections: ['connections'] as const,
};

export const useGetConnectionQuery = (id: number) =>
    useQuery<ConnectionModel, Error>(ConnectionKeys.connection(id), () =>
        new ConnectionsApi().getConnection({id})
    );

export const useGetConnectionsQuery = (filters: {
    componentNames?: string[];
    tagIds?: number[];
}) =>
    useQuery<ConnectionModel[], Error>(
        ConnectionKeys.connectionList(filters),
        () => new ConnectionsApi().getConnections(filters)
    );

export const useGetConnectionTagsQuery = () =>
    useQuery<TagModel[], Error>(ConnectionKeys.connectionTags, () =>
        new ConnectionsApi().getConnectionTags()
    );
