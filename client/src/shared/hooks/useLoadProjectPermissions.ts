import {useMyProjectScopesQuery} from '@/shared/middleware/graphql';
import {usePermissionStore} from '@/shared/stores/usePermissionStore';
import {useEffect} from 'react';
import {toast} from 'sonner';

// Loading-state signal so callers (e.g., `useHasProjectScope`) can distinguish "still fetching" from "fetched but
// the user has no permissions". Without this the hook's three failure modes (loading / errored / truly no perms)
// all collapse to `false` and the UI cannot render a useful intermediate state.
export interface UseLoadProjectPermissionsResultI {
    error: boolean;
    loading: boolean;
}

export const useLoadProjectPermissions = (projectId: number | undefined): UseLoadProjectPermissionsResultI => {
    const setProjectError = usePermissionStore((state) => state.setProjectError);
    const setProjectLoading = usePermissionStore((state) => state.setProjectLoading);
    const setProjectPermissions = usePermissionStore((state) => state.setProjectPermissions);

    const {
        data: scopesData,
        isError,
        isLoading,
    } = useMyProjectScopesQuery({projectId: String(projectId ?? 0)}, {enabled: projectId != null && projectId > 0});

    // Distinguish three response states: (1) still loading, (2) fetched scopes, (3) errored. The discriminated
    // union state in usePermissionStore expresses each state as mutually exclusive — setProjectLoading drops any
    // previously cached scopes, so a user demoted mid-session won't keep stale ALLOW decisions while a re-fetch
    // is in flight. setProjectError does the same on fetch failure, then a toast informs the user why gating
    // tightened. Without this clearing, useHasProjectScope would keep returning true for scopes the backend has
    // already revoked.
    useEffect(() => {
        if (projectId == null) {
            return;
        }

        if (isLoading) {
            setProjectLoading(projectId);

            return;
        }

        if (isError) {
            setProjectError(projectId);

            toast.error('Could not load project permissions. Some actions may appear restricted until you refresh.');
            console.error('Failed to load project permissions for projectId:', projectId);

            return;
        }

        if (scopesData !== undefined) {
            setProjectPermissions(projectId, scopesData.myProjectScopes ?? []);
        }
    }, [isError, isLoading, projectId, scopesData, setProjectError, setProjectLoading, setProjectPermissions]);

    return {error: isError, loading: isLoading};
};
