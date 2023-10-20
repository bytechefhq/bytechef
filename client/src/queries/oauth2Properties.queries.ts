import {useQuery} from '@tanstack/react-query';
import {Oauth2Api, OAuth2PropertiesModel} from '../middleware/connection';

export const OAuth2PropertiesKeys = {
    oAuth2Properties: ['oAuth2Properties'] as const,
};

export const useGetOAuth2PropertiesQuery = () =>
    useQuery<OAuth2PropertiesModel, Error>(
        OAuth2PropertiesKeys.oAuth2Properties,
        () => new Oauth2Api().getOAuth2Properties()
    );
