import App from '@/App';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {authenticationStore} from '@/shared/stores/useAuthenticationStore';
import {render} from '@/shared/util/test-utils';
import {ReactNode} from 'react';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    analytics: {reset: vi.fn()},
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
    AppSidebar: () => null,
}));

vi.mock('@/shared/components/copilot/stores/useCopilotPanelStore', () => ({
    default: (selector: (state: {copilotPanelOpen: boolean}) => unknown) => selector({copilotPanelOpen: false}),
}));

vi.mock('@/shared/stores/useFeatureFlagsStore', () => ({
    useFeatureFlagsStore: () => () => false,
}));

const WORKSPACE_ID = 1234;

const renderAppAt = (path: string) =>
    render(
        <MemoryRouter initialEntries={[path]}>
            <Routes>
                <Route element={<App />} path="/">
                    <Route element={<div>Deployments page</div>} path="automation/deployments" />
                </Route>
            </Routes>
        </MemoryRouter>
    );

describe('App', () => {
    beforeEach(() => {
        useWorkspaceStore.setState({currentWorkspaceId: WORKSPACE_ID});
    });

    afterEach(() => {
        authenticationStore.setState({authenticated: false});

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
});
