import {DEVELOPMENT_ENVIRONMENT, PRODUCTION_ENVIRONMENT, STAGING_ENVIRONMENT} from '@/shared/constants';
import useEeEdition from '@/shared/edition/useEeEdition';
import {useIsTenantAdmin} from '@/shared/hooks/useIsTenantAdmin';
import {EnvironmentEnum, type MyEmbeddedScopesQuery, useMyEmbeddedScopesQuery} from '@/shared/middleware/graphql';
import {useMemo} from 'react';

export const useAccessibleEmbeddedEnvironmentIds = (): number[] | undefined => {
    const eeEdition = useEeEdition();
    const tenantAdmin = useIsTenantAdmin();

    const enabled = eeEdition && !tenantAdmin;

    const {data: developmentScopesData} = useMyEmbeddedScopesQuery(
        {environment: EnvironmentEnum.Development},
        {enabled}
    );

    const {data: stagingScopesData} = useMyEmbeddedScopesQuery({environment: EnvironmentEnum.Staging}, {enabled});

    const {data: productionScopesData} = useMyEmbeddedScopesQuery({environment: EnvironmentEnum.Production}, {enabled});

    return useMemo(() => {
        if (!enabled) {
            return undefined;
        }

        const environmentScopes: [number, MyEmbeddedScopesQuery | undefined][] = [
            [DEVELOPMENT_ENVIRONMENT, developmentScopesData],
            [STAGING_ENVIRONMENT, stagingScopesData],
            [PRODUCTION_ENVIRONMENT, productionScopesData],
        ];

        if (environmentScopes.some(([, scopesData]) => scopesData === undefined)) {
            return undefined;
        }

        return environmentScopes
            .filter(([, scopesData]) => (scopesData?.myEmbeddedScopes ?? []).length > 0)
            .map(([environmentId]) => environmentId);
    }, [developmentScopesData, enabled, productionScopesData, stagingScopesData]);
};
