import {PlatformType} from '@/pages/home/stores/usePlatformTypeStore';
import {describe, expect, it} from 'vitest';

import {SettingsNavItemI} from '../Settings';
import {getSettingsNavItems} from '../settingsNavItems';

const flattenHrefs = (navItems: SettingsNavItemI[]): string[] =>
    navItems.flatMap((navItem) => (navItem.items ? flattenHrefs(navItem.items) : navItem.href ? [navItem.href] : []));

const findAiGroup = (navItems: SettingsNavItemI[]) => navItems.find((navItem) => navItem.title === 'AI');

describe('getSettingsNavItems', () => {
    it('lists Memories under the Automation current workspace, not under the AI group', () => {
        const navItems = getSettingsNavItems(PlatformType.AUTOMATION);

        expect(flattenHrefs(navItems)).toContain('/automation/settings/ai/memories');
        expect(findAiGroup(navItems)?.items?.map((item) => item.title)).toEqual(['Providers', 'Skills']);
    });

    it('lists the Embedded Memories page in Embedded, and not the Automation one', () => {
        const hrefs = flattenHrefs(getSettingsNavItems(PlatformType.EMBEDDED));

        expect(hrefs).toContain('/embedded/settings/ai/memories');
        expect(hrefs).not.toContain('/automation/settings/ai/memories');
    });

    it('lists no Embedded Memories page in Automation', () => {
        const hrefs = flattenHrefs(getSettingsNavItems(PlatformType.AUTOMATION));

        expect(hrefs).not.toContain('/embedded/settings/ai/memories');
    });

    it('lists the Embedded section pages first', () => {
        const hrefs = flattenHrefs(getSettingsNavItems(PlatformType.EMBEDDED));

        expect(hrefs.slice(0, 3)).toEqual([
            '/embedded/settings/signing-keys',
            '/embedded/settings/api-keys',
            '/embedded/settings/ai/memories',
        ]);
    });

    it('links every Organization page under /settings in both modes', () => {
        for (const mode of [PlatformType.AUTOMATION, PlatformType.EMBEDDED]) {
            const navItems = getSettingsNavItems(mode);

            const organizationIndex = navItems.findIndex((navItem) => navItem.title === 'Organization');

            const organizationHrefs = flattenHrefs(navItems.slice(organizationIndex + 1)).filter(
                (href) => href !== '/automation/settings/workspaces'
            );

            expect(organizationHrefs).toContain('/settings/users');
            expect(organizationHrefs.every((href) => href.startsWith('/settings/'))).toBe(true);
        }
    });
});
