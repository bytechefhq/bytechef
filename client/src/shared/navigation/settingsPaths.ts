export const ORGANIZATION_SETTINGS_PATH = '/settings';

export const ORGANIZATION_SETTINGS_PAGES: readonly string[] = [
    'users',
    'global-custom-roles',
    'connections',
    'billing',
    'ai-providers',
    'ai/skills',
    'mcp-server',
    'custom-components',
    'api-connectors',
    'notifications',
    'identity-providers',
    'registered-clients',
    'admin-api-keys',
    'audit-events',
];

export const AI_PROVIDERS_SETTINGS_PATH = `${ORGANIZATION_SETTINGS_PATH}/ai-providers`;

export const AI_SKILLS_SETTINGS_PATH = `${ORGANIZATION_SETTINGS_PATH}/ai/skills`;

export const API_CONNECTORS_SETTINGS_PATH = `${ORGANIZATION_SETTINGS_PATH}/api-connectors`;

export const BILLING_SETTINGS_PATH = `${ORGANIZATION_SETTINGS_PATH}/billing`;

export const EMBEDDED_MEMORIES_SETTINGS_PATH = '/embedded/settings/ai/memories';

export const MEMORIES_SETTINGS_PATH = '/automation/settings/ai/memories';

const SECTION_SETTINGS_PATH_PATTERN = /^\/(automation|embedded)\/settings\/(.+)$/;

export function getOrganizationSettingsRedirectPath(pathname: string, search: string, hash = ''): string | undefined {
    const match = SECTION_SETTINGS_PATH_PATTERN.exec(pathname);

    if (!match) {
        return undefined;
    }

    const settingsPagePath = match[2];

    const organizationPage = ORGANIZATION_SETTINGS_PAGES.find(
        (page) => settingsPagePath === page || settingsPagePath.startsWith(`${page}/`)
    );

    if (!organizationPage) {
        return undefined;
    }

    return `${ORGANIZATION_SETTINGS_PATH}/${settingsPagePath}${search}${hash}`;
}
