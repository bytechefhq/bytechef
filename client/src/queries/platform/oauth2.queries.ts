/* eslint-disable sort-keys */
import {
    GetOAuth2AuthorizationParametersRequestModel,
    OAuth2AuthorizationParametersModel,
    OAuth2PropertiesModel,
} from '@/middleware/platform/configuration';
import {Oauth2Api} from '@/middleware/platform/configuration/apis/Oauth2Api';
import {ConnectionKeys} from '@/queries/automation/connections.queries';
import {useQuery} from '@tanstack/react-query';

export const OAuth2Keys = {
    oAuth2AuthorizationParameters: (request: GetOAuth2AuthorizationParametersRequestModel) => [
        ...ConnectionKeys.connections,
        request.componentName,
        request.connectionVersion,
        request.authorizationName,
        request.parameters,
    ],
    oAuth2Properties: ['oAuth2Properties'] as const,
};

export const useGetOAuth2AuthorizationParametersQuery = (
    request: GetOAuth2AuthorizationParametersRequestModel,
    enabled?: boolean
) =>
    useQuery<OAuth2AuthorizationParametersModel, Error>({
        queryKey: OAuth2Keys.oAuth2AuthorizationParameters(request),
        queryFn: () =>
            new Oauth2Api().getOAuth2AuthorizationParameters({
                getOAuth2AuthorizationParametersRequestModel: request,
            }),
        enabled: enabled === undefined ? true : enabled,
    });

export const useGetOAuth2PropertiesQuery = () =>
    useQuery<OAuth2PropertiesModel, Error>({
        queryKey: OAuth2Keys.oAuth2Properties,
        queryFn: () => new Oauth2Api().getOAuth2Properties(),
    });
