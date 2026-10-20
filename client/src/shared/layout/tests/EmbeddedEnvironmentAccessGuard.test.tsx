import {DEVELOPMENT_ENVIRONMENT, PRODUCTION_ENVIRONMENT} from '@/shared/constants';
import EmbeddedEnvironmentAccessGuard from '@/shared/layout/EmbeddedEnvironmentAccessGuard';
import {EditionType, applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {authenticationStore} from '@/shared/stores/useAuthenticationStore';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {permissionStore} from '@/shared/stores/usePermissionStore';
import {render, screen} from '@testing-library/react';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {beforeEach, describe, expect, it} from 'vitest';

const UNION_ENVIRONMENT_ID = -1;

const ENVIRONMENT_NOTICE =
    'You have no role in this environment. Switch to an environment you have access to, or ask an embedded admin for access.';
const MEMBERSHIP_NOTICE = 'You are not an embedded member. Ask an embedded admin or a tenant admin to add you.';

type ScopeStateType = {scopes: string[]; status: 'loaded'} | {status: 'error' | 'loading'};

function renderAtPath(pathname: string) {
    render(
        <MemoryRouter initialEntries={[pathname]}>
            <Routes>
                <Route element={<EmbeddedEnvironmentAccessGuard />} path="/embedded">
                    <Route element={<div>Connections page</div>} path="connections" />

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

function setScopeStates(embeddedScopeStates: Record<number, ScopeStateType>) {
    permissionStore.setState({embeddedScopeStates} as never);
}

describe('EmbeddedEnvironmentAccessGuard', () => {
    beforeEach(() => {
        applicationInfoStore.setState({application: {edition: EditionType.EE}});
        environmentStore.setState({currentEnvironmentId: PRODUCTION_ENVIRONMENT});
        permissionStore.setState({embeddedScopeStates: {}});

        setAccount(['ROLE_USER']);
    });

    it('testTheGuardShowsTheEnvironmentNoticeForAProductionOnlyMember', () => {
        environmentStore.setState({currentEnvironmentId: DEVELOPMENT_ENVIRONMENT});
        setScopeStates({
            [DEVELOPMENT_ENVIRONMENT]: {scopes: [], status: 'loaded'},
            [PRODUCTION_ENVIRONMENT]: {scopes: ['CONNECTION_VIEW'], status: 'loaded'},
            [UNION_ENVIRONMENT_ID]: {scopes: ['CONNECTION_VIEW'], status: 'loaded'},
        });

        renderAtPath('/embedded/connections');

        expect(screen.getByRole('heading', {name: 'No access to Development'})).toBeInTheDocument();
        expect(screen.getByText(ENVIRONMENT_NOTICE)).toBeInTheDocument();
        expect(screen.queryByText('Connections page')).not.toBeInTheDocument();
    });

    it('testTheGuardShowsTheMembershipNoticeForANonMember', () => {
        setScopeStates({
            [PRODUCTION_ENVIRONMENT]: {scopes: [], status: 'loaded'},
            [UNION_ENVIRONMENT_ID]: {scopes: [], status: 'loaded'},
        });

        renderAtPath('/embedded/connections');

        expect(screen.getByRole('heading', {name: 'No access to Embedded'})).toBeInTheDocument();
        expect(screen.getByText(MEMBERSHIP_NOTICE)).toBeInTheDocument();
        expect(screen.queryByText('Connections page')).not.toBeInTheDocument();
    });

    it('renders the page when the member holds scopes in the environment', () => {
        setScopeStates({
            [PRODUCTION_ENVIRONMENT]: {scopes: ['CONNECTION_VIEW'], status: 'loaded'},
            [UNION_ENVIRONMENT_ID]: {scopes: ['CONNECTION_VIEW'], status: 'loaded'},
        });

        renderAtPath('/embedded/connections');

        expect(screen.getByText('Connections page')).toBeInTheDocument();
    });

    it('renders nothing while membership is loading', () => {
        setScopeStates({[UNION_ENVIRONMENT_ID]: {status: 'loading'}});

        renderAtPath('/embedded/connections');

        expect(screen.queryByText('Connections page')).not.toBeInTheDocument();
        expect(screen.queryByText(MEMBERSHIP_NOTICE)).not.toBeInTheDocument();
        expect(screen.queryByText(ENVIRONMENT_NOTICE)).not.toBeInTheDocument();
    });

    it('renders nothing while the current environment scopes are loading', () => {
        setScopeStates({
            [PRODUCTION_ENVIRONMENT]: {status: 'loading'},
            [UNION_ENVIRONMENT_ID]: {scopes: ['CONNECTION_VIEW'], status: 'loaded'},
        });

        renderAtPath('/embedded/connections');

        expect(screen.queryByText('Connections page')).not.toBeInTheDocument();
        expect(screen.queryByText(ENVIRONMENT_NOTICE)).not.toBeInTheDocument();
    });

    it('renders the page when loading the environment scopes failed', () => {
        setScopeStates({
            [PRODUCTION_ENVIRONMENT]: {status: 'error'},
            [UNION_ENVIRONMENT_ID]: {scopes: ['CONNECTION_VIEW'], status: 'loaded'},
        });

        renderAtPath('/embedded/connections');

        expect(screen.getByText('Connections page')).toBeInTheDocument();
    });

    it('renders the page when loading the union scopes failed', () => {
        setScopeStates({[UNION_ENVIRONMENT_ID]: {status: 'error'}});

        renderAtPath('/embedded/connections');

        expect(screen.getByText('Connections page')).toBeInTheDocument();
        expect(screen.queryByText(MEMBERSHIP_NOTICE)).not.toBeInTheDocument();
    });

    it('renders the page for a tenant admin', () => {
        setAccount(['ROLE_ADMIN']);
        setScopeStates({
            [PRODUCTION_ENVIRONMENT]: {scopes: [], status: 'loaded'},
            [UNION_ENVIRONMENT_ID]: {scopes: [], status: 'loaded'},
        });

        renderAtPath('/embedded/connections');

        expect(screen.getByText('Connections page')).toBeInTheDocument();
    });

    it('renders the page on Community', () => {
        applicationInfoStore.setState({application: {edition: EditionType.CE}});

        renderAtPath('/embedded/connections');

        expect(screen.getByText('Connections page')).toBeInTheDocument();
    });

    it('does not guard the account routes', () => {
        setScopeStates({[UNION_ENVIRONMENT_ID]: {scopes: [], status: 'loaded'}});

        renderAtPath('/embedded/account/profile');

        expect(screen.getByText('Account page')).toBeInTheDocument();
    });

    it('guards the settings routes', () => {
        setScopeStates({[UNION_ENVIRONMENT_ID]: {scopes: [], status: 'loaded'}});

        renderAtPath('/embedded/settings/api-keys');

        expect(screen.getByText(MEMBERSHIP_NOTICE)).toBeInTheDocument();
        expect(screen.queryByText('Settings page')).not.toBeInTheDocument();
    });
});
