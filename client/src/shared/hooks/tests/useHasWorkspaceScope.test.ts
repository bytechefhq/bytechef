import {DEVELOPMENT_ENVIRONMENT, PRODUCTION_ENVIRONMENT} from '@/shared/constants';
import {useHasWorkspaceScope, useWorkspaceScopeState} from '@/shared/hooks/useHasWorkspaceScope';
import {EditionType, applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {authenticationStore} from '@/shared/stores/useAuthenticationStore';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {permissionStore} from '@/shared/stores/usePermissionStore';
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it} from 'vitest';

const WORKSPACE_ID = 42;

function setAccount(authorities: string[]): void {
    // Mirror the steady-state shape produced by getAccount(): account populated AND authenticated=true. The hook's
    // tenant-admin short-circuit now requires both, to defend against stale-authority leaks across user sessions.
    authenticationStore.setState({
        account: {authorities, login: 'tester'} as never,
        authenticated: true,
    });
}

describe('useHasWorkspaceScope', () => {
    beforeEach(() => {
        permissionStore.setState({workspaceScopeStates: {}});
        environmentStore.setState({currentEnvironmentId: DEVELOPMENT_ENVIRONMENT});
        authenticationStore.setState({account: undefined, authenticated: false});
        applicationInfoStore.setState({application: {edition: EditionType.EE}});
    });

    it('returns true for tenant admin regardless of stored scopes', () => {
        // Tenant admin (ROLE_ADMIN) bypasses the cache lookup entirely. This is the short-circuit that protects
        // bootstrap operations from being locked out before permissions load.
        setAccount(['ROLE_ADMIN']);

        const {result} = renderHook(() => useHasWorkspaceScope(WORKSPACE_ID, 'WORKFLOW_DELETE'));

        expect(result.current).toBe(true);
    });

    it('returns true when the user has the requested scope', () => {
        setAccount(['ROLE_USER']);
        permissionStore.setState({
            workspaceScopeStates: {
                [WORKSPACE_ID]: {
                    [DEVELOPMENT_ENVIRONMENT]: {scopes: ['WORKFLOW_VIEW', 'WORKFLOW_EDIT'], status: 'loaded'},
                },
            },
        });

        const {result} = renderHook(() => useHasWorkspaceScope(WORKSPACE_ID, 'WORKFLOW_EDIT'));

        expect(result.current).toBe(true);
    });

    it('returns false when the user is missing the requested scope', () => {
        setAccount(['ROLE_USER']);
        permissionStore.setState({
            workspaceScopeStates: {
                [WORKSPACE_ID]: {[DEVELOPMENT_ENVIRONMENT]: {scopes: ['WORKFLOW_VIEW'], status: 'loaded'}},
            },
        });

        const {result} = renderHook(() => useHasWorkspaceScope(WORKSPACE_ID, 'WORKFLOW_DELETE'));

        expect(result.current).toBe(false);
    });

    it('returns false while permissions are loading even if scopes were previously cached', () => {
        // Discriminated-union invariant: a loading entry has no scopes. The previous parallel-maps store would
        // keep stale scopes alongside loading=true; the union prevents that. Pin this so a future "optimization"
        // that re-introduces stale scopes during a re-fetch fails the test.
        setAccount(['ROLE_USER']);
        permissionStore.setState({
            workspaceScopeStates: {[WORKSPACE_ID]: {[DEVELOPMENT_ENVIRONMENT]: {status: 'loading'}}},
        });

        const {result} = renderHook(() => useHasWorkspaceScope(WORKSPACE_ID, 'WORKFLOW_VIEW'));

        expect(result.current).toBe(false);
    });

    it('returns false when permissions are in an error state', () => {
        setAccount(['ROLE_USER']);
        permissionStore.setState({
            workspaceScopeStates: {[WORKSPACE_ID]: {[DEVELOPMENT_ENVIRONMENT]: {status: 'error'}}},
        });

        const {result} = renderHook(() => useHasWorkspaceScope(WORKSPACE_ID, 'WORKFLOW_VIEW'));

        expect(result.current).toBe(false);
    });

    it('returns false when no permissions are loaded for the workspace', () => {
        // Fail-closed when permissions have not yet been loaded — UI elements should hide rather than flicker into
        // visibility once the hook loads.
        setAccount(['ROLE_USER']);

        const {result} = renderHook(() => useHasWorkspaceScope(WORKSPACE_ID, 'WORKFLOW_VIEW'));

        expect(result.current).toBe(false);
    });

    it('returns false when no account is authenticated and no permissions are loaded', () => {
        // With no account AND no stored permissions, the hook fail-closes to false. The hook does NOT itself
        // gate on authentication when permissions are present — that's the responsibility of the upstream loader
        // (useLoadWorkspaceScopes only runs after the account loads).
        const {result} = renderHook(() => useHasWorkspaceScope(WORKSPACE_ID, 'WORKFLOW_VIEW'));

        expect(result.current).toBe(false);
    });

    it('treats account with empty authorities array as a regular user', () => {
        setAccount([]);
        permissionStore.setState({
            workspaceScopeStates: {
                [WORKSPACE_ID]: {[DEVELOPMENT_ENVIRONMENT]: {scopes: ['WORKFLOW_VIEW'], status: 'loaded'}},
            },
        });

        const {result} = renderHook(() => useHasWorkspaceScope(WORKSPACE_ID, 'WORKFLOW_VIEW'));

        expect(result.current).toBe(true);
    });

    it('does not short-circuit to admin when ROLE_ADMIN is present but authenticated is false', () => {
        // Flash-of-privilege defense: during a logout/re-login transition the `account` field may carry a prior
        // session's ROLE_ADMIN authority before `getAccount()` reconciles. The `authenticated` flag flips to false
        // first, so gating the short-circuit on both prevents stale authorities from granting admin-only UI to the
        // incoming user. Pin this so a future "simpler" refactor that drops the authenticated gate fails the test.
        authenticationStore.setState({
            account: {authorities: ['ROLE_ADMIN'], login: 'tester'} as never,
            authenticated: false,
        });

        const {result} = renderHook(() => useHasWorkspaceScope(WORKSPACE_ID, 'WORKFLOW_DELETE'));

        expect(result.current).toBe(false);
    });

    it('answers for the environment currently selected', () => {
        setAccount(['ROLE_USER']);
        environmentStore.setState({currentEnvironmentId: PRODUCTION_ENVIRONMENT});
        permissionStore.setState({
            workspaceScopeStates: {
                [WORKSPACE_ID]: {
                    [DEVELOPMENT_ENVIRONMENT]: {scopes: ['DEPLOYMENT_CREATE'], status: 'loaded'},
                    [PRODUCTION_ENVIRONMENT]: {scopes: [], status: 'loaded'},
                },
            },
        });

        const {result} = renderHook(() => useHasWorkspaceScope(WORKSPACE_ID, 'DEPLOYMENT_CREATE'));

        expect(result.current).toBe(false);
    });

    it('changes its answer when the environment switches', () => {
        setAccount(['ROLE_USER']);
        permissionStore.setState({
            workspaceScopeStates: {
                [WORKSPACE_ID]: {
                    [DEVELOPMENT_ENVIRONMENT]: {scopes: ['DEPLOYMENT_CREATE'], status: 'loaded'},
                    [PRODUCTION_ENVIRONMENT]: {scopes: ['WORKFLOW_VIEW'], status: 'loaded'},
                },
            },
        });

        const {result} = renderHook(() => useHasWorkspaceScope(WORKSPACE_ID, 'DEPLOYMENT_CREATE'));

        expect(result.current).toBe(true);

        act(() => {
            environmentStore.setState({currentEnvironmentId: PRODUCTION_ENVIRONMENT});
        });

        expect(result.current).toBe(false);
    });

    it('fails closed in an environment whose scopes have not been loaded', () => {
        setAccount(['ROLE_USER']);
        environmentStore.setState({currentEnvironmentId: PRODUCTION_ENVIRONMENT});
        permissionStore.setState({
            workspaceScopeStates: {
                [WORKSPACE_ID]: {[DEVELOPMENT_ENVIRONMENT]: {scopes: ['DEPLOYMENT_CREATE'], status: 'loaded'}},
            },
        });

        const {result} = renderHook(() => useHasWorkspaceScope(WORKSPACE_ID, 'DEPLOYMENT_CREATE'));

        expect(result.current).toBe(false);
    });

    it('returns true on Community for a plain member with nothing loaded', () => {
        // Community has no authorization boundary between workspace members, so useLoadWorkspaceScopes never fetches
        // the EE-only scopes field and the store stays empty. Without the CE short-circuit every gated control would
        // vanish for every CE user, including administrators. Note there is no ROLE_ADMIN authority here on purpose:
        // the short-circuit must not lean on CE happening to grant ROLE_ADMIN.
        applicationInfoStore.setState({application: {edition: EditionType.CE}});
        authenticationStore.setState({
            account: {authorities: ['ROLE_USER'], login: 'tester'} as never,
            authenticated: true,
        });

        const {result} = renderHook(() => useHasWorkspaceScope(WORKSPACE_ID, 'PROJECT_DELETE'));

        expect(result.current).toBe(true);
    });

    it('does not grant on an unresolved edition', () => {
        // `application` is null until /actuator/info resolves. Fail closed there: an Enterprise deployment must not
        // flash privileged controls before the edition is known.
        applicationInfoStore.setState({application: null});
        setAccount(['ROLE_USER']);

        const {result} = renderHook(() => useHasWorkspaceScope(WORKSPACE_ID, 'PROJECT_DELETE'));

        expect(result.current).toBe(false);
    });
});

describe('useWorkspaceScopeState', () => {
    beforeEach(() => {
        permissionStore.setState({workspaceScopeStates: {}});
        environmentStore.setState({currentEnvironmentId: DEVELOPMENT_ENVIRONMENT});
        authenticationStore.setState({account: undefined, authenticated: false});
        applicationInfoStore.setState({application: {edition: EditionType.EE}});
    });

    it('reports an unresolved edition as its own state rather than as a denial', () => {
        // /actuator/info never retries after a non-200 (getApplicationInfo leaves `loading` true and early-returns while
        // loading), so the edition can stay unresolved for a whole session. The EE-only scope query is gated on
        // positively-known Enterprise, so nothing is fetched, nothing errors and the store stays empty -- which used to
        // arrive at callers as granted/loading/error all false, indistinguishable from a refusal the server never made.
        applicationInfoStore.setState({application: null});
        setAccount(['ROLE_USER']);

        const {result} = renderHook(() => useWorkspaceScopeState(WORKSPACE_ID, 'PROJECT_DELETE'));

        expect(result.current.editionUnknown).toBe(true);
        expect(result.current.error).toBe(false);
        expect(result.current.loading).toBe(false);
    });

    it('still fails closed while the edition is unresolved', () => {
        applicationInfoStore.setState({application: null});
        setAccount(['ROLE_USER']);

        const {result} = renderHook(() => useWorkspaceScopeState(WORKSPACE_ID, 'PROJECT_DELETE'));

        expect(result.current.granted).toBe(false);
    });

    it('does not report an unresolved edition for a tenant admin, who is granted regardless', () => {
        applicationInfoStore.setState({application: null});
        setAccount(['ROLE_ADMIN']);

        const {result} = renderHook(() => useWorkspaceScopeState(WORKSPACE_ID, 'PROJECT_DELETE'));

        expect(result.current).toEqual({editionUnknown: false, error: false, granted: true, loading: false});
    });

    it('reports a plain denial once the edition is known and the scopes loaded without it', () => {
        setAccount(['ROLE_USER']);
        permissionStore.setState({
            workspaceScopeStates: {
                [WORKSPACE_ID]: {[DEVELOPMENT_ENVIRONMENT]: {scopes: ['WORKFLOW_VIEW'], status: 'loaded'}},
            },
        });

        const {result} = renderHook(() => useWorkspaceScopeState(WORKSPACE_ID, 'PROJECT_DELETE'));

        expect(result.current).toEqual({editionUnknown: false, error: false, granted: false, loading: false});
    });

    it('reports loading and error separately once the edition is known', () => {
        setAccount(['ROLE_USER']);
        permissionStore.setState({
            workspaceScopeStates: {[WORKSPACE_ID]: {[DEVELOPMENT_ENVIRONMENT]: {status: 'loading'}}},
        });

        const {result: loadingResult} = renderHook(() => useWorkspaceScopeState(WORKSPACE_ID, 'PROJECT_DELETE'));

        expect(loadingResult.current).toEqual({editionUnknown: false, error: false, granted: false, loading: true});

        permissionStore.setState({
            workspaceScopeStates: {[WORKSPACE_ID]: {[DEVELOPMENT_ENVIRONMENT]: {status: 'error'}}},
        });

        const {result: errorResult} = renderHook(() => useWorkspaceScopeState(WORKSPACE_ID, 'PROJECT_DELETE'));

        expect(errorResult.current).toEqual({editionUnknown: false, error: true, granted: false, loading: false});
    });

    it('reports the load state of the environment currently selected', () => {
        setAccount(['ROLE_USER']);
        permissionStore.setState({
            workspaceScopeStates: {
                [WORKSPACE_ID]: {
                    [DEVELOPMENT_ENVIRONMENT]: {scopes: ['DEPLOYMENT_CREATE'], status: 'loaded'},
                    [PRODUCTION_ENVIRONMENT]: {status: 'loading'},
                },
            },
        });

        const {result} = renderHook(() => useWorkspaceScopeState(WORKSPACE_ID, 'DEPLOYMENT_CREATE'));

        expect(result.current).toEqual({editionUnknown: false, error: false, granted: true, loading: false});

        act(() => {
            environmentStore.setState({currentEnvironmentId: PRODUCTION_ENVIRONMENT});
        });

        expect(result.current).toEqual({editionUnknown: false, error: false, granted: false, loading: true});
    });
});
