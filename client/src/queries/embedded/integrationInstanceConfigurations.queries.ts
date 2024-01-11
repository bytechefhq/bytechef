import {
    EnvironmentModel,
    IntegrationInstanceConfigurationApi,
    IntegrationInstanceConfigurationModel,
} from '@/middleware/embedded/configuration';

/* eslint-disable sort-keys */
import {useQuery} from '@tanstack/react-query';

export const IntegrationInstanceConfigurationKeys = {
    filteredIntegrationInstanceConfigurations: (filters: {
        environment?: EnvironmentModel;
        integrationId?: number;
        tagId?: number;
    }) => [...IntegrationInstanceConfigurationKeys.integrationInstanceConfigurations, filters],
    integrationInstanceConfigurationTags: ['integrationInstanceConfigurationTags'] as const,
    integrationInstanceConfigurations: ['integrationInstanceConfigurations'] as const,
};

export const useGetIntegrationInstanceConfigurationsQuery = (filters: {
    environment?: EnvironmentModel;
    integrationId?: number;
    tagId?: number;
}) =>
    useQuery<IntegrationInstanceConfigurationModel[], Error>({
        queryKey: IntegrationInstanceConfigurationKeys.filteredIntegrationInstanceConfigurations(filters),
        queryFn: () => new IntegrationInstanceConfigurationApi().getIntegrationInstanceConfigurations(filters),
    });
