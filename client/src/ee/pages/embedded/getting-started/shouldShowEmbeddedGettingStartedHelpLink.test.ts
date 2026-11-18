import {PlatformType} from '@/pages/home/stores/usePlatformTypeStore';
import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {EditionType} from '@/shared/stores/useApplicationInfoStore';
import {describe, expect, it} from 'vitest';

import shouldShowEmbeddedGettingStartedHelpLink from './shouldShowEmbeddedGettingStartedHelpLink';

const STAGING_ENVIRONMENT = 1;

describe('shouldShowEmbeddedGettingStartedHelpLink', () => {
    it('shows the help link for EE admins on embedded pages', () => {
        expect(
            shouldShowEmbeddedGettingStartedHelpLink({
                edition: EditionType.EE,
                environmentId: DEVELOPMENT_ENVIRONMENT,
                isAdmin: true,
                pathname: '/embedded/integrations',
            })
        ).toBe(true);
    });

    it('hides the help link outside embedded pages', () => {
        expect(
            shouldShowEmbeddedGettingStartedHelpLink({
                edition: EditionType.EE,
                environmentId: DEVELOPMENT_ENVIRONMENT,
                isAdmin: true,
                pathname: '/automation/projects',
            })
        ).toBe(false);
        expect(
            shouldShowEmbeddedGettingStartedHelpLink({
                edition: EditionType.EE,
                environmentId: DEVELOPMENT_ENVIRONMENT,
                isAdmin: true,
                pathname: '/embedded-other',
            })
        ).toBe(false);
    });

    it('hides the help link outside Development', () => {
        expect(
            shouldShowEmbeddedGettingStartedHelpLink({
                edition: EditionType.EE,
                environmentId: STAGING_ENVIRONMENT,
                isAdmin: true,
                pathname: '/embedded/configurations',
            })
        ).toBe(false);
    });

    it('hides the help link for non-admins and outside EE', () => {
        expect(
            shouldShowEmbeddedGettingStartedHelpLink({
                edition: EditionType.EE,
                environmentId: DEVELOPMENT_ENVIRONMENT,
                isAdmin: false,
                pathname: '/embedded/integrations',
            })
        ).toBe(false);
        expect(
            shouldShowEmbeddedGettingStartedHelpLink({
                edition: EditionType.CE,
                environmentId: DEVELOPMENT_ENVIRONMENT,
                isAdmin: true,
                pathname: '/embedded/integrations',
            })
        ).toBe(false);
    });

    it('shows the help link on Organization settings after coming from Embedded', () => {
        expect(
            shouldShowEmbeddedGettingStartedHelpLink({
                edition: EditionType.EE,
                environmentId: DEVELOPMENT_ENVIRONMENT,
                isAdmin: true,
                pathname: '/settings/users',
                storedPlatformType: PlatformType.EMBEDDED,
            })
        ).toBe(true);
    });

    it('hides the help link on Organization settings after coming from Automation', () => {
        expect(
            shouldShowEmbeddedGettingStartedHelpLink({
                edition: EditionType.EE,
                environmentId: DEVELOPMENT_ENVIRONMENT,
                isAdmin: true,
                pathname: '/settings/users',
                storedPlatformType: PlatformType.AUTOMATION,
            })
        ).toBe(false);
    });
});
