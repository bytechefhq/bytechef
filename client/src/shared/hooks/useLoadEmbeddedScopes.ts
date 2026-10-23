import {DEVELOPMENT_ENVIRONMENT, PRODUCTION_ENVIRONMENT, STAGING_ENVIRONMENT} from '@/shared/constants';
import useEeEdition from '@/shared/edition/useEeEdition';
import {UNION_ENVIRONMENT_ID} from '@/shared/hooks/useHasEmbeddedScope';
import {EnvironmentEnum, useMyEmbeddedScopesQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {usePermissionStore} from '@/shared/stores/usePermissionStore';
import {useEffect} from 'react';
import {toast} from 'sonner';

const ENVIRONMENT_ENUMS: Record<number, EnvironmentEnum | null> = {
    [DEVELOPMENT_ENVIRONMENT]: EnvironmentEnum.Development,
    [PRODUCTION_ENVIRONMENT]: EnvironmentEnum.Production,
    [STAGING_ENVIRONMENT]: EnvironmentEnum.Staging,
    [UNION_ENVIRONMENT_ID]: null,
};

export interface UseLoadEmbeddedScopesResultI {
    error: boolean;
    loading: boolean;
}

const useLoadEnvironmentEmbeddedScopes = (environmentId: number, enabled: boolean): UseLoadEmbeddedScopesResultI => {
    const setEmbeddedScopeError = usePermissionStore((state) => state.setEmbeddedScopeError);
    const setEmbeddedScopeLoading = usePermissionStore((state) => state.setEmbeddedScopeLoading);
    const setEmbeddedScopePermissions = usePermissionStore((state) => state.setEmbeddedScopePermissions);

    const environment = ENVIRONMENT_ENUMS[environmentId];

    const {
        data: scopesData,
        isError,
        isLoading,
    } = useMyEmbeddedScopesQuery({environment}, {enabled: enabled && environment !== undefined});

    useEffect(() => {
        if (!enabled || environment === undefined) {
            return;
        }

        if (isLoading) {
            setEmbeddedScopeLoading(environmentId);

            return;
        }

        if (isError && scopesData === undefined) {
            setEmbeddedScopeError(environmentId);

            toast.error('Could not load embedded permissions. Some actions may appear restricted until you refresh.', {
                id: 'embedded-permissions-error',
            });
            console.error('Failed to load embedded permissions for environmentId:', environmentId);

            return;
        }

        if (scopesData !== undefined) {
            setEmbeddedScopePermissions(environmentId, scopesData.myEmbeddedScopes ?? []);
        }
    }, [
        enabled,
        environment,
        environmentId,
        isError,
        isLoading,
        scopesData,
        setEmbeddedScopeError,
        setEmbeddedScopeLoading,
        setEmbeddedScopePermissions,
    ]);

    return {error: isError && scopesData === undefined, loading: isLoading};
};

export const useLoadEmbeddedScopes = (active: boolean): UseLoadEmbeddedScopesResultI => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const eeEdition = useEeEdition();

    const enabled = eeEdition && active;

    const unionResult = useLoadEnvironmentEmbeddedScopes(UNION_ENVIRONMENT_ID, enabled);

    useLoadEnvironmentEmbeddedScopes(currentEnvironmentId, enabled);

    useLoadEnvironmentEmbeddedScopes(
        DEVELOPMENT_ENVIRONMENT,
        enabled && currentEnvironmentId !== DEVELOPMENT_ENVIRONMENT
    );

    return unionResult;
};
