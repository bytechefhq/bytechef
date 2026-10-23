import {DEVELOPMENT_ENVIRONMENT, PRODUCTION_ENVIRONMENT, STAGING_ENVIRONMENT} from '@/shared/constants';
import useCeEdition from '@/shared/edition/useCeEdition';
import useEeEdition from '@/shared/edition/useEeEdition';
import {type WorkspaceScopeType} from '@/shared/hooks/useHasWorkspaceScope';
import {useIsTenantAdmin} from '@/shared/hooks/useIsTenantAdmin';
import {EnvironmentEnum, type MyWorkspaceScopesQuery, useMyWorkspaceScopesQuery} from '@/shared/middleware/graphql';
import {useMemo} from 'react';

/**
 * Returns the ids of the environments in which the member holds `scope` on the workspace; `undefined` when every
 * environment is allowed (Community and tenant admins).
 */
export const useWorkspaceScopeEnvironmentIds = (
    workspaceId: number | undefined,
    scope: WorkspaceScopeType
): number[] | undefined => {
    const ceEdition = useCeEdition();
    const eeEdition = useEeEdition();
    const tenantAdmin = useIsTenantAdmin();

    const unrestricted = ceEdition || tenantAdmin;
    const enabled = eeEdition && !unrestricted && workspaceId != null && workspaceId > 0;

    const workspaceIdVariable = String(workspaceId ?? 0);

    const {data: developmentScopesData} = useMyWorkspaceScopesQuery(
        {environment: EnvironmentEnum.Development, workspaceId: workspaceIdVariable},
        {enabled}
    );

    const {data: stagingScopesData} = useMyWorkspaceScopesQuery(
        {environment: EnvironmentEnum.Staging, workspaceId: workspaceIdVariable},
        {enabled}
    );

    const {data: productionScopesData} = useMyWorkspaceScopesQuery(
        {environment: EnvironmentEnum.Production, workspaceId: workspaceIdVariable},
        {enabled}
    );

    return useMemo(() => {
        if (unrestricted) {
            return undefined;
        }

        if (!enabled) {
            return [];
        }

        const environmentScopes: [number, MyWorkspaceScopesQuery | undefined][] = [
            [DEVELOPMENT_ENVIRONMENT, developmentScopesData],
            [STAGING_ENVIRONMENT, stagingScopesData],
            [PRODUCTION_ENVIRONMENT, productionScopesData],
        ];

        return environmentScopes
            .filter(([, scopesData]) => (scopesData?.myWorkspaceScopes ?? []).includes(scope))
            .map(([environmentId]) => environmentId);
    }, [developmentScopesData, enabled, productionScopesData, scope, stagingScopesData, unrestricted]);
};
