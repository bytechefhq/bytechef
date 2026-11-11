import {IntegrationInstanceConfiguration} from '@/ee/shared/middleware/embedded/configuration';

export const NO_AUTHORIZATION_TYPE = 'NONE' as unknown as NonNullable<
    IntegrationInstanceConfiguration['authorizationType']
>;
