import {render, screen, userEvent} from '@/shared/util/test-utils';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {AppSidebarFooter} from './AppSidebarFooter';

const hoisted = vi.hoisted(() => ({
    edition: 'CE',
    logoutMock: vi.fn(() => Promise.resolve()),
    workspaces: [] as {id: number; name: string}[],
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useEnvironmentsQuery: () => ({data: {environments: []}}),
}));

vi.mock('@/shared/queries/automation/workspaces.queries', () => ({
    useGetUserWorkspacesQuery: () => ({data: hoisted.workspaces}),
}));

vi.mock('@/shared/hooks/useAnalytics', () => ({
    useAnalytics: () => ({reset: vi.fn()}),
}));

vi.mock('@/shared/stores/useFeatureFlagsStore', () => ({
    useFeatureFlagsStore: () => () => false,
}));

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: vi.fn((selector: (state: {application: {edition: string} | null}) => unknown) =>
        selector({application: {edition: hoisted.edition}})
    ),
}));

vi.mock('@/shared/stores/useAuthenticationStore', () => ({
    useAuthenticationStore: vi.fn(
        (selector: (state: {account: {email: string; id: number} | undefined; logout: () => void}) => unknown) =>
            selector({account: {email: 'user@localhost.com', id: 1}, logout: hoisted.logoutMock})
    ),
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: vi.fn(
        (selector: (state: {currentWorkspaceId: number | undefined; setCurrentWorkspaceId: () => void}) => unknown) =>
            selector({currentWorkspaceId: 1, setCurrentWorkspaceId: vi.fn()})
    ),
}));

vi.mock('@/pages/home/stores/usePlatformTypeStore', () => ({
    PlatformType: {AUTOMATION: 0, EMBEDDED: 1},
    usePlatformTypeStore: vi.fn((selector: (state: {currentType: number; setCurrentType: () => void}) => unknown) =>
        selector({currentType: 0, setCurrentType: vi.fn()})
    ),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: vi.fn(
        (
            selector: (state: {
                currentEnvironmentId: number | undefined;
                setCurrentEnvironmentId: () => void;
            }) => unknown
        ) => selector({currentEnvironmentId: undefined, setCurrentEnvironmentId: vi.fn()})
    ),
}));

describe('AppSidebarFooter', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        hoisted.edition = 'CE';
        hoisted.workspaces = [];
    });

    it('renders the user menu trigger with the signed-in email', () => {
        render(
            <MemoryRouter>
                <AppSidebarFooter />
            </MemoryRouter>
        );

        expect(screen.getByRole('button', {name: 'User menu'})).toBeInTheDocument();
        expect(screen.getAllByText('user@localhost.com').length).toBeGreaterThan(0);
    });

    it('shows the account menu with Log Out when opened', async () => {
        const user = userEvent.setup();

        render(
            <MemoryRouter>
                <AppSidebarFooter />
            </MemoryRouter>
        );

        await user.click(screen.getByRole('button', {name: 'User menu'}));

        expect(screen.getByText('Log Out')).toBeInTheDocument();
        // Email appears in both the trigger and the open menu.
        expect(screen.getAllByText('user@localhost.com').length).toBeGreaterThanOrEqual(2);
    });

    it('drops the query cache on log out without refetching anything', async () => {
        const user = userEvent.setup();
        const queryClient = new QueryClient();

        const cancelQueriesSpy = vi.spyOn(queryClient, 'cancelQueries');
        const clearSpy = vi.spyOn(queryClient, 'clear');
        const resetQueriesSpy = vi.spyOn(queryClient, 'resetQueries');

        render(
            <QueryClientProvider client={queryClient}>
                <MemoryRouter>
                    <AppSidebarFooter />
                </MemoryRouter>
            </QueryClientProvider>
        );

        await user.click(screen.getByRole('button', {name: 'User menu'}));
        await user.click(screen.getByText('Log Out'));

        expect(resetQueriesSpy).not.toHaveBeenCalled();
        expect(cancelQueriesSpy).toHaveBeenCalled();
        expect(hoisted.logoutMock).toHaveBeenCalled();
        expect(clearSpy).toHaveBeenCalled();
    });

    it('links Manage Workspaces in the workspace menu to the workspaces settings page', async () => {
        hoisted.edition = 'EE';
        hoisted.workspaces = [{id: 1, name: 'Default'}];

        const user = userEvent.setup();

        render(
            <MemoryRouter initialEntries={['/automation/projects']}>
                <Routes>
                    <Route element={<AppSidebarFooter />} path="/automation/projects" />

                    <Route element={<div>Workspaces settings page</div>} path="/automation/settings/workspaces" />
                </Routes>
            </MemoryRouter>
        );

        await user.click(screen.getByRole('button', {name: 'User menu'}));
        await user.click(screen.getByText('Workspace: Default'));

        const manageWorkspacesItem = await screen.findByRole('menuitem', {name: 'Manage Workspaces'});

        // jsdom has no layout, so Radix closes the submenu on pointer travel; select the item via keyboard instead.
        manageWorkspacesItem.focus();

        await user.keyboard('{Enter}');

        expect(await screen.findByText('Workspaces settings page')).toBeInTheDocument();
    });
});
