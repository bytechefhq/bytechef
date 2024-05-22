import {IntegrationInstanceApi, IntegrationInstanceModel} from '@/shared/middleware/embedded/configuration';

/* eslint-disable sort-keys */
import {useQuery} from '@tanstack/react-query';

export const IntegrationInstanceKeys = {
    integrationInstance: (id: number) => [...IntegrationInstanceKeys.integrationInstances, id],
    integrationInstances: ['integrationInstances'] as const,
};

export const useGetIntegrationInstanceQuery = (id: number) =>
    useQuery<IntegrationInstanceModel, Error>({
        queryKey: IntegrationInstanceKeys.integrationInstance(id),
        queryFn: () => new IntegrationInstanceApi().getIntegrationInstance({id}),
    });
