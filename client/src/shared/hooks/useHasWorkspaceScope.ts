import useCeEdition from '@/shared/edition/useCeEdition';
import useEditionResolved from '@/shared/edition/useEditionResolved';
import {useIsTenantAdmin} from '@/shared/hooks/useIsTenantAdmin';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {usePermissionStore} from '@/shared/stores/usePermissionStore';

// String-literal allowlist of the permission-scope names the client gates on. Server-side these names are contributed
// by the per-module `PermissionScopeProvider` SPI (there is no central enum any more), and the GraphQL schema types a
// scope as a plain String, so there is no generated TS enum to reuse. This is deliberately a curated subset, NOT a
// mirror of every server scope: add a member here only when you introduce a call site that gates on that scope.
// Passing a name absent from this union is a compile error (catches typos); passing a name the server does not — or no
// longer — grants simply evaluates to `false` at runtime (fail-closed). If a future UI needs to render the full set of
// available scopes (e.g. a custom-role editor), fetch them via a query over the server registry rather than widening
// this union to mirror the SPI.
//
// A member also has to be a scope the server actually enforces: a name that no `@PreAuthorize` (or direct
// `PermissionService` call) anywhere in main sources checks would let a component gate on protection that does not
// exist, which reads as security in review while denying nothing. Grep the server for the literal before adding one.
export type WorkspaceScopeType =
    | 'WORKFLOW_VIEW'
    | 'WORKFLOW_CREATE'
    | 'WORKFLOW_EDIT'
    | 'WORKFLOW_DELETE'
    | 'PROJECT_CREATE'
    | 'PROJECT_SETTINGS'
    | 'PROJECT_DELETE'
    | 'PROJECT_PUBLISH'
    | 'PROJECT_PUSH'
    | 'PROJECT_PULL'
    | 'DEPLOYMENT_CREATE'
    | 'DEPLOYMENT_EDIT'
    | 'DEPLOYMENT_DELETE'
    | 'API_PLATFORM_CREATE'
    | 'API_PLATFORM_EDIT'
    | 'API_PLATFORM_DELETE'
    | 'MCP_CREATE'
    | 'MCP_EDIT'
    | 'MCP_DELETE'
    | 'DATA_TABLE_CREATE'
    | 'DATA_TABLE_EDIT'
    | 'DATA_TABLE_DELETE'
    | 'KNOWLEDGE_BASE_CREATE'
    | 'KNOWLEDGE_BASE_EDIT'
    | 'KNOWLEDGE_BASE_DELETE'
    | 'CONNECTION_VIEW'
    | 'CONNECTION_EDIT'
    | 'CONNECTION_DELETE'
    | 'API_KEY_CREATE'
    | 'API_KEY_DELETE'
    | 'WORKSPACE_MANAGE'
    | 'WORKSPACE_MEMBER_MANAGE';

// Four-state return for callers that need to distinguish "the edition was never established" / "still fetching" /
// "errored" / "genuinely denied". The plain boolean hook below collapses all four into `false` — safe for hide-based
// gating but misleading if a caller wants to render a loading spinner or an error banner. Components that need the
// richer state should prefer `useWorkspaceScopeState` plus explicit loading/error rendering.
//
// `editionUnknown` is the state the other three cannot express. The scope query is gated on positively-known Enterprise
// (see `useEeEdition`), so an unresolved `/actuator/info` means no query is ever sent: nothing is loading, nothing
// errored, and the store holds no entry — which reads identically to a member the server refused. Callers that render a
// reason must not turn that into "you do not have permission", because no permission decision was ever made.
export interface WorkspaceScopeStateI {
    editionUnknown: boolean;
    error: boolean;
    granted: boolean;
    loading: boolean;
}

/**
 * Returns `true` when the authenticated user holds the given permission scope on the workspace. Tenant admins
 * (`ROLE_ADMIN` authority) always return `true`. The answer is for the environment currently selected in
 * `useEnvironmentStore`. Requires `useLoadWorkspaceScopes(workspaceId)` to have run earlier in the component tree so
 * `usePermissionStore.workspaceScopeStates[workspaceId][currentEnvironmentId]` is populated.
 *
 * On Community the answer is unconditionally `true`. CE has no authorization boundary between workspace members —
 * every member is effectively an administrator of the workspace — so there is nothing for a scope check to
 * distinguish, and `useLoadWorkspaceScopes` correspondingly never fetches (the query is EE-only). Without this
 * short-circuit the store would stay empty on CE and every gated control would vanish for everyone.
 *
 * `workspaceId` accepts `undefined` so callers that receive an unresolved-yet workspace reference (e.g., route still
 * loading) can pass through without synthesizing a placeholder id like `0` that could theoretically collide with a
 * stored workspace's cache entry. `undefined` always fail-closes to `false` (except for tenant admins), matching the
 * hook's broader "hide if unknown" posture.
 *
 * The return type is a plain boolean for ergonomic hide-gating; when a caller cannot tolerate the loading/error/
 * denied ambiguity, use `useWorkspaceScopeState` instead.
 *
 * `environmentId` answers for a fixed environment instead of the selected one, for operations the server always checks
 * in one environment — workflow definition writes, which are checked in Development.
 */
export const useHasWorkspaceScope = (
    workspaceId: number | undefined,
    scope: WorkspaceScopeType,
    environmentId?: number
): boolean => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const workspaceScopeState = usePermissionStore((state) =>
        workspaceId === undefined
            ? undefined
            : state.workspaceScopeStates[workspaceId]?.[environmentId ?? currentEnvironmentId]
    );
    const ceEdition = useCeEdition();
    const tenantAdmin = useIsTenantAdmin();

    if (ceEdition || tenantAdmin) {
        return true;
    }

    if (workspaceId === undefined) {
        return false;
    }

    return workspaceScopeState?.status === 'loaded' && workspaceScopeState.scopes.includes(scope);
};

/**
 * Richer companion to `useHasWorkspaceScope` that returns `{editionUnknown, granted, loading, error}` so callers can
 * render loading spinners, error banners, or four-state affordances instead of collapsing everything into a `false`.
 * The flags are derived from the discriminated union state in `usePermissionStore`, populated by
 * `useLoadWorkspaceScopes`. An unresolved edition reports `{editionUnknown: true, error: false, granted: false,
 * loading: false}`; `undefined` workspaceId reports all-false — nothing to load, nothing denied. Community reports
 * `{editionUnknown: false, error: false, granted: true, loading: false}` for the reason given on `useHasWorkspaceScope`.
 *
 * `granted` is `false` in every non-Community, non-admin case, so every caller still fails closed; the extra flags only
 * change what a caller is allowed to *say* about the refusal. `environmentId` behaves as on `useHasWorkspaceScope`.
 */
export const useWorkspaceScopeState = (
    workspaceId: number | undefined,
    scope: WorkspaceScopeType,
    environmentId?: number
): WorkspaceScopeStateI => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const workspaceScopeState = usePermissionStore((state) =>
        workspaceId === undefined
            ? undefined
            : state.workspaceScopeStates[workspaceId]?.[environmentId ?? currentEnvironmentId]
    );
    const ceEdition = useCeEdition();
    const editionResolved = useEditionResolved();
    const tenantAdmin = useIsTenantAdmin();

    if (ceEdition || tenantAdmin) {
        return {editionUnknown: false, error: false, granted: true, loading: false};
    }

    // Reported before the store lookup on purpose: with the edition unresolved the loader never fetched, so the store
    // entry is missing rather than denied, and a caller reading only `granted` would render a refusal nobody made.
    if (!editionResolved) {
        return {editionUnknown: true, error: false, granted: false, loading: false};
    }

    if (workspaceId === undefined) {
        return {editionUnknown: false, error: false, granted: false, loading: false};
    }

    return {
        editionUnknown: false,
        error: workspaceScopeState?.status === 'error',
        granted: workspaceScopeState?.status === 'loaded' && workspaceScopeState.scopes.includes(scope),
        loading: workspaceScopeState?.status === 'loading',
    };
};
