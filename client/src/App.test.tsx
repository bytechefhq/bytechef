import App from '@/App';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {DEVELOPMENT_ENVIRONMENT, PRODUCTION_ENVIRONMENT} from '@/shared/constants';
import {authenticationStore} from '@/shared/stores/useAuthenticationStore';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {render} from '@/shared/util/test-utils';
import {ReactNode} from 'react';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    analytics: {reset: vi.fn()},
    embeddedScopes: [] as string[],
    enabledFeatureFlags: [] as string[],
    helpHub: {addRouter: vi.fn(), boot: vi.fn(), shutdown: vi.fn()},
    isTenantAdmin: false,
    mockUseLoadWorkspaceScopes: vi.fn(),
    userGuiding: {identify: vi.fn(), shutdown: vi.fn()},
}));

vi.mock('@/shared/hooks/useHasEmbeddedScope', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/hooks/useHasEmbeddedScope')>()),
    useHasEmbeddedScope: (scope: string) => hoisted.isTenantAdmin || hoisted.embeddedScopes.includes(scope),
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
        hoisted.embeddedScopes = [];
        hoisted.enabledFeatureFlags = [];
        hoisted.isTenantAdmin = false;

        useWorkspaceStore.setState({currentWorkspaceId: WORKSPACE_ID});
    });

    afterEach(() => {
        authenticationStore.setState({account: undefined, authenticated: false});
        environmentStore.setState({currentEnvironmentId: DEVELOPMENT_ENVIRONMENT});

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

    it('testATenantAdminSeesEveryEmbeddedEntry', () => {
        hoisted.enabledFeatureFlags = ['ff-2446'];
        hoisted.isTenantAdmin = true;

        authenticationStore.setState({account: {authorities: ['ROLE_ADMIN'], id: 1} as never, authenticated: true});

        const {getByText} = renderAppAt('/embedded/connected-users');

        [
            'Integrations',
            'Integration Configurations',
            'App Events',
            'Automations',
            'MCP Servers',
            'Connected Users',
            'Workflow Executions',
            'Connections',
        ].forEach((entryName) => expect(getByText(entryName)).toBeInTheDocument());
    });

    it('testANonMemberSeesNoEmbeddedNavigation', () => {
        hoisted.enabledFeatureFlags = ['ff-2446'];

        authenticationStore.setState({account: {authorities: ['ROLE_USER'], id: 1} as never, authenticated: true});

        const {queryByText} = renderAppAt('/embedded/connected-users');

        [
            'Integrations',
            'Integration Configurations',
            'App Events',
            'Automations',
            'MCP Servers',
            'Connected Users',
            'Workflow Executions',
            'Connections',
        ].forEach((entryName) => expect(queryByText(entryName)).not.toBeInTheDocument());
    });

    it('testAMemberSeesOnlyPagesTheyCanView', () => {
        hoisted.embeddedScopes = ['MCP_VIEW', 'CONNECTION_VIEW', 'WORKFLOW_VIEW'];

        authenticationStore.setState({account: {authorities: ['ROLE_USER'], id: 1} as never, authenticated: true});

        const {getByText, queryByText} = renderAppAt('/embedded/connected-users');

        expect(getByText('MCP Servers')).toBeInTheDocument();
        expect(getByText('Connections')).toBeInTheDocument();
        expect(getByText('Integrations')).toBeInTheDocument();
        expect(getByText('Automations')).toBeInTheDocument();
        expect(queryByText('Connected Users')).not.toBeInTheDocument();
        expect(queryByText('Workflow Executions')).not.toBeInTheDocument();
        expect(queryByText('App Events')).not.toBeInTheDocument();
    });

    it('hides Integrations outside Development even for a member with WORKFLOW_VIEW', () => {
        hoisted.embeddedScopes = ['WORKFLOW_VIEW'];

        environmentStore.setState({currentEnvironmentId: PRODUCTION_ENVIRONMENT});
        authenticationStore.setState({account: {authorities: ['ROLE_USER'], id: 1} as never, authenticated: true});

        const {getByText, queryByText} = renderAppAt('/embedded/connected-users');

        expect(getByText('Automations')).toBeInTheDocument();
        expect(queryByText('Integrations')).not.toBeInTheDocument();
    });
});
