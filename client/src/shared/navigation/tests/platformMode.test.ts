import {PlatformType} from '@/pages/home/stores/usePlatformTypeStore';
import {resolvePlatformMode, resolveShellPlatformMode} from '@/shared/navigation/platformMode';
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

describe('resolveShellPlatformMode', () => {
    it('uses the stored mode on an Organization settings page', () => {
        expect(resolveShellPlatformMode('/settings/users', PlatformType.EMBEDDED)).toBe(PlatformType.EMBEDDED);
    });

    it('takes the mode from an Automation or Embedded URL', () => {
        expect(resolveShellPlatformMode('/automation/projects', PlatformType.EMBEDDED)).toBe(PlatformType.AUTOMATION);
        expect(resolveShellPlatformMode('/embedded/connections', PlatformType.AUTOMATION)).toBe(PlatformType.EMBEDDED);
    });

    it('has no mode on a page outside Automation, Embedded and Organization settings', () => {
        expect(resolveShellPlatformMode('/platform/connectors', PlatformType.AUTOMATION)).toBeUndefined();
        expect(resolveShellPlatformMode('/settings-archive', PlatformType.AUTOMATION)).toBeUndefined();
    });
});
