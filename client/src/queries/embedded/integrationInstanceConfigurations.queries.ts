import {
    GetIntegrationInstanceConfigurationsRequest,
    IntegrationInstanceConfigurationApi,
    IntegrationInstanceConfigurationModel,
} from '@/middleware/embedded/configuration';

/* eslint-disable sort-keys */
import {useQuery} from '@tanstack/react-query';

export const IntegrationInstanceConfigurationKeys = {
    filteredIntegrationInstanceConfigurations: (filters: GetIntegrationInstanceConfigurationsRequest) => [
        ...IntegrationInstanceConfigurationKeys.integrationInstanceConfigurations,
        filters,
    ],
    integrationInstanceConfiguration: (id: number) => [
        ...IntegrationInstanceConfigurationKeys.integrationInstanceConfigurations,
        id,
    ],
    integrationInstanceConfigurations: ['integrationInstanceConfigurations'] as const,
};

export const useGetIntegrationInstanceConfigurationQuery = (id: number, enabled?: boolean) =>
    useQuery<IntegrationInstanceConfigurationModel, Error>({
        queryKey: IntegrationInstanceConfigurationKeys.integrationInstanceConfiguration(id),
        queryFn: () => new IntegrationInstanceConfigurationApi().getIntegrationInstanceConfiguration({id}),
        enabled: enabled === undefined ? true : enabled,
    });

export const useGetIntegrationInstanceConfigurationsQuery = (filters: GetIntegrationInstanceConfigurationsRequest) =>
    useQuery<IntegrationInstanceConfigurationModel[], Error>({
        queryKey: IntegrationInstanceConfigurationKeys.filteredIntegrationInstanceConfigurations(filters),
        queryFn: () => new IntegrationInstanceConfigurationApi().getIntegrationInstanceConfigurations(filters),
    });
