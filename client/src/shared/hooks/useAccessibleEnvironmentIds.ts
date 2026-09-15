import {DEVELOPMENT_ENVIRONMENT, PRODUCTION_ENVIRONMENT, STAGING_ENVIRONMENT} from '@/shared/constants';
import useEeEdition from '@/shared/edition/useEeEdition';
import {EnvironmentEnum, type MyWorkspaceScopesQuery, useMyWorkspaceScopesQuery} from '@/shared/middleware/graphql';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {useMemo} from 'react';

export const useAccessibleEnvironmentIds = (workspaceId: number | undefined): number[] | undefined => {
    const account = useAuthenticationStore((state) => state.account);
    const authenticated = useAuthenticationStore((state) => state.authenticated);

    const eeEdition = useEeEdition();

    const tenantAdmin = authenticated && (account?.authorities?.includes('ROLE_ADMIN') ?? false);

    const enabled = eeEdition && !tenantAdmin && workspaceId != null && workspaceId > 0;

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
        if (!enabled) {
            return undefined;
        }

        const environmentScopes: [number, MyWorkspaceScopesQuery | undefined][] = [
            [DEVELOPMENT_ENVIRONMENT, developmentScopesData],
            [STAGING_ENVIRONMENT, stagingScopesData],
            [PRODUCTION_ENVIRONMENT, productionScopesData],
        ];

        if (environmentScopes.some(([, scopesData]) => scopesData === undefined)) {
            return undefined;
        }

        return environmentScopes
            .filter(([, scopesData]) => (scopesData?.myWorkspaceScopes ?? []).length > 0)
            .map(([environmentId]) => environmentId);
    }, [developmentScopesData, enabled, productionScopesData, stagingScopesData]);
};
