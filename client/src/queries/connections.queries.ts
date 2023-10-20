import {useQuery} from '@tanstack/react-query';

import {
    ConnectionModel,
    ConnectionTagsApi,
    ConnectionsApi,
    GetComponentConnectionsRequest,
    GetConnectionsRequest,
    TagModel,
} from '../middleware/automation/connection';
import {
    ConnectionDefinitionsApi,
    GetOAuth2AuthorizationParametersRequestModel,
    OAuth2AuthorizationParametersModel,
} from '../middleware/core/definition-registry';

export const ConnectionKeys = {
    componentConnectionList: (request: GetComponentConnectionsRequest) => [
        ...ConnectionKeys.connections,
        request,
    ],
    connection: (id: number) => [...ConnectionKeys.connections, id],
    connectionList: (filters: GetConnectionsRequest) => [
        ...ConnectionKeys.connections,
        filters,
    ],
    connectionOAuth2AuthorizationParameters: (
        request: GetOAuth2AuthorizationParametersRequestModel
    ) => [...ConnectionKeys.connections, request],
    connectionTags: ['connectionTags'] as const,
    connections: ['connections'] as const,
};

export const useGetComponentConnectionsQuery = (
    request: GetComponentConnectionsRequest,
    enabledCondition?: boolean
) =>
    useQuery<ConnectionModel[], Error>(
        ConnectionKeys.componentConnectionList(request),
        () => new ConnectionsApi().getComponentConnections(request),
        {enabled: false || enabledCondition}
    );

export const useGetConnectionsQuery = (
    filters: GetConnectionsRequest,
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
