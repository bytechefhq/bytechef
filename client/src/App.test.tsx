import App from '@/App';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {authenticationStore} from '@/shared/stores/useAuthenticationStore';
import {render} from '@/shared/util/test-utils';
import {ReactNode} from 'react';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    analytics: {reset: vi.fn()},
    enabledFeatureFlags: [] as string[],
    helpHub: {addRouter: vi.fn(), boot: vi.fn(), shutdown: vi.fn()},
    mockUseLoadWorkspaceScopes: vi.fn(),
    userGuiding: {identify: vi.fn(), shutdown: vi.fn()},
}));

vi.mock('@/shared/hooks/useLoadWorkspaceScopes', () => ({
    useLoadWorkspaceScopes: hoisted.mockUseLoadWorkspaceScopes,
}));

vi.mock('@/components/ui/sidebar', () => ({
    SidebarInset: ({children}: {children: ReactNode}) => <div>{children}</div>,
    SidebarProvider: ({children}: {children: ReactNode}) => <div>{children}</div>,
}));

vi.mock('@/components/ui/sonner', () => ({
    Toaster: () => null,
}));

vi.mock('@/config/useFetchInterceptor', () => ({
    default: vi.fn(),
}));

vi.mock('@/hooks/useUserGuiding', () => ({
    useUserGuiding: () => hoisted.userGuiding,
}));

vi.mock('@/shared/hooks/useAnalytics', () => ({
    useAnalytics: () => hoisted.analytics,
}));

vi.mock('@/shared/hooks/useHelpHub', () => ({
    useHelpHub: () => hoisted.helpHub,
}));

vi.mock('@/shared/layout/MobileTopNavigation', () => ({
    MobileTopNavigation: () => null,
}));

vi.mock('@/shared/layout/TrialBanner', () => ({
    TrialBanner: () => null,
}));

vi.mock('@/shared/layout/app-sidebar/AppSidebar', () => ({
    AppSidebar: ({navigation}: {navigation: {href: string; name: string}[]}) => (
        <nav>
            {navigation.map((navItem) => (
                <span key={navItem.href}>{navItem.name}</span>
            ))}
        </nav>
    ),
}));

vi.mock('@/shared/components/copilot/stores/useCopilotPanelStore', () => ({
    default: (selector: (state: {copilotPanelOpen: boolean}) => unknown) => selector({copilotPanelOpen: false}),
}));

vi.mock('@/shared/stores/useFeatureFlagsStore', () => ({
    useFeatureFlagsStore: () => (featureFlag: string) => hoisted.enabledFeatureFlags.includes(featureFlag),
}));

const WORKSPACE_ID = 1234;

const renderAppAt = (path: string) =>
    render(
        <MemoryRouter initialEntries={[path]}>
            <Routes>
                <Route element={<App />} path="/">
                    <Route element={<div>Deployments page</div>} path="automation/deployments" />

                    <Route element={<div>Connected users page</div>} path="embedded/connected-users" />
                </Route>
            </Routes>
        </MemoryRouter>
    );

describe('App', () => {
    beforeEach(() => {
        hoisted.enabledFeatureFlags = [];

        useWorkspaceStore.setState({currentWorkspaceId: WORKSPACE_ID});
    });

    afterEach(() => {
        authenticationStore.setState({account: undefined, authenticated: false});

        vi.clearAllMocks();
    });

    it('loads the current workspace scopes on a non-project automation page', () => {
        authenticationStore.setState({authenticated: true});

        const {getByText} = renderAppAt('/automation/deployments');

        expect(getByText('Deployments page')).toBeInTheDocument();
        expect(hoisted.mockUseLoadWorkspaceScopes).toHaveBeenCalledWith(WORKSPACE_ID);
    });

    it('does not load workspace scopes before the user is authenticated', () => {
        authenticationStore.setState({authenticated: false});

        renderAppAt('/automation/deployments');

        expect(hoisted.mockUseLoadWorkspaceScopes).not.toHaveBeenCalledWith(WORKSPACE_ID);
        expect(hoisted.mockUseLoadWorkspaceScopes).toHaveBeenCalledWith(undefined);
    });

    it('shows the embedded MCP Servers entry to a tenant admin', () => {
        hoisted.enabledFeatureFlags = ['ff-2446'];

        authenticationStore.setState({account: {authorities: ['ROLE_ADMIN'], id: 1} as never, authenticated: true});

        const {getByText} = renderAppAt('/embedded/connected-users');

        expect(getByText('MCP Servers')).toBeInTheDocument();
    });

    it('hides the embedded MCP Servers entry from a user who is not a tenant admin', () => {
        hoisted.enabledFeatureFlags = ['ff-2446'];

        authenticationStore.setState({account: {authorities: ['ROLE_USER'], id: 1} as never, authenticated: true});

        const {getByText, queryByText} = renderAppAt('/embedded/connected-users');

        expect(getByText('Connected Users')).toBeInTheDocument();
        expect(queryByText('MCP Servers')).not.toBeInTheDocument();
    });
});
