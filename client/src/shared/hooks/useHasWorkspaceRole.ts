import {WorkspaceRole} from '@/shared/middleware/graphql';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {usePermissionStore} from '@/shared/stores/usePermissionStore';

// Role hierarchy ordered from most to least privileged. Mirrors the server-side `ProjectRole` /
// `WorkspaceRole` enum declaration order — lower index means higher privilege. Keep in sync with
// `server/ee/libs/automation/automation-configuration/automation-configuration-api/src/main/java/
// com/bytechef/ee/automation/configuration/security/constant/WorkspaceRole.java`.
const ROLE_HIERARCHY: readonly WorkspaceRole[] = [WorkspaceRole.Admin, WorkspaceRole.Editor, WorkspaceRole.Viewer];

// Tri-state return for callers that need to distinguish "still fetching" / "errored" / "genuinely denied". See the
// docstring on `useWorkspaceRoleState` for when to prefer this over the plain boolean hook.
export interface WorkspaceRoleStateI {
    error: boolean;
    granted: boolean;
    loading: boolean;
}

const hasAtLeast = (userRole: string | undefined, minimumRole: WorkspaceRole): boolean => {
    if (!userRole) {
        return false;
    }

    const userRoleIndex = ROLE_HIERARCHY.indexOf(userRole as WorkspaceRole);
    const minimumRoleIndex = ROLE_HIERARCHY.indexOf(minimumRole);

    return userRoleIndex >= 0 && userRoleIndex <= minimumRoleIndex;
};

// Tenant-admin short-circuit requires both an authenticated session AND the ROLE_ADMIN authority. See the
// explanatory comment on the sibling `useHasProjectScope` helper — gating on `authenticated` prevents a
// flash-of-privilege during the logout/re-login transition when `account` may still carry a prior session's
// authorities before `getAccount()` reconciles.
const isTenantAdmin = (account: {authorities?: string[] | null} | undefined, authenticated: boolean): boolean =>
    authenticated && (account?.authorities?.includes('ROLE_ADMIN') ?? false);

/**
 * Returns `true` when the authenticated user's role in the given workspace is at least `minimumRole`. Tenant admins
 * (`ROLE_ADMIN` authority) always return `true`. A user who is not a workspace member returns `false`, which callers
 * should use to hide member-management UI.
 *
 * `workspaceId` accepts `undefined` so callers receiving an unresolved workspace reference (route params still
 * loading, list item with missing id) can pass through without synthesizing a placeholder id like `0` that could
 * theoretically collide with a stored workspace's cache entry. `undefined` fail-closes to `false` (except for
 * tenant admins), matching the hook's broader "hide if unknown" posture.
 *
 * The caller is expected to already have the workspace's role loaded in `usePermissionStore` — typically via
 * `useLoadWorkspacePermissions(workspaceId)` at the top of the enclosing screen. The return type is a plain boolean
 * for ergonomic hide-gating; when a caller cannot tolerate the loading/error/denied ambiguity, use
 * `useWorkspaceRoleState` instead.
 */
export const useHasWorkspaceRole = (workspaceId: number | undefined, minimumRole: WorkspaceRole): boolean => {
    const account = useAuthenticationStore((state) => state.account);
    const authenticated = useAuthenticationStore((state) => state.authenticated);
    const workspaceState = usePermissionStore((state) =>
        workspaceId === undefined ? undefined : state.workspaceStates[workspaceId]
    );

    if (isTenantAdmin(account, authenticated)) {
        return true;
    }

    if (workspaceId === undefined || workspaceState?.status !== 'loaded') {
        return false;
    }

    return hasAtLeast(workspaceState.role, minimumRole);
};

/**
 * Richer companion to `useHasWorkspaceRole` that returns `{granted, loading, error}` so callers can render loading
 * spinners, error banners, or tri-state affordances instead of collapsing everything into a `false`. The flags are
 * derived from the discriminated union state in `usePermissionStore`, populated by `useLoadWorkspacePermissions`.
 * `undefined` workspaceId reports `{error: false, granted: false, loading: false}` — nothing to load, nothing denied.
 */
export const useWorkspaceRoleState = (
    workspaceId: number | undefined,
    minimumRole: WorkspaceRole
): WorkspaceRoleStateI => {
    const account = useAuthenticationStore((state) => state.account);
    const authenticated = useAuthenticationStore((state) => state.authenticated);
    const workspaceState = usePermissionStore((state) =>
        workspaceId === undefined ? undefined : state.workspaceStates[workspaceId]
    );

    if (isTenantAdmin(account, authenticated)) {
        return {error: false, granted: true, loading: false};
    }

    if (workspaceId === undefined) {
        return {error: false, granted: false, loading: false};
    }

    return {
        error: workspaceState?.status === 'error',
        granted: workspaceState?.status === 'loaded' && hasAtLeast(workspaceState.role, minimumRole),
        loading: workspaceState?.status === 'loading',
    };
};
