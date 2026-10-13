/* eslint-disable sort-keys */
import {
    GetOAuth2AuthorizationParametersRequest,
    OAuth2AuthorizationParameters,
    OAuth2Properties,
} from '@/shared/middleware/platform/configuration';
import {Oauth2Api} from '@/shared/middleware/platform/configuration/apis/Oauth2Api';
import {ConnectionKeys} from '@/shared/queries/automation/connections.queries';
import {useQuery} from '@tanstack/react-query';

const oAuth2AuthorizationParametersPrefix = [...ConnectionKeys.connections, 'oAuth2AuthorizationParameters'];

export const OAuth2Keys = {
    oAuth2AuthorizationParametersPrefix,
    oAuth2AuthorizationParameters: (request: GetOAuth2AuthorizationParametersRequest) => [
        ...oAuth2AuthorizationParametersPrefix,
        request.componentName,
        request.connectionVersion,
        request.authorizationType,
        request.parameters,
    ],
    oAuth2Properties: ['oAuth2Properties'] as const,
};

export const getOAuth2AuthorizationParametersQueryOptions = (
    request: GetOAuth2AuthorizationParametersRequest,
    enabled?: boolean
) => ({
    queryKey: OAuth2Keys.oAuth2AuthorizationParameters(request),
    queryFn: () =>
        new Oauth2Api().getOAuth2AuthorizationParameters({
            getOAuth2AuthorizationParametersRequest: request,
        }),
    enabled: enabled === undefined ? true : enabled,
    gcTime: 0,
    refetchOnReconnect: false,
    refetchOnWindowFocus: false,
    retry: false,
    staleTime: Infinity,
});

export const useGetOAuth2AuthorizationParametersQuery = (
    request: GetOAuth2AuthorizationParametersRequest,
    enabled?: boolean
) => useQuery<OAuth2AuthorizationParameters, Error>(getOAuth2AuthorizationParametersQueryOptions(request, enabled));

export const useGetOAuth2PropertiesQuery = () =>
    useQuery<OAuth2Properties, Error>({
        queryKey: OAuth2Keys.oAuth2Properties,
        queryFn: () => new Oauth2Api().getOAuth2Properties(),
    });
