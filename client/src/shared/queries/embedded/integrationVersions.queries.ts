/* eslint-disable sort-keys */
import {IntegrationApi, IntegrationVersionModel} from '@/shared/middleware/embedded/configuration';
import {useQuery} from '@tanstack/react-query';

export const IntegrationVersionKeys = {
    integrationIntegrationVersions: (integrationId: number) => [
        ...IntegrationVersionKeys.integrationVersions,
        'integrations',
        integrationId,
    ],
    integrationVersion: (id: number) => [...IntegrationVersionKeys.integrationVersions, id],
    integrationVersions: ['integrationVersions'] as const,
};

export const useGetIntegrationVersionsQuery = (integrationId: number, enabled?: boolean) =>
    useQuery<IntegrationVersionModel[], Error>({
        queryFn: () => new IntegrationApi().getIntegrationVersions({id: integrationId}),
        queryKey: IntegrationVersionKeys.integrationIntegrationVersions(integrationId),
        enabled: enabled === undefined ? true : enabled,
    });
