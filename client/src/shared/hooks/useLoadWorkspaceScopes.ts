import {DEVELOPMENT_ENVIRONMENT, PRODUCTION_ENVIRONMENT, STAGING_ENVIRONMENT} from '@/shared/constants';
import useEeEdition from '@/shared/edition/useEeEdition';
import {EnvironmentEnum, useMyWorkspaceScopesQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {usePermissionStore} from '@/shared/stores/usePermissionStore';
import {useEffect} from 'react';
import {toast} from 'sonner';

const ENVIRONMENT_ENUMS: Record<number, EnvironmentEnum> = {
    [DEVELOPMENT_ENVIRONMENT]: EnvironmentEnum.Development,
    [PRODUCTION_ENVIRONMENT]: EnvironmentEnum.Production,
    [STAGING_ENVIRONMENT]: EnvironmentEnum.Staging,
};

// Loading-state signal so callers (e.g., `useHasWorkspaceScope`) can distinguish "still fetching" from "fetched but
// the user has no permissions". Without this the hook's three failure modes (loading / errored / truly no perms)
// all collapse to `false` and the UI cannot render a useful intermediate state.
export interface UseLoadWorkspaceScopesResultI {
    error: boolean;
    loading: boolean;
}

const useLoadEnvironmentWorkspaceScopes = (
    workspaceId: number | undefined,
    environmentId: number,
    eeEdition: boolean
): UseLoadWorkspaceScopesResultI => {
    const setWorkspaceScopeError = usePermissionStore((state) => state.setWorkspaceScopeError);
    const setWorkspaceScopeLoading = usePermissionStore((state) => state.setWorkspaceScopeLoading);
    const setWorkspaceScopePermissions = usePermissionStore((state) => state.setWorkspaceScopePermissions);

    const environment = ENVIRONMENT_ENUMS[environmentId];

    const {
        data: scopesData,
        isError,
        isLoading,
    } = useMyWorkspaceScopesQuery(
        {environment, workspaceId: String(workspaceId ?? 0)},
        {enabled: eeEdition && environment !== undefined && workspaceId != null && workspaceId > 0}
    );

    // Distinguish three response states: (1) still loading, (2) fetched scopes, (3) errored. The discriminated
    // union state in usePermissionStore expresses each state as mutually exclusive — setWorkspaceScopeLoading drops
    // any previously cached scopes, so a user demoted mid-session won't keep stale ALLOW decisions while a re-fetch
    // is in flight. A failed first fetch stores the error and a toast informs the user why gating tightened. A failed
    // background refetch keeps the scopes already loaded: React Query keeps the last good data alongside the error,
    // and dropping it would lock an editor out over a network blip while the server still enforces every write.
    useEffect(() => {
        if (!eeEdition || environment === undefined || workspaceId == null) {
            return;
        }

        if (isLoading) {
            setWorkspaceScopeLoading(workspaceId, environmentId);

            return;
        }

        if (isError && scopesData === undefined) {
            setWorkspaceScopeError(workspaceId, environmentId);

            toast.error('Could not load workspace permissions. Some actions may appear restricted until you refresh.', {
                id: 'workspace-permissions-error',
            });
            console.error('Failed to load workspace permissions for workspaceId:', workspaceId);

            return;
        }

        if (scopesData !== undefined) {
            setWorkspaceScopePermissions(workspaceId, environmentId, scopesData.myWorkspaceScopes ?? []);
        }
    }, [
        eeEdition,
        environment,
        environmentId,
        isError,
        isLoading,
        scopesData,
        setWorkspaceScopeError,
        setWorkspaceScopeLoading,
        setWorkspaceScopePermissions,
        workspaceId,
    ]);

    return {error: isError && scopesData === undefined, loading: isLoading};
};

export const useLoadWorkspaceScopes = (workspaceId: number | undefined): UseLoadWorkspaceScopesResultI => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    // `myWorkspaceScopes` is declared only in the EE GraphQL schema, behind a @ConditionalOnEEVersion controller. On
    // Community the field does not exist, the server answers with an `errors` array, and `graphqlFetcher` turns that
    // into a thrown error — one toast from the loader plus a second from the global fetch interceptor, on every
    // page that mounts this hook. The gate lives here rather than at the call site so a future caller cannot forget
    // it. Community needs no scope data at all: there is no authorization boundary between workspace members there,
    // so `useHasWorkspaceScope` short-circuits to "granted" without consulting the store.
    const eeEdition = useEeEdition();

    const currentEnvironmentResult = useLoadEnvironmentWorkspaceScopes(workspaceId, currentEnvironmentId, eeEdition);

    // Workflow definitions are created, edited and deleted in Development whichever environment is selected, and the
    // server checks those writes there, so Development's scopes are loaded as well.
    useLoadEnvironmentWorkspaceScopes(
        currentEnvironmentId === DEVELOPMENT_ENVIRONMENT ? undefined : workspaceId,
        DEVELOPMENT_ENVIRONMENT,
        eeEdition
    );

    return currentEnvironmentResult;
};
