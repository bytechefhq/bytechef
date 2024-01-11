/* eslint-disable sort-keys */
import {IntegrationInstanceApi, IntegrationInstanceModel} from '@/middleware/embedded/configuration';
import {useQuery} from '@tanstack/react-query';

export const IntegrationInstanceKeys = {
    filteredIntegrationInstances: (filters: {integrationId?: number; tagId?: number}) => [
        ...IntegrationInstanceKeys.integrationInstances,
        filters,
    ],
    integrationInstanceTags: ['integrationInstanceTags'] as const,
    integrationInstances: ['integrationInstances'] as const,
};

export const useGetIntegrationInstancesQuery = (filters: {integrationId?: number; tagId?: number}) =>
    useQuery<IntegrationInstanceModel[], Error>({
        queryKey: IntegrationInstanceKeys.filteredIntegrationInstances(filters),
        queryFn: () => new IntegrationInstanceApi().getIntegrationInstances(filters),
    });
