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

    const eeEdition = useEeEdition();

    const currentEnvironmentResult = useLoadEnvironmentWorkspaceScopes(workspaceId, currentEnvironmentId, eeEdition);

    useLoadEnvironmentWorkspaceScopes(
        currentEnvironmentId === DEVELOPMENT_ENVIRONMENT ? undefined : workspaceId,
        DEVELOPMENT_ENVIRONMENT,
        eeEdition
    );

    return currentEnvironmentResult;
};
