import {IntegrationStatus, IntegrationVersion} from '@/ee/shared/middleware/embedded/configuration';
import {describe, expect, it} from 'vitest';

import {isNewerIntegrationVersionAvailable} from '../useNewerIntegrationVersionAvailable';

const integrationVersions = [
    {status: IntegrationStatus.Published, version: 2},
    {status: IntegrationStatus.Published, version: 3},
    {status: IntegrationStatus.Draft, version: 4},
] as IntegrationVersion[];

describe('isNewerIntegrationVersionAvailable', () => {
    it('is true when a later version is published', () => {
        expect(isNewerIntegrationVersionAvailable(integrationVersions, 2)).toBe(true);
    });

    it('is false on the last published version even when a later draft exists', () => {
        expect(isNewerIntegrationVersionAvailable(integrationVersions, 3)).toBe(false);
    });

    it('is false without a version or without loaded versions', () => {
        expect(isNewerIntegrationVersionAvailable(integrationVersions, undefined)).toBe(false);
        expect(isNewerIntegrationVersionAvailable(undefined, 1)).toBe(false);
    });
});
