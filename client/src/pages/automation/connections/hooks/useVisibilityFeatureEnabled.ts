import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {EditionType, useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';

/**
 * Discriminated union: when {@code enabled === true} the caller is guaranteed a concrete
 * {@code workspaceId}. This eliminates the redundant narrowing (`if (enabled && workspaceId !== undefined)`)
 * at every call site and makes the impossible state ({@code enabled=true, workspaceId=undefined}) unrepresentable.
 */
export type VisibilityFeatureType =
    | {enabled: true; isAdmin: boolean; workspaceId: number}
    | {enabled: false; isAdmin: boolean; workspaceId: number | undefined};

/**
 * Primitive EE-edition gate for the connection-visibility feature. Use this as the edition check
 * primitive — higher-level gates (workspace-scoped, platform-type-scoped) compose this with their
 * own context. Keeping the edition check in one place means a future switch from
 * {@link EditionType.EE} to a feature-flag or other mechanism updates every call site at once.
 */
export const useIsVisibilityEditionEnabled = (): boolean =>
    useApplicationInfoStore((state) => state.application?.edition === EditionType.EE);

/**
 * Gate for the connection-visibility UI on workspace-scoped pages (connection list, list-item
 * menus, scope badges). Returns:
 * - enabled: the visibility UI should render (EE edition + a concrete workspace context)
 * - isAdmin: current user has ROLE_ADMIN (controls promote/demote/share menu items)
 * - workspaceId: the workspace mutations should be scoped to
 *
 * Shared dialogs that render in both Automation and Embedded platforms (e.g.
 * {@code ConnectionDialog}) cannot use this hook because embedded has no workspaceId; they should
 * compose {@link useIsVisibilityEditionEnabled} with their own platform-type check.
 */
export const useVisibilityFeatureEnabled = (): VisibilityFeatureType => {
    const workspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const isEE = useIsVisibilityEditionEnabled();
    const isAdmin = useAuthenticationStore((state) => state.account?.authorities?.includes('ROLE_ADMIN') ?? false);

    if (isEE && workspaceId !== undefined) {
        return {enabled: true, isAdmin, workspaceId};
    }

    return {enabled: false, isAdmin, workspaceId};
};
