import {Connection, IntegrationInstance, IntegrationInstanceApi} from '@/ee/shared/middleware/embedded/configuration';

/* eslint-disable sort-keys */
import {useQuery} from '@tanstack/react-query';

export const IntegrationInstanceKeys = {
    integrationInstance: (id: number) => [...IntegrationInstanceKeys.integrationInstances, id],
    integrationInstanceConnection: (id: number) => [...IntegrationInstanceKeys.integrationInstance(id), 'connection'],
    integrationInstances: ['integrationInstances'] as const,
};

export const useGetIntegrationInstanceQuery = (id: number) =>
    useQuery<IntegrationInstance, Error>({
        queryKey: IntegrationInstanceKeys.integrationInstance(id),
        queryFn: () => new IntegrationInstanceApi().getIntegrationInstance({id}),
    });

export const useGetIntegrationInstanceConnectionQuery = (integrationInstanceId: number, enabled?: boolean) =>
    useQuery<Connection, Error>({
        queryKey: IntegrationInstanceKeys.integrationInstanceConnection(integrationInstanceId),
        queryFn: () => new IntegrationInstanceApi().getIntegrationInstanceConnection({id: integrationInstanceId}),
        enabled: enabled === undefined ? true : enabled,
    });
