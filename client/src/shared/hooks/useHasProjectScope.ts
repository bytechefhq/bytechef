import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {usePermissionStore} from '@/shared/stores/usePermissionStore';

// String literal union mirroring the server-side `PermissionScope` enum. The GraphQL schema types the scope as a
// String (see custom-role.graphqls), so there is no generated TS enum to reuse. Keep this in sync with
// `server/ee/libs/automation/automation-configuration/automation-configuration-api/src/main/java/
// com/bytechef/ee/automation/configuration/security/constant/PermissionScope.java` — adding a scope without updating
// this union surfaces as a compile error at call sites.
export type ProjectScopeType =
    | 'WORKFLOW_VIEW'
    | 'WORKFLOW_CREATE'
    | 'WORKFLOW_EDIT'
    | 'WORKFLOW_DELETE'
    | 'WORKFLOW_TOGGLE'
    | 'EXECUTION_VIEW'
    | 'EXECUTION_DATA'
    | 'EXECUTION_RETRY'
    | 'CONNECTION_VIEW'
    | 'CONNECTION_CREATE'
    | 'CONNECTION_EDIT'
    | 'CONNECTION_DELETE'
    | 'CONNECTION_USE'
    | 'AGENT_VIEW'
    | 'AGENT_CREATE'
    | 'AGENT_EDIT'
    | 'AGENT_EXECUTE'
    | 'PROJECT_VIEW_USERS'
    | 'PROJECT_MANAGE_USERS'
    | 'PROJECT_SETTINGS'
    | 'DEPLOYMENT_PUSH'
    | 'DEPLOYMENT_PULL'
    | 'PROJECT_DELETE';

// Tri-state return for callers that need to distinguish "still fetching" / "errored" / "genuinely denied". The plain
// boolean hook below collapses all three into `false` — safe for hide-based gating but misleading if a caller wants
// to render a loading spinner or an error banner. Components that need the richer state should prefer
// `useProjectScopeState` plus explicit loading/error rendering.
export interface ProjectScopeStateI {
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
 * Returns `true` when the authenticated user holds the given permission scope on the project. Tenant admins
 * (`ROLE_ADMIN` authority) always return `true`. Requires `useLoadProjectPermissions(projectId)` to have run earlier
 * in the component tree so `usePermissionStore.projectStates[projectId]` is populated.
 *
 * `projectId` accepts `undefined` so callers that receive an unresolved-yet project reference (e.g., route still
 * loading) can pass through without synthesizing a placeholder id like `0` that could theoretically collide with a
 * stored project's cache entry. `undefined` always fail-closes to `false` (except for tenant admins), matching the
 * hook's broader "hide if unknown" posture.
 *
 * The return type is a plain boolean for ergonomic hide-gating; when a caller cannot tolerate the loading/error/
 * denied ambiguity, use `useProjectScopeState` instead.
 */
export const useHasProjectScope = (projectId: number | undefined, scope: ProjectScopeType): boolean => {
    const account = useAuthenticationStore((state) => state.account);
    const authenticated = useAuthenticationStore((state) => state.authenticated);
    const projectState = usePermissionStore((state) =>
        projectId === undefined ? undefined : state.projectStates[projectId]
    );

    if (isTenantAdmin(account, authenticated)) {
        return true;
    }

    if (projectId === undefined) {
        return false;
    }

    return projectState?.status === 'loaded' && projectState.scopes.includes(scope);
};

/**
 * Richer companion to `useHasProjectScope` that returns `{granted, loading, error}` so callers can render loading
 * spinners, error banners, or tri-state affordances instead of collapsing everything into a `false`. The flags are
 * derived from the discriminated union state in `usePermissionStore`, populated by `useLoadProjectPermissions`.
 * `undefined` projectId reports `{error: false, granted: false, loading: false}` — nothing to load, nothing denied.
 */
export const useProjectScopeState = (projectId: number | undefined, scope: ProjectScopeType): ProjectScopeStateI => {
    const account = useAuthenticationStore((state) => state.account);
    const authenticated = useAuthenticationStore((state) => state.authenticated);
    const projectState = usePermissionStore((state) =>
        projectId === undefined ? undefined : state.projectStates[projectId]
    );

    if (isTenantAdmin(account, authenticated)) {
        return {error: false, granted: true, loading: false};
    }

    if (projectId === undefined) {
        return {error: false, granted: false, loading: false};
    }

    return {
        error: projectState?.status === 'error',
        granted: projectState?.status === 'loaded' && projectState.scopes.includes(scope),
        loading: projectState?.status === 'loading',
    };
};
