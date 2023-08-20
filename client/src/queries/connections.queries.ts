import {
    AutomationConnectionApi,
    AutomationConnectionTagApi,
    ConnectionModel,
    GetConnectionsRequest,
    TagModel,
} from '@/middleware/helios/connection';
import {
    ConnectionDefinitionApi,
    GetOAuth2AuthorizationParametersRequestModel,
    OAuth2AuthorizationParametersModel,
} from '@/middleware/hermes/configuration';
import {useQuery} from '@tanstack/react-query';

export const ConnectionKeys = {
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

export const useGetConnectionsQuery = (
    filters: GetConnectionsRequest,
    enabledCondition?: boolean
) =>
    useQuery<ConnectionModel[], Error>(
        ConnectionKeys.connectionList(filters),
        () => new AutomationConnectionApi().getConnections(filters),
        {enabled: false || enabledCondition}
    );

export const useGetConnectionTagsQuery = () =>
    useQuery<TagModel[], Error>(ConnectionKeys.connectionTags, () =>
        new AutomationConnectionTagApi().getConnectionTags()
    );

export const useGetOAuth2AuthorizationParametersQuery = (
    request: GetOAuth2AuthorizationParametersRequestModel,
    enabled: boolean
) =>
    useQuery<OAuth2AuthorizationParametersModel, Error>(
        ConnectionKeys.connectionOAuth2AuthorizationParameters(request),
        () =>
            new ConnectionDefinitionApi().getOAuth2AuthorizationParameters({
                getOAuth2AuthorizationParametersRequestModel: request,
            }),
        {
            enabled,
        }
    );
