import {useQuery} from '@tanstack/react-query';

import {ConnectionsApi, TagModel} from '../middleware/connection';

export const ConnectionKeys = {
    connection: (id: number) => ['connections', id],
    connectionList: (filters: {
        componentNames?: string[];
        tagIds?: number[];
    }) => [...ConnectionKeys.connections, filters],
    connectionTags: ['connectionTags'] as const,
    connections: ['connections'] as const,
};

export const useGetConnectionTagsQuery = () =>
    useQuery<TagModel[], Error>(
        ConnectionKeys.connectionTags,
        () => new ConnectionsApi().getConnectionTags(),
        {
            staleTime: 1 * 60 * 1000,
        }
    );
