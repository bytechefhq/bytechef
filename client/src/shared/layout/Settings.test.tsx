import {render, screen, userEvent} from '@/shared/util/test-utils';
import {ReactNode} from 'react';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import Settings, {SettingsNavItemI, isNavItemCurrent} from './Settings';

const hoisted = vi.hoisted(() => ({
    billingEnabled: false,
    enabledFeatureFlags: [] as string[],
    isTenantAdmin: true,
}));

vi.mock('@/shared/hooks/useIsTenantAdmin', () => ({
    useIsTenantAdmin: () => hoisted.isTenantAdmin,
}));

vi.mock('@/shared/layout/Header', () => ({
    default: () => null,
}));

vi.mock('@/shared/layout/LayoutContainer', () => ({
    default: ({leftSidebarBody}: {leftSidebarBody: ReactNode}) => <div>{leftSidebarBody}</div>,
}));

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: () => hoisted.billingEnabled,
}));

vi.mock('@/shared/stores/useFeatureFlagsStore', () => ({
    useFeatureFlagsStore: () => (featureFlag: string) => hoisted.enabledFeatureFlags.includes(featureFlag),
}));

const renderSettings = (sidebarNavItems: SettingsNavItemI[], pathname = '/') =>
    render(
        <MemoryRouter initialEntries={[pathname]}>
            <Settings sidebarNavItems={sidebarNavItems} />
        </MemoryRouter>
    );

const aiNavGroup: SettingsNavItemI = {
    items: [
        {href: 'ai-providers', title: 'Providers'},
        {href: 'ai/skills', title: 'Skills'},
    ],
    title: 'AI',
};

const tenantAdminNavItems: SettingsNavItemI[] = [
    {href: '/automation/settings/workspaces', title: 'Workspaces'},
    {href: 'git-configuration', title: 'Git Configuration'},
    {href: 'workspace-api-keys', title: 'Workspace API Keys'},
    {href: 'users', title: 'Organization Users'},
    {href: 'global-custom-roles', title: 'Roles'},
    {href: 'billing', title: 'Billing'},
    {href: 'ai-providers', title: 'Providers'},
    {href: 'notifications', title: 'Notifications'},
    {href: 'identity-providers', title: 'Identity Providers'},
    {href: 'audit-events', title: 'Audit Events'},
    {href: '/embedded/settings/signing-keys', title: 'Signing Keys'},
    {href: '/embedded/settings/api-keys', title: 'Embedded API Keys'},
];

describe('Settings', () => {
    beforeEach(() => {
        hoisted.billingEnabled = false;
        hoisted.enabledFeatureFlags = [];
        hoisted.isTenantAdmin = true;
    });

    it('shows every tenant-admin-only entry to a tenant admin', () => {
        hoisted.billingEnabled = true;
        hoisted.enabledFeatureFlags = ['ff-1025', 'ff-1039', 'ff-1040'];

        renderSettings(tenantAdminNavItems);

        tenantAdminNavItems.forEach((navItem) => expect(screen.getByText(navItem.title)).toBeInTheDocument());
    });

    it('hides every tenant-admin-only entry from a user who is not a tenant admin', () => {
        hoisted.billingEnabled = true;
        hoisted.enabledFeatureFlags = ['ff-1025', 'ff-1039', 'ff-1040'];
        hoisted.isTenantAdmin = false;

        renderSettings([{href: 'workspace-users', title: 'Workspace Users'}, ...tenantAdminNavItems]);

        tenantAdminNavItems.forEach((navItem) => expect(screen.queryByText(navItem.title)).not.toBeInTheDocument());
        expect(screen.getByText('Workspace Users')).toBeInTheDocument();
    });

    it('shows the MCP Server entry to a tenant admin', () => {
        hoisted.enabledFeatureFlags = ['ff-2197'];

        renderSettings([{href: 'mcp-server', title: 'MCP Server'}]);

        expect(screen.getByText('MCP Server')).toBeInTheDocument();
    });

    it('hides the MCP Server entry from a user who is not a tenant admin', () => {
        hoisted.enabledFeatureFlags = ['ff-2197'];
        hoisted.isTenantAdmin = false;

        renderSettings([
            {href: 'workspace-users', title: 'Users'},
            {href: 'mcp-server', title: 'MCP Server'},
        ]);

        expect(screen.queryByText('MCP Server')).not.toBeInTheDocument();
        expect(screen.getByText('Users')).toBeInTheDocument();
    });

    it('shows the API Connectors entry to a tenant admin', () => {
        hoisted.enabledFeatureFlags = ['ff-207'];

        renderSettings([{href: 'api-connectors', title: 'API Connectors'}]);

        expect(screen.getByText('API Connectors')).toBeInTheDocument();
    });

    it('hides the API Connectors entry from a user who is not a tenant admin', () => {
        hoisted.enabledFeatureFlags = ['ff-207'];
        hoisted.isTenantAdmin = false;

        renderSettings([
            {href: 'workspace-users', title: 'Users'},
            {href: 'api-connectors', title: 'API Connectors'},
        ]);

        expect(screen.queryByText('API Connectors')).not.toBeInTheDocument();
        expect(screen.getByText('Users')).toBeInTheDocument();
    });

    it('hides a section heading when every item below it is hidden by a feature flag', () => {
        renderSettings([
            {href: '/automation/settings/workspaces', title: 'Workspaces'},
            {title: 'Current Workspace'},
            {href: 'git-configuration', title: 'Git Configuration'},
            {href: 'workspace-api-keys', title: 'API Keys'},
            {title: 'Organization'},
            {href: 'users', title: 'Users'},
        ]);

        expect(screen.queryByText('Current Workspace')).not.toBeInTheDocument();
        expect(screen.getByText('Organization')).toBeInTheDocument();
        expect(screen.getByText('Users')).toBeInTheDocument();
    });

    it('keeps a section heading when an item below it is visible', () => {
        hoisted.enabledFeatureFlags = ['ff-1039'];

        renderSettings([
            {title: 'Current Workspace'},
            {href: 'git-configuration', title: 'Git Configuration'},
            {title: 'Organization'},
            {href: 'users', title: 'Users'},
        ]);

        expect(screen.getByText('Current Workspace')).toBeInTheDocument();
        expect(screen.getByText('Git Configuration')).toBeInTheDocument();
    });

    it('hides a trailing section heading that has no items at all', () => {
        renderSettings([{href: 'users', title: 'Users'}, {title: 'Organization'}]);

        expect(screen.queryByText('Organization')).not.toBeInTheDocument();
        expect(screen.getByText('Users')).toBeInTheDocument();
    });

    it('opens the nav group holding the current route', () => {
        renderSettings([{title: 'Organization'}, aiNavGroup], '/automation/settings/ai/skills');

        expect(screen.getByRole('link', {name: 'Providers'})).toBeInTheDocument();
        expect(screen.getByRole('link', {name: 'Skills'})).toBeInTheDocument();
    });

    it('keeps a nav group closed while the current route sits outside it', () => {
        renderSettings([{title: 'Organization'}, aiNavGroup], '/automation/settings/users');

        expect(screen.getByRole('button', {name: 'AI'})).toBeInTheDocument();
        expect(screen.queryByRole('link', {name: 'Providers'})).not.toBeInTheDocument();
    });

    it('opens a closed nav group when its parent row is clicked', async () => {
        renderSettings([{title: 'Organization'}, aiNavGroup], '/automation/settings/users');

        await userEvent.click(screen.getByRole('button', {name: 'AI'}));

        expect(screen.getByRole('link', {name: 'Providers'})).toBeInTheDocument();
    });

    it('marks a collapsed nav group as current so closing it does not lose the you-are-here mark', async () => {
        renderSettings([{title: 'Organization'}, aiNavGroup], '/automation/settings/ai/skills');

        const groupRow = screen.getByRole('button', {name: 'AI'});

        expect(groupRow).not.toHaveClass('bg-accent');

        await userEvent.click(groupRow);

        expect(groupRow).toHaveClass('bg-accent');
    });

    it('drops a nav group whose every entry is hidden by a feature flag', () => {
        renderSettings([
            {title: 'Organization'},
            {href: 'users', title: 'Users'},
            {items: [{href: 'custom-components', title: 'Custom Components'}], title: 'AI'},
        ]);

        expect(screen.queryByText('AI')).not.toBeInTheDocument();
        expect(screen.queryByText('Custom Components')).not.toBeInTheDocument();
        expect(screen.getByText('Users')).toBeInTheDocument();
    });
});

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
