import {useHasWorkspaceRole} from '@/shared/hooks/useHasWorkspaceRole';
import {WorkspaceRole} from '@/shared/middleware/graphql';
import {authenticationStore} from '@/shared/stores/useAuthenticationStore';
import {permissionStore} from '@/shared/stores/usePermissionStore';
import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it} from 'vitest';

const WORKSPACE_ID = 7;

function setAccount(authorities: string[]): void {
    // Mirror the steady-state shape produced by getAccount(): account populated AND authenticated=true. The hook's
    // tenant-admin short-circuit now requires both, to defend against stale-authority leaks across user sessions.
    authenticationStore.setState({
        account: {authorities, login: 'tester'} as never,
        authenticated: true,
    });
}

describe('useHasWorkspaceRole', () => {
    beforeEach(() => {
        permissionStore.setState({projectStates: {}, workspaceStates: {}});
        authenticationStore.setState({account: undefined, authenticated: false});
    });

    it('returns true for tenant admin even without a stored workspace role', () => {
        // Tenant admin short-circuits before the hierarchy check — protects bootstrap operations.
        setAccount(['ROLE_ADMIN']);

        const {result} = renderHook(() => useHasWorkspaceRole(WORKSPACE_ID, WorkspaceRole.Admin));

        expect(result.current).toBe(true);
    });

    it('returns true when stored role exactly matches the minimum', () => {
        setAccount(['ROLE_USER']);
        permissionStore.setState({
            projectStates: {},
            workspaceStates: {[WORKSPACE_ID]: {role: WorkspaceRole.Editor, status: 'loaded'}},
        });

        const {result} = renderHook(() => useHasWorkspaceRole(WORKSPACE_ID, WorkspaceRole.Editor));

        expect(result.current).toBe(true);
    });

    it('returns true when stored role is more privileged than the minimum', () => {
        // ADMIN ≤ EDITOR ≤ VIEWER by ordinal — ADMIN should satisfy a request for VIEWER.
        setAccount(['ROLE_USER']);
        permissionStore.setState({
            projectStates: {},
            workspaceStates: {[WORKSPACE_ID]: {role: WorkspaceRole.Admin, status: 'loaded'}},
        });

        const {result} = renderHook(() => useHasWorkspaceRole(WORKSPACE_ID, WorkspaceRole.Viewer));

        expect(result.current).toBe(true);
    });

    it('returns false when stored role is less privileged than the minimum', () => {
        // VIEWER cannot satisfy a request for ADMIN. This is the off-by-one case that gates UI access — guard
        // against accidental >= flip.
        setAccount(['ROLE_USER']);
        permissionStore.setState({
            projectStates: {},
            workspaceStates: {[WORKSPACE_ID]: {role: WorkspaceRole.Viewer, status: 'loaded'}},
        });

        const {result} = renderHook(() => useHasWorkspaceRole(WORKSPACE_ID, WorkspaceRole.Admin));

        expect(result.current).toBe(false);
    });

    it('returns false while the workspace role is loading', () => {
        // Discriminated-union invariant: a loading entry has no role. Pin so a future change that re-introduces
        // a stale role during a re-fetch fails the test.
        setAccount(['ROLE_USER']);
        permissionStore.setState({
            projectStates: {},
            workspaceStates: {[WORKSPACE_ID]: {status: 'loading'}},
        });

        const {result} = renderHook(() => useHasWorkspaceRole(WORKSPACE_ID, WorkspaceRole.Viewer));

        expect(result.current).toBe(false);
    });

    it('returns false when the workspace role lookup is in error state', () => {
        setAccount(['ROLE_USER']);
        permissionStore.setState({
            projectStates: {},
            workspaceStates: {[WORKSPACE_ID]: {status: 'error'}},
        });

        const {result} = renderHook(() => useHasWorkspaceRole(WORKSPACE_ID, WorkspaceRole.Viewer));

        expect(result.current).toBe(false);
    });

    it('returns false when the workspace has no stored role', () => {
        // Fail-closed when permissions have not yet been loaded for the workspace.
        setAccount(['ROLE_USER']);

        const {result} = renderHook(() => useHasWorkspaceRole(WORKSPACE_ID, WorkspaceRole.Viewer));

        expect(result.current).toBe(false);
    });

    it('returns false when no account is authenticated and no role is loaded', () => {
        // With no account AND no stored role, the hook fail-closes to false. The hook does NOT itself gate on
        // authentication when a role is present — that's the responsibility of the upstream loader
        // (useLoadWorkspacePermissions only runs after the account loads).
        const {result} = renderHook(() => useHasWorkspaceRole(WORKSPACE_ID, WorkspaceRole.Viewer));

        expect(result.current).toBe(false);
    });

    it('treats account with empty authorities array as a regular user', () => {
        // Without ROLE_ADMIN the user must rely on their workspace role, not the tenant-admin shortcut.
        setAccount([]);
        permissionStore.setState({
            projectStates: {},
            workspaceStates: {[WORKSPACE_ID]: {role: WorkspaceRole.Editor, status: 'loaded'}},
        });

        const {result} = renderHook(() => useHasWorkspaceRole(WORKSPACE_ID, WorkspaceRole.Admin));

        expect(result.current).toBe(false);
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

        const {result} = renderHook(() => useHasWorkspaceRole(WORKSPACE_ID, WorkspaceRole.Admin));

        expect(result.current).toBe(false);
    });
});
