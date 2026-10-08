import {isNavItemCurrent} from '@/shared/layout/Settings';
import {describe, expect, it} from 'vitest';

describe('isNavItemCurrent', () => {
    it('matches the item whose segment the route ends with', () => {
        expect(isNavItemCurrent('/automation/settings/users', 'users')).toBe(true);
    });

    // The regression: `pathname.includes(href)` lit up both the workspace and the organization
    // entry at once, because `users` is a substring of `workspace-users`.
    it('does not match a longer segment that merely contains the item', () => {
        expect(isNavItemCurrent('/automation/settings/workspace-users', 'users')).toBe(false);
        expect(isNavItemCurrent('/automation/settings/global-custom-roles', 'custom-roles')).toBe(false);
    });

    it('still matches the longer item on its own route', () => {
        expect(isNavItemCurrent('/automation/settings/workspace-users', 'workspace-users')).toBe(true);
        expect(isNavItemCurrent('/automation/settings/global-custom-roles', 'global-custom-roles')).toBe(true);
    });

    it('treats a nested route as inside its nav item', () => {
        expect(isNavItemCurrent('/automation/settings/ai/guardrails/detail', 'ai/guardrails')).toBe(true);
    });

    it('matches an absolute href', () => {
        expect(isNavItemCurrent('/automation/settings/workspaces', '/automation/settings/workspaces')).toBe(true);
    });
});
