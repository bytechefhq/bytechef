import {OAuth2PropertiesModel} from '@/middleware/hermes/configuration';
import {Oauth2Api} from '@/middleware/hermes/configuration/apis/Oauth2Api';
import {useQuery} from '@tanstack/react-query';

export const OAuth2PropertiesKeys = {
    oAuth2Properties: ['oAuth2Properties'] as const,
};

export const useGetOAuth2PropertiesQuery = () =>
    useQuery<OAuth2PropertiesModel, Error>(
        OAuth2PropertiesKeys.oAuth2Properties,
        () => new Oauth2Api().getOAuth2Properties()
    );
