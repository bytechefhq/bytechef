import {useUserGuidingStore} from '@/shared/stores/useUserGuidingStore';
import {fireEvent, render, screen, userEvent} from '@/shared/util/test-utils';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {AppSidebarFooter} from './AppSidebarFooter';

const DOCS = {
    changelogUrl: 'https://changelog.example.com',
    communitySupportUrl: 'https://community.example.com',
    documentationUrl: 'https://docs.example.com',
    reportBugUrl: 'https://bugs.example.com',
};

const hoisted = vi.hoisted(() => ({
    currentType: 0,
    docs: {} as Record<string, string | undefined>,
    edition: 'CE',
    logoutMock: vi.fn(() => Promise.resolve()),
    userGuidingEnabled: false,
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
    useApplicationInfoStore: vi.fn(
        (
            selector: (state: {
                application: {edition: string} | null;
                docs: Record<string, string | undefined>;
                userGuiding: {containerId: string | undefined; enabled: boolean};
            }) => unknown
        ) =>
            selector({
                application: {edition: hoisted.edition},
                docs: hoisted.docs,
                userGuiding: {containerId: 'container', enabled: hoisted.userGuidingEnabled},
            })
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
        hoisted.docs = {...DOCS};
        hoisted.edition = 'CE';
        hoisted.userGuidingEnabled = false;

        useUserGuidingStore.setState({launcherHidden: false});
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

    it('opens the Help & Feedback links in a new tab', async () => {
        const openSpy = vi.spyOn(window, 'open').mockImplementation(() => null);
        const user = userEvent.setup();

        render(
            <MemoryRouter>
                <AppSidebarFooter />
            </MemoryRouter>
        );

        await user.click(screen.getByRole('button', {name: 'User menu'}));
        await user.click(screen.getByText('Help & Feedback'));

        expect(await screen.findByText('Need Help?')).toBeInTheDocument();

        fireEvent.click(screen.getByText('Documentation'));

        expect(openSpy).toHaveBeenLastCalledWith(DOCS.documentationUrl, '_blank', 'noopener,noreferrer');

        await user.click(screen.getByRole('button', {name: 'User menu'}));
        await user.click(screen.getByText('Help & Feedback'));
        fireEvent.click(await screen.findByText('Changelog'));

        expect(openSpy).toHaveBeenLastCalledWith(DOCS.changelogUrl, '_blank', 'noopener,noreferrer');

        await user.click(screen.getByRole('button', {name: 'User menu'}));
        await user.click(screen.getByText('Help & Feedback'));
        fireEvent.click(await screen.findByText('Community Support'));

        expect(openSpy).toHaveBeenLastCalledWith(DOCS.communitySupportUrl, '_blank', 'noopener,noreferrer');

        await user.click(screen.getByRole('button', {name: 'User menu'}));
        await user.click(screen.getByText('Help & Feedback'));
        fireEvent.click(await screen.findByText('Report a Bug'));

        expect(openSpy).toHaveBeenLastCalledWith(DOCS.reportBugUrl, '_blank', 'noopener,noreferrer');

        openSpy.mockRestore();
    });

    it('hides the links whose URLs are not configured', async () => {
        hoisted.docs = {documentationUrl: DOCS.documentationUrl};

        const user = userEvent.setup();

        render(
            <MemoryRouter>
                <AppSidebarFooter />
            </MemoryRouter>
        );

        await user.click(screen.getByRole('button', {name: 'User menu'}));
        await user.click(screen.getByText('Help & Feedback'));

        expect(await screen.findByText('Documentation')).toBeInTheDocument();
        expect(screen.queryByText('Changelog')).not.toBeInTheDocument();
        expect(screen.queryByText('Need Help?')).not.toBeInTheDocument();
        expect(screen.queryByText('Community Support')).not.toBeInTheDocument();
        expect(screen.queryByText('Report a Bug')).not.toBeInTheDocument();
    });

    it('does not offer the guide toggle when UserGuiding is disabled', async () => {
        const user = userEvent.setup();

        render(
            <MemoryRouter>
                <AppSidebarFooter />
            </MemoryRouter>
        );

        await user.click(screen.getByRole('button', {name: 'User menu'}));
        await user.click(screen.getByText('Help & Feedback'));

        expect(await screen.findByText('Report a Bug')).toBeInTheDocument();
        expect(screen.queryByText('Hide Guide')).not.toBeInTheDocument();
        expect(screen.queryByText('Show Guide')).not.toBeInTheDocument();
    });

    it('hides the guide launcher and offers to show it again', async () => {
        hoisted.userGuidingEnabled = true;

        const hideChecklist = vi.fn();
        const hideResourceCenter = vi.fn();
        const launchResourceCenter = vi.fn();

        window.userGuiding = {
            getAppState: () => ({api: {resourceCenters: [{id: 42}]}}),
            hideChecklist,
            hideResourceCenter,
            launchResourceCenter,
        } as unknown as Window['userGuiding'];

        const user = userEvent.setup();

        render(
            <MemoryRouter>
                <AppSidebarFooter />
            </MemoryRouter>
        );

        await user.click(screen.getByRole('button', {name: 'User menu'}));
        await user.click(screen.getByText('Help & Feedback'));

        fireEvent.click(await screen.findByText('Hide Guide'));

        expect(hideResourceCenter).toHaveBeenCalled();
        expect(hideChecklist).toHaveBeenCalled();
        expect(useUserGuidingStore.getState().launcherHidden).toBe(true);

        await user.click(screen.getByRole('button', {name: 'User menu'}));
        await user.click(screen.getByText('Help & Feedback'));

        fireEvent.click(await screen.findByText('Show Guide'));

        expect(launchResourceCenter).toHaveBeenCalledWith(42);
        expect(useUserGuidingStore.getState().launcherHidden).toBe(false);

        window.userGuiding = undefined;
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
