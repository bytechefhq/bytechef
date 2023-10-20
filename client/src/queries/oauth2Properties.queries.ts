import {
    ConnectionOauth2PropertiesApi,
    OAuth2PropertiesModel,
} from '@/middleware/hermes/connection';
import {useQuery} from '@tanstack/react-query';

export const OAuth2PropertiesKeys = {
    oAuth2Properties: ['oAuth2Properties'] as const,
};

export const useGetOAuth2PropertiesQuery = () =>
    useQuery<OAuth2PropertiesModel, Error>(
        OAuth2PropertiesKeys.oAuth2Properties,
        () => new ConnectionOauth2PropertiesApi().getOAuth2Properties()
    );
