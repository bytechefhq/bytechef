import {
    ConnectionModel,
    GetConnectionsRequest,
    ProjectConnectionApi,
    ProjectConnectionTagApi,
    TagModel,
} from '@/middleware/helios/connection';
import {
    GetOAuth2AuthorizationParametersRequestModel,
    OAuth2AuthorizationParametersModel,
    Oauth2Api,
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
        () => new ProjectConnectionApi().getConnections(filters),
        {enabled: false || enabledCondition}
    );

export const useGetConnectionTagsQuery = () =>
    useQuery<TagModel[], Error>(ConnectionKeys.connectionTags, () =>
        new ProjectConnectionTagApi().getConnectionTags()
    );

export const useGetOAuth2AuthorizationParametersQuery = (
    request: GetOAuth2AuthorizationParametersRequestModel,
    enabled: boolean
) =>
    useQuery<OAuth2AuthorizationParametersModel, Error>(
        ConnectionKeys.connectionOAuth2AuthorizationParameters(request),
        () =>
            new Oauth2Api().getOAuth2AuthorizationParameters({
                getOAuth2AuthorizationParametersRequestModel: request,
            }),
        {
            enabled,
        }
    );
