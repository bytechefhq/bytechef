import {getPlatformModePath} from '@/pages/home/constants/platformModes';
import {PlatformType} from '@/pages/home/stores/usePlatformTypeStore';
import {describe, expect, it} from 'vitest';

describe('getPlatformModePath', () => {
    it('returns development paths in the development environment', () => {
        expect(getPlatformModePath(PlatformType.AUTOMATION, 0)).toBe('/automation');
        expect(getPlatformModePath(PlatformType.EMBEDDED, 0)).toBe('/embedded/integrations');
        expect(getPlatformModePath(PlatformType.AUTOMATION, undefined)).toBe('/automation');
    });

    it('returns deployed paths outside the development environment', () => {
        expect(getPlatformModePath(PlatformType.AUTOMATION, 2)).toBe('/automation/deployments');
        expect(getPlatformModePath(PlatformType.EMBEDDED, 2)).toBe('/embedded/configurations');
    });
});
