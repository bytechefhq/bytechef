import {useQuery} from '@tanstack/react-query';

import {
    ConnectionModel,
    ConnectionTagsApi,
    ConnectionsApi,
    TagModel,
} from '../middleware/automation/connection';
import {
    ConnectionDefinitionsApi,
    GetOAuth2AuthorizationParametersRequestModel,
    OAuth2AuthorizationParametersModel,
} from '../middleware/core/definition-registry';

export const ConnectionKeys = {
    connection: (id: number) => [...ConnectionKeys.connections, id],
    connectionList: (filters: {
        componentNames?: string[];
        tagIds?: number[];
    }) => [...ConnectionKeys.connections, filters],
    connectionOAuth2AuthorizationParameters: (
        request: GetOAuth2AuthorizationParametersRequestModel
    ) => [...ConnectionKeys.connections, request],
    connectionTags: ['connectionTags'] as const,
    connections: ['connections'] as const,
};

export const useGetConnectionQuery = (id: number) =>
    useQuery<ConnectionModel, Error>(ConnectionKeys.connection(id), () =>
        new ConnectionsApi().getConnection({id})
    );

export const useGetOAuth2AuthorizationParametersQuery = (
    request: GetOAuth2AuthorizationParametersRequestModel,
    enabled: boolean
) =>
    useQuery<OAuth2AuthorizationParametersModel, Error>(
        ConnectionKeys.connectionOAuth2AuthorizationParameters(request),
        () =>
            new ConnectionDefinitionsApi().getOAuth2AuthorizationParameters({
                getOAuth2AuthorizationParametersRequestModel: request,
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
