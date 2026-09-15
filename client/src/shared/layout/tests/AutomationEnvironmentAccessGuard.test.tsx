import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {DEVELOPMENT_ENVIRONMENT, PRODUCTION_ENVIRONMENT} from '@/shared/constants';
import AutomationEnvironmentAccessGuard from '@/shared/layout/AutomationEnvironmentAccessGuard';
import {EditionType, applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {authenticationStore} from '@/shared/stores/useAuthenticationStore';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {permissionStore} from '@/shared/stores/usePermissionStore';
import {render, screen} from '@testing-library/react';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {beforeEach, describe, expect, it} from 'vitest';

const WORKSPACE_ID = 42;

const NOTICE_HEADING = 'No access to Production';

function renderAtPath(pathname: string) {
    render(
        <MemoryRouter initialEntries={[pathname]}>
            <Routes>
                <Route element={<AutomationEnvironmentAccessGuard />} path="/automation">
                    <Route element={<div>Deployments page</div>} path="deployments" />

                    <Route element={<div>Settings page</div>} path="settings/*" />

                    <Route element={<div>Account page</div>} path="account/*" />
                </Route>
            </Routes>
        </MemoryRouter>
    );
}

function setAccount(authorities: string[]) {
    authenticationStore.setState({account: {authorities, login: 'tester'} as never, authenticated: true});
}

function setProductionScopeState(scopeState: {scopes: string[]; status: 'loaded'} | {status: 'loading' | 'error'}) {
    permissionStore.setState({
        workspaceScopeStates: {[WORKSPACE_ID]: {[PRODUCTION_ENVIRONMENT]: scopeState}},
    } as never);
}

describe('AutomationEnvironmentAccessGuard', () => {
    beforeEach(() => {
        applicationInfoStore.setState({application: {edition: EditionType.EE}});
        environmentStore.setState({currentEnvironmentId: PRODUCTION_ENVIRONMENT});
        permissionStore.setState({workspaceScopeStates: {}});
        useWorkspaceStore.setState({currentWorkspaceId: WORKSPACE_ID});

        setAccount(['ROLE_USER']);
    });

    it('shows the notice instead of the page for a loaded empty scope set', () => {
        setProductionScopeState({scopes: [], status: 'loaded'});

        renderAtPath('/automation/deployments');

        expect(screen.getByRole('heading', {name: NOTICE_HEADING})).toBeInTheDocument();
        expect(
            screen.getByText(
                'You have no role in this environment. Switch to an environment you have access to, or ask a workspace admin for access.'
            )
        ).toBeInTheDocument();
        expect(screen.queryByText('Deployments page')).not.toBeInTheDocument();
    });

    it('uses the label of the selected environment', () => {
        environmentStore.setState({currentEnvironmentId: DEVELOPMENT_ENVIRONMENT});
        permissionStore.setState({
            workspaceScopeStates: {[WORKSPACE_ID]: {[DEVELOPMENT_ENVIRONMENT]: {scopes: [], status: 'loaded'}}},
        });

        renderAtPath('/automation/deployments');

        expect(screen.getByRole('heading', {name: 'No access to Development'})).toBeInTheDocument();
    });

    it('renders the page when the member holds scopes in the environment', () => {
        setProductionScopeState({scopes: ['WORKFLOW_VIEW'], status: 'loaded'});

        renderAtPath('/automation/deployments');

        expect(screen.getByText('Deployments page')).toBeInTheDocument();
        expect(screen.queryByRole('heading', {name: NOTICE_HEADING})).not.toBeInTheDocument();
    });

    it('renders neither the page nor the notice while scopes are loading', () => {
        setProductionScopeState({status: 'loading'});

        renderAtPath('/automation/deployments');

        expect(screen.queryByText('Deployments page')).not.toBeInTheDocument();
        expect(screen.queryByRole('heading', {name: NOTICE_HEADING})).not.toBeInTheDocument();
    });

    it('renders neither the page nor the notice before scopes start loading', () => {
        renderAtPath('/automation/deployments');

        expect(screen.queryByText('Deployments page')).not.toBeInTheDocument();
        expect(screen.queryByRole('heading', {name: NOTICE_HEADING})).not.toBeInTheDocument();
    });

    it('renders the page when loading scopes failed', () => {
        setProductionScopeState({status: 'error'});

        renderAtPath('/automation/deployments');

        expect(screen.getByText('Deployments page')).toBeInTheDocument();
    });

    it('renders the page for a tenant admin', () => {
        setAccount(['ROLE_ADMIN']);
        setProductionScopeState({scopes: [], status: 'loaded'});

        renderAtPath('/automation/deployments');

        expect(screen.getByText('Deployments page')).toBeInTheDocument();
        expect(screen.queryByRole('heading', {name: NOTICE_HEADING})).not.toBeInTheDocument();
    });

    it('renders the page on Community', () => {
        applicationInfoStore.setState({application: {edition: EditionType.CE}});
        setProductionScopeState({scopes: [], status: 'loaded'});

        renderAtPath('/automation/deployments');

        expect(screen.getByText('Deployments page')).toBeInTheDocument();
    });

    it('renders the page while the edition is unresolved', () => {
        applicationInfoStore.setState({application: null} as never);
        setProductionScopeState({scopes: [], status: 'loaded'});

        renderAtPath('/automation/deployments');

        expect(screen.getByText('Deployments page')).toBeInTheDocument();
    });

    it('does not guard settings routes', () => {
        setProductionScopeState({scopes: [], status: 'loaded'});

        renderAtPath('/automation/settings/workspace-users');

        expect(screen.getByText('Settings page')).toBeInTheDocument();
        expect(screen.queryByRole('heading', {name: NOTICE_HEADING})).not.toBeInTheDocument();
    });

    it('does not guard account routes', () => {
        setProductionScopeState({scopes: [], status: 'loaded'});

        renderAtPath('/automation/account/profile');

        expect(screen.getByText('Account page')).toBeInTheDocument();
    });
});
