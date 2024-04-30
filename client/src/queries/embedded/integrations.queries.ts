/* eslint-disable sort-keys */
import {useQuery} from '@tanstack/react-query';
import {IntegrationApi, IntegrationModel, IntegrationStatusModel} from 'middleware/embedded/configuration';

export const IntegrationKeys = {
    filteredIntegrations: (filters: {categoryId?: number; tagId?: number}) => [
        ...IntegrationKeys.integrations,
        filters,
    ],
    integration: (id: number) => ['integration', id],
    integrations: ['integrations'] as const,
};

export const useGetIntegrationQuery = (id: number, initialData?: IntegrationModel, enabled?: boolean) =>
    useQuery<IntegrationModel, Error>({
        queryKey: IntegrationKeys.integration(id),
        queryFn: () => new IntegrationApi().getIntegration({id}),
        initialData,
        enabled: enabled === undefined ? true : enabled,
    });

export const useGetIntegrationsQuery = (filters?: {
    categoryId?: number;
    integrationInstanceConfigurations?: boolean;
    tagId?: number;
    status?: IntegrationStatusModel;
}) =>
    useQuery<IntegrationModel[], Error>({
        queryKey: IntegrationKeys.filteredIntegrations(filters ?? {}),
        queryFn: () => new IntegrationApi().getIntegrations(filters),
    });
