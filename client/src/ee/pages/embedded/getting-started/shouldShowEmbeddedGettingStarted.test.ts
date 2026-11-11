import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {EditionType} from '@/shared/stores/useApplicationInfoStore';
import {describe, expect, it} from 'vitest';

import shouldShowEmbeddedGettingStarted from './shouldShowEmbeddedGettingStarted';

const STAGING_ENVIRONMENT = 1;

describe('shouldShowEmbeddedGettingStarted', () => {
    it('shows Getting Started for EE admins until completed', () => {
        expect(
            shouldShowEmbeddedGettingStarted({
                completed: false,
                edition: EditionType.EE,
                environmentId: DEVELOPMENT_ENVIRONMENT,
                isAdmin: true,
            })
        ).toBe(true);
    });

    it('hides it for non-admins and once completed', () => {
        expect(
            shouldShowEmbeddedGettingStarted({
                completed: false,
                edition: EditionType.EE,
                environmentId: DEVELOPMENT_ENVIRONMENT,
                isAdmin: false,
            })
        ).toBe(false);
        expect(
            shouldShowEmbeddedGettingStarted({
                completed: true,
                edition: EditionType.EE,
                environmentId: DEVELOPMENT_ENVIRONMENT,
                isAdmin: true,
            })
        ).toBe(false);
    });

    it('hides it while the completion state is unknown', () => {
        expect(
            shouldShowEmbeddedGettingStarted({
                completed: undefined,
                edition: EditionType.EE,
                environmentId: DEVELOPMENT_ENVIRONMENT,
                isAdmin: true,
            })
        ).toBe(false);
    });

    it('hides it outside Development', () => {
        expect(
            shouldShowEmbeddedGettingStarted({
                completed: false,
                edition: EditionType.EE,
                environmentId: STAGING_ENVIRONMENT,
                isAdmin: true,
            })
        ).toBe(false);
    });

    it('hides it outside EE', () => {
        expect(
            shouldShowEmbeddedGettingStarted({
                completed: false,
                edition: EditionType.CE,
                environmentId: DEVELOPMENT_ENVIRONMENT,
                isAdmin: true,
            })
        ).toBe(false);
        expect(
            shouldShowEmbeddedGettingStarted({
                completed: false,
                edition: undefined,
                environmentId: DEVELOPMENT_ENVIRONMENT,
                isAdmin: true,
            })
        ).toBe(false);
    });
});
