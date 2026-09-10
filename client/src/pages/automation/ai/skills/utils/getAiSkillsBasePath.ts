const AUTOMATION_SKILLS_PATH = '/automation/settings/ai/skills';

const SKILLS_PATH_SEGMENT = '/settings/ai/skills';

/**
 * Resolves the Skills mount the caller is currently on.
 *
 * Skills are tenant-wide, so the page lives in `platformSettingsRoutes` — which is mounted TWICE, under
 * `/automation/settings` and `/embedded/settings`. A hardcoded `/automation/...` link would therefore throw an
 * embedded-settings visitor across platforms mid-flow, so every in-page link is built from the mount in the
 * current pathname instead.
 *
 * Callers outside a Skills route (the workflow editor's "Manage skills" link, the AI Hub sidebar) pass no
 * pathname and get the automation mount, which is the platform they navigate from.
 */
export default function getAiSkillsBasePath(pathname?: string): string {
    if (!pathname) {
        return AUTOMATION_SKILLS_PATH;
    }

    const segmentIndex = pathname.indexOf(SKILLS_PATH_SEGMENT);

    if (segmentIndex === -1) {
        return AUTOMATION_SKILLS_PATH;
    }

    return pathname.slice(0, segmentIndex + SKILLS_PATH_SEGMENT.length);
}
