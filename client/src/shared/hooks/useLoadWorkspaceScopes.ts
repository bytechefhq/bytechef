import {useMyWorkspaceScopesQuery} from '@/shared/middleware/graphql';
import {usePermissionStore} from '@/shared/stores/usePermissionStore';
import {useEffect} from 'react';
import {toast} from 'sonner';

// Loading-state signal so callers (e.g., `useHasWorkspaceScope`) can distinguish "still fetching" from "fetched but
// the user has no permissions". Without this the hook's three failure modes (loading / errored / truly no perms)
// all collapse to `false` and the UI cannot render a useful intermediate state.
export interface UseLoadWorkspaceScopesResultI {
    error: boolean;
    loading: boolean;
}

export const useLoadWorkspaceScopes = (workspaceId: number | undefined): UseLoadWorkspaceScopesResultI => {
    const setWorkspaceScopeError = usePermissionStore((state) => state.setWorkspaceScopeError);
    const setWorkspaceScopeLoading = usePermissionStore((state) => state.setWorkspaceScopeLoading);
    const setWorkspaceScopePermissions = usePermissionStore((state) => state.setWorkspaceScopePermissions);

    const {
        data: scopesData,
        isError,
        isLoading,
    } = useMyWorkspaceScopesQuery(
        {workspaceId: String(workspaceId ?? 0)},
        {enabled: workspaceId != null && workspaceId > 0}
    );

    // Distinguish three response states: (1) still loading, (2) fetched scopes, (3) errored. The discriminated
    // union state in usePermissionStore expresses each state as mutually exclusive — setWorkspaceScopeLoading drops
    // any previously cached scopes, so a user demoted mid-session won't keep stale ALLOW decisions while a re-fetch
    // is in flight. setWorkspaceScopeError does the same on fetch failure, then a toast informs the user why gating
    // tightened. Without this clearing, useHasWorkspaceScope would keep returning true for scopes the backend has
    // already revoked.
    useEffect(() => {
        if (workspaceId == null) {
            return;
        }

        if (isLoading) {
            setWorkspaceScopeLoading(workspaceId);

            return;
        }

        if (isError) {
            setWorkspaceScopeError(workspaceId);

            toast.error('Could not load workspace permissions. Some actions may appear restricted until you refresh.');
            console.error('Failed to load workspace permissions for workspaceId:', workspaceId);

            return;
        }

        if (scopesData !== undefined) {
            setWorkspaceScopePermissions(workspaceId, scopesData.myWorkspaceScopes ?? []);
        }
    }, [
        isError,
        isLoading,
        scopesData,
        setWorkspaceScopeError,
        setWorkspaceScopeLoading,
        setWorkspaceScopePermissions,
        workspaceId,
    ]);

    return {error: isError, loading: isLoading};
};
