import {useHasProjectScope} from '@/shared/hooks/useHasProjectScope';
import {authenticationStore} from '@/shared/stores/useAuthenticationStore';
import {permissionStore} from '@/shared/stores/usePermissionStore';
import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it} from 'vitest';

const PROJECT_ID = 42;

function setAccount(authorities: string[]): void {
    // Mirror the steady-state shape produced by getAccount(): account populated AND authenticated=true. The hook's
    // tenant-admin short-circuit now requires both, to defend against stale-authority leaks across user sessions.
    authenticationStore.setState({
        account: {authorities, login: 'tester'} as never,
        authenticated: true,
    });
}

describe('useHasProjectScope', () => {
    beforeEach(() => {
        permissionStore.setState({projectStates: {}, workspaceStates: {}});
        authenticationStore.setState({account: undefined, authenticated: false});
    });

    it('returns true for tenant admin regardless of stored scopes', () => {
        // Tenant admin (ROLE_ADMIN) bypasses the cache lookup entirely. This is the short-circuit that protects
        // bootstrap operations from being locked out before permissions load.
        setAccount(['ROLE_ADMIN']);

        const {result} = renderHook(() => useHasProjectScope(PROJECT_ID, 'WORKFLOW_DELETE'));

        expect(result.current).toBe(true);
    });

    it('returns true when the user has the requested scope', () => {
        setAccount(['ROLE_USER']);
        permissionStore.setState({
            projectStates: {[PROJECT_ID]: {scopes: ['WORKFLOW_VIEW', 'WORKFLOW_EDIT'], status: 'loaded'}},
            workspaceStates: {},
        });

        const {result} = renderHook(() => useHasProjectScope(PROJECT_ID, 'WORKFLOW_EDIT'));

        expect(result.current).toBe(true);
    });

    it('returns false when the user is missing the requested scope', () => {
        setAccount(['ROLE_USER']);
        permissionStore.setState({
            projectStates: {[PROJECT_ID]: {scopes: ['WORKFLOW_VIEW'], status: 'loaded'}},
            workspaceStates: {},
        });

        const {result} = renderHook(() => useHasProjectScope(PROJECT_ID, 'WORKFLOW_DELETE'));

        expect(result.current).toBe(false);
    });

    it('returns false while permissions are loading even if scopes were previously cached', () => {
        // Discriminated-union invariant: a loading entry has no scopes. The previous parallel-maps store would
        // keep stale scopes alongside loading=true; the union prevents that. Pin this so a future "optimization"
        // that re-introduces stale scopes during a re-fetch fails the test.
        setAccount(['ROLE_USER']);
        permissionStore.setState({
            projectStates: {[PROJECT_ID]: {status: 'loading'}},
            workspaceStates: {},
        });

        const {result} = renderHook(() => useHasProjectScope(PROJECT_ID, 'WORKFLOW_VIEW'));

        expect(result.current).toBe(false);
    });

    it('returns false when permissions are in an error state', () => {
        setAccount(['ROLE_USER']);
        permissionStore.setState({
            projectStates: {[PROJECT_ID]: {status: 'error'}},
            workspaceStates: {},
        });

        const {result} = renderHook(() => useHasProjectScope(PROJECT_ID, 'WORKFLOW_VIEW'));

        expect(result.current).toBe(false);
    });

    it('returns false when no permissions are loaded for the project', () => {
        // Fail-closed when permissions have not yet been loaded — UI elements should hide rather than flicker into
        // visibility once the hook loads.
        setAccount(['ROLE_USER']);

        const {result} = renderHook(() => useHasProjectScope(PROJECT_ID, 'WORKFLOW_VIEW'));

        expect(result.current).toBe(false);
    });

    it('returns false when no account is authenticated and no permissions are loaded', () => {
        // With no account AND no stored permissions, the hook fail-closes to false. The hook does NOT itself
        // gate on authentication when permissions are present — that's the responsibility of the upstream loader
        // (useLoadProjectPermissions only runs after the account loads).
        const {result} = renderHook(() => useHasProjectScope(PROJECT_ID, 'WORKFLOW_VIEW'));

        expect(result.current).toBe(false);
    });

    it('treats account with empty authorities array as a regular user', () => {
        setAccount([]);
        permissionStore.setState({
            projectStates: {[PROJECT_ID]: {scopes: ['WORKFLOW_VIEW'], status: 'loaded'}},
            workspaceStates: {},
        });

        const {result} = renderHook(() => useHasProjectScope(PROJECT_ID, 'WORKFLOW_VIEW'));

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

        const {result} = renderHook(() => useHasProjectScope(PROJECT_ID, 'WORKFLOW_DELETE'));

        expect(result.current).toBe(false);
    });
});
