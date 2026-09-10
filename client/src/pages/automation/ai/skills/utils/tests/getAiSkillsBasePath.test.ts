import getAiSkillsBasePath from '@/pages/automation/ai/skills/utils/getAiSkillsBasePath';
import {describe, expect, it} from 'vitest';

describe('getAiSkillsBasePath', () => {
    it('keeps a visitor on the automation mount', () => {
        expect(getAiSkillsBasePath('/automation/settings/ai/skills')).toBe('/automation/settings/ai/skills');
    });

    // The whole point of the helper: Skills is tenant-wide, so platformSettingsRoutes mounts it under
    // /embedded/settings as well. A hardcoded /automation/... link would throw an embedded visitor across
    // platforms on any in-page navigation.
    it('keeps a visitor on the embedded mount', () => {
        expect(getAiSkillsBasePath('/embedded/settings/ai/skills')).toBe('/embedded/settings/ai/skills');
    });

    it('strips a skill id back to the list path', () => {
        expect(getAiSkillsBasePath('/embedded/settings/ai/skills/42')).toBe('/embedded/settings/ai/skills');
    });

    it('falls back to the automation mount for callers outside a skills route', () => {
        expect(getAiSkillsBasePath('/automation/projects/7')).toBe('/automation/settings/ai/skills');
        expect(getAiSkillsBasePath()).toBe('/automation/settings/ai/skills');
    });
});
