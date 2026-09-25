import {render, screen} from '@/shared/util/test-utils';
import {MemoryRouter, Route, Routes, useLocation} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {SettingsNavItemI} from './Settings';
import SettingsIndexRedirect from './SettingsIndexRedirect';

const hoisted = vi.hoisted(() => ({
    isTenantAdmin: false,
}));

vi.mock('@/shared/hooks/useIsTenantAdmin', () => ({
    useIsTenantAdmin: () => hoisted.isTenantAdmin,
}));

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: () => true,
}));

vi.mock('@/shared/stores/useFeatureFlagsStore', () => ({
    useFeatureFlagsStore: () => () => true,
}));

const LocationPathname = () => <span data-testid="pathname">{useLocation().pathname}</span>;

const renderSettingsIndexRedirect = (sidebarNavItems: SettingsNavItemI[]) =>
    render(
        <MemoryRouter initialEntries={['/automation/settings']}>
            <Routes>
                <Route path="/automation/settings">
                    <Route
                        element={
                            <SettingsIndexRedirect
                                fallbackHref="/automation/account"
                                sidebarNavItems={sidebarNavItems}
                                tenantAdminHref="workspaces"
                            />
                        }
                        index
                    />
                </Route>

                <Route element={<LocationPathname />} path="*" />
            </Routes>
        </MemoryRouter>
    );

const tenantAdminOnlyNavItems: SettingsNavItemI[] = [
    {title: 'Organization'},
    {href: '/automation/settings/workspaces', title: 'Workspaces'},
    {href: 'users', title: 'Users'},
    {items: [{href: 'ai-providers', title: 'Providers'}], title: 'AI'},
];

describe('SettingsIndexRedirect', () => {
    beforeEach(() => {
        hoisted.isTenantAdmin = false;
    });

    it('sends a tenant admin to the tenant admin page', () => {
        hoisted.isTenantAdmin = true;

        renderSettingsIndexRedirect([{href: 'workspace-users', title: 'Users'}, ...tenantAdminOnlyNavItems]);

        expect(screen.getByTestId('pathname')).toHaveTextContent('/automation/settings/workspaces');
    });

    it('sends a non-admin to the first entry they can see, skipping headings and looking inside groups', () => {
        renderSettingsIndexRedirect([
            ...tenantAdminOnlyNavItems,
            {
                items: [
                    {href: 'ai-providers', title: 'Providers'},
                    {href: 'ai/skills', title: 'Skills'},
                ],
                title: 'AI',
            },
            {href: 'custom-components', title: 'Custom Components'},
        ]);

        expect(screen.getByTestId('pathname')).toHaveTextContent('/automation/settings/ai/skills');
    });

    it('sends a non-admin who can see no settings entry to the account page', () => {
        renderSettingsIndexRedirect(tenantAdminOnlyNavItems);

        expect(screen.getByTestId('pathname')).toHaveTextContent('/automation/account');
    });
});
