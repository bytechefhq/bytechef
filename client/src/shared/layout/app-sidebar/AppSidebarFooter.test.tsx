import {render, screen, userEvent} from '@/shared/util/test-utils';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {AppSidebarFooter} from './AppSidebarFooter';

vi.mock('@/shared/middleware/graphql', () => ({
    useEnvironmentsQuery: () => ({data: {environments: []}}),
}));

vi.mock('@/shared/queries/automation/workspaces.queries', () => ({
    useGetUserWorkspacesQuery: () => ({data: []}),
}));

vi.mock('@/shared/hooks/useAnalytics', () => ({
    useAnalytics: () => ({reset: vi.fn()}),
}));

vi.mock('@/shared/stores/useFeatureFlagsStore', () => ({
    useFeatureFlagsStore: () => () => false,
}));

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: vi.fn((selector: (state: {application: {edition: string} | null}) => unknown) =>
        selector({application: {edition: 'CE'}})
    ),
}));

vi.mock('@/shared/stores/useAuthenticationStore', () => ({
    useAuthenticationStore: vi.fn(
        (selector: (state: {account: {email: string; id: number} | undefined; logout: () => void}) => unknown) =>
            selector({account: {email: 'user@localhost.com', id: 1}, logout: vi.fn()})
    ),
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: vi.fn(
        (selector: (state: {currentWorkspaceId: number | undefined; setCurrentWorkspaceId: () => void}) => unknown) =>
            selector({currentWorkspaceId: undefined, setCurrentWorkspaceId: vi.fn()})
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
});
