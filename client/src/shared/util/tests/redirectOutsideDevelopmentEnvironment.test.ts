import {DEVELOPMENT_ENVIRONMENT, PRODUCTION_ENVIRONMENT, STAGING_ENVIRONMENT} from '@/shared/constants';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import redirectOutsideDevelopmentEnvironment from '@/shared/util/redirectOutsideDevelopmentEnvironment';
import {beforeEach, describe, expect, it} from 'vitest';

describe('redirectOutsideDevelopmentEnvironment', () => {
    beforeEach(() => {
        environmentStore.setState({currentEnvironmentId: DEVELOPMENT_ENVIRONMENT});
    });

    it('does not redirect in the development environment', () => {
        expect(redirectOutsideDevelopmentEnvironment('/automation/deployments')).toBeNull();
    });

    it.each([STAGING_ENVIRONMENT, PRODUCTION_ENVIRONMENT])(
        'redirects to the given path in environment %i',
        (environmentId) => {
            environmentStore.setState({currentEnvironmentId: environmentId});

            const response = redirectOutsideDevelopmentEnvironment('/automation/deployments');

            expect(response?.status).toBe(302);
            expect(response?.headers.get('Location')).toBe('/automation/deployments');
        }
    );
});
