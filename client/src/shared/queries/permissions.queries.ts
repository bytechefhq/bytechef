import {QueryClient} from '@tanstack/react-query';

// The generated hook keys this as ['MyWorkspaceScopes', {workspaceId}]. Naming only the first element invalidates
// every workspace's entry, which is what a membership change calls for: a mutation can move the caller's own access in
// a workspace other than the one the screen is showing.
export const MY_WORKSPACE_SCOPES_QUERY_KEY = 'MyWorkspaceScopes';

/**
 * Invalidates the caller's own permission queries after a membership mutation.
 *
 * A membership mutation can change the *actor's* access, not just the target's: removing yourself from a workspace,
 * demoting yourself, or swapping your own built-in role for a custom one. `myWorkspaceScopes` is an ordinary
 * react-query entry with no relation to the `WorkspaceUsers` list, so without this every control gated on it stays
 * enabled against a stale cached answer until a full reload — the user keeps clicking affordances the server now
 * refuses.
 *
 * Refetching also re-runs the `useLoadWorkspaceScopes` effect, which is what pushes the new answer into
 * `usePermissionStore` where the gating hooks read it.
 */
export const invalidateMyPermissionQueries = (queryClient: QueryClient): void => {
    queryClient.invalidateQueries({queryKey: [MY_WORKSPACE_SCOPES_QUERY_KEY]});
};
