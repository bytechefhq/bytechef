import {useMyWorkspaceRoleQuery} from '@/shared/middleware/graphql';
import {usePermissionStore} from '@/shared/stores/usePermissionStore';
import {useEffect} from 'react';
import {toast} from 'sonner';

// Loading-state signal so callers (e.g., `useHasWorkspaceRole`) can distinguish "still fetching" from "fetched but
// the user is not a workspace member". Without this the hook's three failure modes (loading / errored / truly
// non-member) all collapse to `false` and the UI cannot render a useful intermediate state.
export interface UseLoadWorkspacePermissionsResultI {
    error: boolean;
    loading: boolean;
}

export const useLoadWorkspacePermissions = (workspaceId: number | undefined): UseLoadWorkspacePermissionsResultI => {
    const clearWorkspaceRole = usePermissionStore((state) => state.clearWorkspaceRole);
    const setWorkspaceError = usePermissionStore((state) => state.setWorkspaceError);
    const setWorkspaceLoading = usePermissionStore((state) => state.setWorkspaceLoading);
    const setWorkspaceRole = usePermissionStore((state) => state.setWorkspaceRole);

    const {
        data: roleData,
        isError,
        isLoading,
    } = useMyWorkspaceRoleQuery(
        {workspaceId: String(workspaceId ?? 0)},
        {enabled: workspaceId != null && workspaceId > 0}
    );

    // Distinguish four response states: (1) still loading, (2) fetched a non-null role, (3) fetched a
    // defined-null role (user was demoted or removed from the workspace), (4) errored. The discriminated union
    // state in usePermissionStore expresses each as mutually exclusive — setWorkspaceLoading drops any previously
    // cached role, so a user demoted mid-session won't keep stale ALLOW decisions while a re-fetch is in flight.
    // setWorkspaceError does the same on fetch failure, then a toast informs the user. clearWorkspaceRole is
    // distinct from setWorkspaceError because "no membership" is the server's authoritative answer (not an error).
    useEffect(() => {
        if (workspaceId == null) {
            return;
        }

        if (isLoading) {
            setWorkspaceLoading(workspaceId);

            return;
        }

        if (isError) {
            setWorkspaceError(workspaceId);

            toast.error('Could not load workspace permissions. Some actions may appear restricted until you refresh.');
            console.error('Failed to load workspace permissions for workspaceId:', workspaceId);

            return;
        }

        if (roleData !== undefined) {
            if (roleData.myWorkspaceRole) {
                setWorkspaceRole(workspaceId, roleData.myWorkspaceRole);
            } else {
                clearWorkspaceRole(workspaceId);
            }
        }
    }, [
        clearWorkspaceRole,
        isError,
        isLoading,
        roleData,
        setWorkspaceError,
        setWorkspaceLoading,
        setWorkspaceRole,
        workspaceId,
    ]);

    return {error: isError, loading: isLoading};
};
