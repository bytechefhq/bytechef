import {useQuery} from '@tanstack/react-query';

import {
    ConnectionModel,
    ConnectionTagsApi,
    ConnectionsApi,
    OAuth2AuthorizationParametersModel,
    TagModel,
} from '../middleware/connection';

export const ConnectionKeys = {
    connection: (id: number) => [...ConnectionKeys.connections, id],
    connectionList: (filters: {
        componentNames?: string[];
        tagIds?: number[];
    }) => [...ConnectionKeys.connections, filters],
    connectionOAuth2AuthorizationParameters: (connection: ConnectionModel) => [
        ...ConnectionKeys.connections,
        connection,
    ],
    connectionTags: ['connectionTags'] as const,
    connections: ['connections'] as const,
};

export const useGetConnectionQuery = (id: number) =>
    useQuery<ConnectionModel, Error>(ConnectionKeys.connection(id), () =>
        new ConnectionsApi().getConnection({id})
    );

export const useGetConnectionOAuth2AuthorizationParametersQuery = (
    connection: ConnectionModel,
    enabled: boolean
) =>
    useQuery<OAuth2AuthorizationParametersModel, Error>(
        ConnectionKeys.connectionOAuth2AuthorizationParameters(connection),
        () =>
            new ConnectionsApi().getConnectionOAuth2AuthorizationParameters({
                connectionModel: connection,
            }),
        {
            enabled,
        }
    );

export const useGetConnectionsQuery = (
    filters: {
        componentNames?: string[];
        tagIds?: number[];
    },
    enabledCondition?: boolean
) =>
    useQuery<ConnectionModel[], Error>(
        ConnectionKeys.connectionList(filters),
        () => new ConnectionsApi().getConnections(filters),
        {enabled: false || enabledCondition}
    );

export const useGetConnectionTagsQuery = () =>
    useQuery<TagModel[], Error>(ConnectionKeys.connectionTags, () =>
        new ConnectionTagsApi().getConnectionTags()
    );
