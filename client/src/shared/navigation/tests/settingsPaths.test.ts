import {getOrganizationSettingsRedirectPath} from '@/shared/navigation/settingsPaths';
import {describe, expect, it} from 'vitest';

describe('getOrganizationSettingsRedirectPath', () => {
    it('moves an Automation Organization page to /settings', () => {
        expect(getOrganizationSettingsRedirectPath('/automation/settings/users', '')).toBe('/settings/users');
    });

    it('keeps the nested path and the query of an Embedded Organization page', () => {
        expect(getOrganizationSettingsRedirectPath('/embedded/settings/ai/skills/42', '?tagId=3')).toBe(
            '/settings/ai/skills/42?tagId=3'
        );
    });

    it('keeps a nested API Connectors page', () => {
        expect(getOrganizationSettingsRedirectPath('/automation/settings/api-connectors/new/import', '')).toBe(
            '/settings/api-connectors/new/import'
        );
    });

    it('leaves Memories, which is not an Organization page, alone', () => {
        expect(getOrganizationSettingsRedirectPath('/automation/settings/ai/memories', '')).toBeUndefined();
    });

    it('leaves a workspace settings page alone', () => {
        expect(getOrganizationSettingsRedirectPath('/automation/settings/workspace-api-keys', '')).toBeUndefined();
    });

    it('matches an Organization page only as a whole path segment', () => {
        expect(getOrganizationSettingsRedirectPath('/automation/settings/users-extra', '')).toBeUndefined();
    });
});
