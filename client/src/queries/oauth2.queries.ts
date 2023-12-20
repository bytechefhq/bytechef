/* eslint-disable sort-keys */
import {
    GetOAuth2AuthorizationParametersRequestModel,
    OAuth2AuthorizationParametersModel,
    OAuth2PropertiesModel,
} from '@/middleware/hermes/configuration';
import {Oauth2Api} from '@/middleware/hermes/configuration/apis/Oauth2Api';
import {ConnectionKeys} from '@/queries/connections.queries';
import {useQuery} from '@tanstack/react-query';

export const OAuth2Keys = {
    oAuth2AuthorizationParameters: (request: GetOAuth2AuthorizationParametersRequestModel) => [
        ...ConnectionKeys.connections,
        request,
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
