import {
    ConfigurationConnectedUser,
    ConfigurationMoveImpact,
    ConfigurationMoveRequest,
    ConfigurationUpgradeRequest,
    GetIntegrationInstanceConfigurationsRequest,
    IntegrationInstanceConfiguration,
    IntegrationInstanceConfigurationApi,
} from '@/ee/shared/middleware/embedded/configuration';

/* eslint-disable sort-keys */
import {useQuery} from '@tanstack/react-query';

export const IntegrationInstanceConfigurationKeys = {
    filteredIntegrationInstanceConfigurations: (filters: GetIntegrationInstanceConfigurationsRequest) => [
        ...IntegrationInstanceConfigurationKeys.integrationInstanceConfigurations,
        filters,
    ],
    integrationInstanceConfigurationConnectedUsers: (id: number) => [
        ...IntegrationInstanceConfigurationKeys.integrationInstanceConfiguration(id),
        'connectedUsers',
    ],
    integrationInstanceConfigurationMoveImpact: (id: number, request: ConfigurationMoveRequest) => [
        ...IntegrationInstanceConfigurationKeys.integrationInstanceConfiguration(id),
        'moveImpact',
        request,
    ],
    integrationInstanceConfigurationUpgradeImpact: (id: number, request: ConfigurationUpgradeRequest) => [
        ...IntegrationInstanceConfigurationKeys.integrationInstanceConfiguration(id),
        'upgradeImpact',
        request,
    ],
    integrationInstanceConfiguration: (id: number) => [
        ...IntegrationInstanceConfigurationKeys.integrationInstanceConfigurations,
        id,
    ],
    integrationInstanceConfigurations: ['integrationInstanceConfigurations'] as const,
};

export const useGetIntegrationInstanceConfigurationQuery = (id: number, enabled?: boolean) =>
    useQuery<IntegrationInstanceConfiguration, Error>({
        queryKey: IntegrationInstanceConfigurationKeys.integrationInstanceConfiguration(id),
        queryFn: () => new IntegrationInstanceConfigurationApi().getIntegrationInstanceConfiguration({id}),
        enabled: enabled === undefined ? true : enabled,
    });

export const useGetIntegrationInstanceConfigurationsQuery = (
    filters: GetIntegrationInstanceConfigurationsRequest,
    enabled?: boolean
) =>
    useQuery<IntegrationInstanceConfiguration[], Error>({
        queryKey: IntegrationInstanceConfigurationKeys.filteredIntegrationInstanceConfigurations(filters),
        queryFn: () => new IntegrationInstanceConfigurationApi().getIntegrationInstanceConfigurations(filters),
        enabled: enabled === undefined ? true : enabled,
    });

export const useGetIntegrationInstanceConfigurationConnectedUsersQuery = (id: number, enabled?: boolean) =>
    useQuery<ConfigurationConnectedUser[], Error>({
        queryKey: IntegrationInstanceConfigurationKeys.integrationInstanceConfigurationConnectedUsers(id),
        queryFn: () =>
            new IntegrationInstanceConfigurationApi().getIntegrationInstanceConfigurationConnectedUsers({id}),
        enabled: enabled === undefined ? true : enabled,
    });

export const useGetIntegrationInstanceConfigurationMoveImpactQuery = (
    id: number,
    configurationMoveRequest: ConfigurationMoveRequest,
    enabled?: boolean
) =>
    useQuery<ConfigurationMoveImpact, Error>({
        queryKey: IntegrationInstanceConfigurationKeys.integrationInstanceConfigurationMoveImpact(
            id,
            configurationMoveRequest
        ),
        queryFn: () =>
            new IntegrationInstanceConfigurationApi().getIntegrationInstanceConfigurationMoveImpact({
                configurationMoveRequest,
                id,
            }),
        enabled: enabled === undefined ? true : enabled,
    });

export const useGetIntegrationInstanceConfigurationUpgradeImpactQuery = (
    id: number,
    configurationUpgradeRequest: ConfigurationUpgradeRequest,
    enabled?: boolean
) =>
    useQuery<ConfigurationMoveImpact, Error>({
        queryKey: IntegrationInstanceConfigurationKeys.integrationInstanceConfigurationUpgradeImpact(
            id,
            configurationUpgradeRequest
        ),
        queryFn: () =>
            new IntegrationInstanceConfigurationApi().getIntegrationInstanceConfigurationUpgradeImpact({
                configurationUpgradeRequest,
                id,
            }),
        enabled: enabled === undefined ? true : enabled,
    });
