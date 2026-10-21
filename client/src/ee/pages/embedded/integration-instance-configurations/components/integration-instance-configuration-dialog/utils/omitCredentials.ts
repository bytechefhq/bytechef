import {IntegrationInstanceConfiguration} from '@/ee/shared/middleware/embedded/configuration';

const CREDENTIAL_KEYS = [
    'connectionAuthorizationParameters',
    'connectionConnectionParameters',
    'connectionParameters',
] as const;

type CredentialKeyType = (typeof CREDENTIAL_KEYS)[number];

export const omitCredentials = (
    integrationInstanceConfiguration: IntegrationInstanceConfiguration
): Omit<IntegrationInstanceConfiguration, CredentialKeyType> =>
    Object.fromEntries(
        Object.entries(integrationInstanceConfiguration).filter(
            ([key]) => !CREDENTIAL_KEYS.includes(key as CredentialKeyType)
        )
    ) as Omit<IntegrationInstanceConfiguration, CredentialKeyType>;
