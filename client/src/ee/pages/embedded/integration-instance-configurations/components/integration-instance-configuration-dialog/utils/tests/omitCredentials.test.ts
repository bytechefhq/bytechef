import {IntegrationInstanceConfiguration} from '@/ee/shared/middleware/embedded/configuration';
import {describe, expect, it} from 'vitest';

import {omitCredentials} from '../omitCredentials';

describe('omitCredentials', () => {
    it('drops the three credential maps and keeps every other field', () => {
        const integrationInstanceConfiguration = {
            connectionAuthorizationParameters: {value: 'secret-api-key-value'},
            connectionConnectionParameters: {subdomain: 'acme-subdomain'},
            connectionParameters: {subdomain: 'acme-subdomain', value: 'secret-api-key-value'},
            enabled: true,
            id: 1,
            integrationId: 2,
            integrationVersion: 3,
            name: 'Existing',
        } as unknown as IntegrationInstanceConfiguration;

        const result = omitCredentials(integrationInstanceConfiguration);

        expect(result).toEqual({enabled: true, id: 1, integrationId: 2, integrationVersion: 3, name: 'Existing'});
        expect(integrationInstanceConfiguration).toHaveProperty('connectionParameters');
    });

    it('returns the configuration unchanged when it carries no credentials', () => {
        expect(omitCredentials({integrationId: 2, name: 'Plain'} as IntegrationInstanceConfiguration)).toEqual({
            integrationId: 2,
            name: 'Plain',
        });
    });
});
