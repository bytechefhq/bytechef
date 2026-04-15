import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {usePermissionStore} from '@/shared/stores/usePermissionStore';

// String-literal allowlist of the permission-scope names the client gates on. Server-side these names are contributed
// by the per-module `PermissionScopeProvider` SPI (there is no central enum any more), and the GraphQL schema types a
// scope as a plain String, so there is no generated TS enum to reuse. This is deliberately a curated subset, NOT a
// mirror of every server scope: add a member here only when you introduce a call site that gates on that scope.
// Passing a name absent from this union is a compile error (catches typos); passing a name the server does not — or no
// longer — grants simply evaluates to `false` at runtime (fail-closed). If a future UI needs to render the full set of
// available scopes (e.g. a custom-role editor), fetch them via a query over the server registry rather than widening
// this union to mirror the SPI.
export type WorkspaceScopeType =
    | 'WORKFLOW_VIEW'
    | 'WORKFLOW_CREATE'
    | 'WORKFLOW_EDIT'
    | 'WORKFLOW_DELETE'
    | 'EXECUTION_VIEW'
    | 'CONNECTION_VIEW'
    | 'CONNECTION_EDIT'
    | 'CONNECTION_DELETE'
    | 'CONNECTION_USE'
    | 'PROJECT_SETTINGS'
    | 'DEPLOYMENT_PUSH'
    | 'DEPLOYMENT_PULL'
    | 'PROJECT_DELETE';

// Tri-state return for callers that need to distinguish "still fetching" / "errored" / "genuinely denied". The plain
// boolean hook below collapses all three into `false` — safe for hide-based gating but misleading if a caller wants
// to render a loading spinner or an error banner. Components that need the richer state should prefer
// `useWorkspaceScopeState` plus explicit loading/error rendering.
export interface WorkspaceScopeStateI {
    error: boolean;
    granted: boolean;
    loading: boolean;
}

// Tenant-admin short-circuit requires both an authenticated session AND the ROLE_ADMIN authority. Gating on
// `authenticated` (not just `account.authorities`) prevents a flash-of-privilege where the account field still carries
// a prior session's authorities during a logout/re-login transition — the store clears `authenticated` to `false` in
// `useAuthenticationStore.logout/reset/clearAuthentication` before `getAccount()` re-reconciles `account` for the new
// user. Without this gate, a component mounting during that transition would see `ROLE_ADMIN` on the stale account and
// hand the new (possibly non-admin) user admin-only UI until the refetch lands.
const isTenantAdmin = (account: {authorities?: string[] | null} | undefined, authenticated: boolean): boolean =>
    authenticated && (account?.authorities?.includes('ROLE_ADMIN') ?? false);

/**
 * Returns `true` when the authenticated user holds the given permission scope on the workspace. Tenant admins
 * (`ROLE_ADMIN` authority) always return `true`. Requires `useLoadWorkspaceScopes(workspaceId)` to have run earlier
 * in the component tree so `usePermissionStore.workspaceScopeStates[workspaceId]` is populated.
 *
 * `workspaceId` accepts `undefined` so callers that receive an unresolved-yet workspace reference (e.g., route still
 * loading) can pass through without synthesizing a placeholder id like `0` that could theoretically collide with a
 * stored workspace's cache entry. `undefined` always fail-closes to `false` (except for tenant admins), matching the
 * hook's broader "hide if unknown" posture.
 *
 * The return type is a plain boolean for ergonomic hide-gating; when a caller cannot tolerate the loading/error/
 * denied ambiguity, use `useWorkspaceScopeState` instead.
 */
export const useHasWorkspaceScope = (workspaceId: number | undefined, scope: WorkspaceScopeType): boolean => {
    const account = useAuthenticationStore((state) => state.account);
    const authenticated = useAuthenticationStore((state) => state.authenticated);
    const workspaceScopeState = usePermissionStore((state) =>
        workspaceId === undefined ? undefined : state.workspaceScopeStates[workspaceId]
    );

    if (isTenantAdmin(account, authenticated)) {
        return true;
    }

    if (workspaceId === undefined) {
        return false;
    }

    return workspaceScopeState?.status === 'loaded' && workspaceScopeState.scopes.includes(scope);
};

/**
 * Richer companion to `useHasWorkspaceScope` that returns `{granted, loading, error}` so callers can render loading
 * spinners, error banners, or tri-state affordances instead of collapsing everything into a `false`. The flags are
 * derived from the discriminated union state in `usePermissionStore`, populated by `useLoadWorkspaceScopes`.
 * `undefined` workspaceId reports `{error: false, granted: false, loading: false}` — nothing to load, nothing denied.
 */
export const useWorkspaceScopeState = (
    workspaceId: number | undefined,
    scope: WorkspaceScopeType
): WorkspaceScopeStateI => {
    const account = useAuthenticationStore((state) => state.account);
    const authenticated = useAuthenticationStore((state) => state.authenticated);
    const workspaceScopeState = usePermissionStore((state) =>
        workspaceId === undefined ? undefined : state.workspaceScopeStates[workspaceId]
    );

    if (isTenantAdmin(account, authenticated)) {
        return {error: false, granted: true, loading: false};
    }

    if (workspaceId === undefined) {
        return {error: false, granted: false, loading: false};
    }

    return {
        error: workspaceScopeState?.status === 'error',
        granted: workspaceScopeState?.status === 'loaded' && workspaceScopeState.scopes.includes(scope),
        loading: workspaceScopeState?.status === 'loading',
    };
};
