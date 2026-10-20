import {PlatformType} from '@/pages/home/stores/usePlatformTypeStore';
import {
    AI_PROVIDERS_SETTINGS_PATH,
    AI_SKILLS_SETTINGS_PATH,
    API_CONNECTORS_SETTINGS_PATH,
    BILLING_SETTINGS_PATH,
    EMBEDDED_MEMORIES_SETTINGS_PATH,
    MEMORIES_SETTINGS_PATH,
    ORGANIZATION_SETTINGS_PATH,
} from '@/shared/navigation/settingsPaths';

import {SettingsNavItemI} from './Settings';

const AUTOMATION_SETTINGS_PATH = '/automation/settings';

const EMBEDDED_SETTINGS_PATH = '/embedded/settings';

const automationSectionNavItems: SettingsNavItemI[] = [
    {
        title: 'Current Workspace',
    },
    {
        href: `${AUTOMATION_SETTINGS_PATH}/workspace-users`,
        title: 'Users',
    },
    {
        href: `${AUTOMATION_SETTINGS_PATH}/git-configuration`,
        title: 'Git Configuration',
    },
    {
        href: `${AUTOMATION_SETTINGS_PATH}/workspace-api-keys`,
        title: 'API Keys',
    },
    {
        items: [
            {
                href: MEMORIES_SETTINGS_PATH,
                title: 'Memories',
            },
        ],
        title: 'AI',
    },
];

const embeddedSectionNavItems: SettingsNavItemI[] = [
    {
        href: `${EMBEDDED_SETTINGS_PATH}/members`,
        title: 'Members',
    },
    {
        href: `${EMBEDDED_SETTINGS_PATH}/signing-keys`,
        title: 'Signing Keys',
    },
    {
        href: `${EMBEDDED_SETTINGS_PATH}/api-keys`,
        title: 'API Keys',
    },
    {
        items: [
            {
                href: EMBEDDED_MEMORIES_SETTINGS_PATH,
                title: 'Memories',
            },
        ],
        title: 'AI',
    },
];

const organizationNavItems: SettingsNavItemI[] = [
    {
        href: `${ORGANIZATION_SETTINGS_PATH}/users`,
        title: 'Users',
    },
    {
        href: `${ORGANIZATION_SETTINGS_PATH}/global-custom-roles`,
        title: 'Roles',
    },
    {
        href: BILLING_SETTINGS_PATH,
        title: 'Billing',
    },
    {
        items: [
            {
                href: AI_PROVIDERS_SETTINGS_PATH,
                title: 'Providers',
            },
            {
                href: AI_SKILLS_SETTINGS_PATH,
                title: 'Skills',
            },
        ],
        title: 'AI',
    },
    {
        href: `${ORGANIZATION_SETTINGS_PATH}/mcp-server`,
        title: 'MCP Server',
    },
    {
        href: `${ORGANIZATION_SETTINGS_PATH}/custom-components`,
        title: 'Custom Components',
    },
    {
        href: API_CONNECTORS_SETTINGS_PATH,
        title: 'API Connectors',
    },
    {
        href: `${ORGANIZATION_SETTINGS_PATH}/notifications`,
        title: 'Notifications',
    },
    {
        href: `${ORGANIZATION_SETTINGS_PATH}/identity-providers`,
        title: 'Identity Providers',
    },
    {
        href: `${ORGANIZATION_SETTINGS_PATH}/registered-clients`,
        title: 'OAuth2 Clients',
    },
    {
        href: `${ORGANIZATION_SETTINGS_PATH}/admin-api-keys`,
        title: 'Admin API Keys',
    },
    {
        href: `${ORGANIZATION_SETTINGS_PATH}/audit-events`,
        title: 'Audit Events',
    },
];

export function getSettingsNavItems(mode: PlatformType): SettingsNavItemI[] {
    if (mode === PlatformType.EMBEDDED) {
        return [...embeddedSectionNavItems, {title: 'Organization'}, ...organizationNavItems];
    }

    return [
        ...automationSectionNavItems,
        {title: 'Organization'},
        {
            href: `${AUTOMATION_SETTINGS_PATH}/workspaces`,
            title: 'Workspaces',
        },
        ...organizationNavItems,
    ];
}
