import {PlatformType} from '@/pages/home/stores/usePlatformTypeStore';
import {resolvePlatformMode} from '@/shared/navigation/platformMode';
import {describe, expect, it} from 'vitest';

describe('resolvePlatformMode', () => {
    it('takes Automation from the URL over the stored mode', () => {
        expect(resolvePlatformMode('/automation/projects', PlatformType.EMBEDDED)).toBe(PlatformType.AUTOMATION);
    });

    it('takes Embedded from the URL over the stored mode', () => {
        expect(resolvePlatformMode('/embedded/connections', PlatformType.AUTOMATION)).toBe(PlatformType.EMBEDDED);
    });

    it('uses the stored mode on a URL without a mode', () => {
        expect(resolvePlatformMode('/settings/users', PlatformType.EMBEDDED)).toBe(PlatformType.EMBEDDED);
    });

    it('falls back to Automation when the URL has no mode and none is stored', () => {
        expect(resolvePlatformMode('/settings/users', undefined)).toBe(PlatformType.AUTOMATION);
    });
});
