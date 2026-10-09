import {render, screen, userEvent} from '@/shared/util/test-utils';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {AppSidebarFooter} from './AppSidebarFooter';

const hoisted = vi.hoisted(() => ({
    currentType: 0,
    edition: 'CE',
    logoutMock: vi.fn(() => Promise.resolve()),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useEnvironmentsQuery: () => ({data: {environments: []}}),
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

vi.mock('@/pages/home/stores/usePlatformTypeStore', () => ({
    PlatformType: {AUTOMATION: 0, EMBEDDED: 1},
    usePlatformTypeStore: vi.fn((selector: (state: {currentType: number; setCurrentType: () => void}) => unknown) =>
        selector({currentType: hoisted.currentType, setCurrentType: vi.fn()})
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

        hoisted.currentType = 0;
        hoisted.edition = 'CE';
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

    it('offers Approval Tasks in automation mode', async () => {
        const user = userEvent.setup();

        render(
            <MemoryRouter>
                <AppSidebarFooter />
            </MemoryRouter>
        );

        await user.click(screen.getByRole('button', {name: 'User menu'}));

        expect(screen.getByText('Approval Tasks')).toBeInTheDocument();
    });

    it('hides Approval Tasks in embedded mode', async () => {
        hoisted.currentType = 1;

        const user = userEvent.setup();

        render(
            <MemoryRouter>
                <AppSidebarFooter />
            </MemoryRouter>
        );

        await user.click(screen.getByRole('button', {name: 'User menu'}));

        expect(screen.getByText('Log Out')).toBeInTheDocument();
        expect(screen.queryByText('Approval Tasks')).not.toBeInTheDocument();
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
});
