import {DEVELOPMENT_ENVIRONMENT, PRODUCTION_ENVIRONMENT, STAGING_ENVIRONMENT} from '@/shared/constants';
import {getDevelopmentOnlyFallbackHref, isDevelopmentOnlyHref} from '@/shared/navigation/developmentOnlyRoutes';
import {describe, expect, it} from 'vitest';

describe('isDevelopmentOnlyHref', () => {
    it.each(['/automation/projects', '/embedded/integrations', '/embedded/automation-workflows'])(
        'reports %s as development only',
        (href) => {
            expect(isDevelopmentOnlyHref(href)).toBe(true);
        }
    );

    it.each([
        '/automation/deployments',
        '/automation/connections',
        '/embedded/automation-configurations',
        '/embedded/configurations',
        '/embedded/connections',
    ])('leaves the deployed surface %s alone', (href) => {
        expect(isDevelopmentOnlyHref(href)).toBe(false);
    });

    it('matches the nav href exactly rather than by prefix', () => {
        expect(isDevelopmentOnlyHref('/automation/projects/123')).toBe(false);
    });
});

describe('getDevelopmentOnlyFallbackHref', () => {
    it('keeps every route in the development environment', () => {
        expect(getDevelopmentOnlyFallbackHref('/automation/projects', DEVELOPMENT_ENVIRONMENT)).toBeUndefined();
        expect(getDevelopmentOnlyFallbackHref('/embedded/integrations', DEVELOPMENT_ENVIRONMENT)).toBeUndefined();
    });

    it.each([
        ['/automation/projects', '/automation/deployments'],
        ['/embedded/integrations', '/embedded/configurations'],
        ['/embedded/automation-workflows', '/embedded/automation-configurations'],
    ])('sends %s to %s in staging', (pathname, fallbackHref) => {
        expect(getDevelopmentOnlyFallbackHref(pathname, STAGING_ENVIRONMENT)).toBe(fallbackHref);
    });

    it.each([
        ['/automation/projects/12/project-workflows/34', '/automation/deployments'],
        ['/automation/projects/templates', '/automation/deployments'],
        ['/embedded/integrations/12/integration-workflows/34', '/embedded/configurations'],
        ['/embedded/automation-workflows/12/editor', '/embedded/automation-configurations'],
    ])('covers the detail route %s in production', (pathname, fallbackHref) => {
        expect(getDevelopmentOnlyFallbackHref(pathname, PRODUCTION_ENVIRONMENT)).toBe(fallbackHref);
    });

    it('leaves deployed surfaces in place outside development', () => {
        expect(getDevelopmentOnlyFallbackHref('/automation/deployments', PRODUCTION_ENVIRONMENT)).toBeUndefined();
        expect(getDevelopmentOnlyFallbackHref('/embedded/configurations', PRODUCTION_ENVIRONMENT)).toBeUndefined();
    });

    it('does not match a sibling route that merely shares a prefix', () => {
        expect(getDevelopmentOnlyFallbackHref('/automation/projects-archive', PRODUCTION_ENVIRONMENT)).toBeUndefined();
    });
});
