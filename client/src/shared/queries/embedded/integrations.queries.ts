import {Integration, IntegrationApi, IntegrationStatus} from '@/shared/middleware/embedded/configuration';

/* eslint-disable sort-keys */
import {useQuery} from '@tanstack/react-query';

export const IntegrationKeys = {
    filteredIntegrations: (filters: {categoryId?: number; tagId?: number}) => [
        ...IntegrationKeys.integrations,
        filters,
    ],
    integration: (id: number) => [...IntegrationKeys.integrations, id],
    integrations: ['integrations'] as const,
};

export const useGetIntegrationQuery = (id: number, initialData?: Integration, enabled?: boolean) =>
    useQuery<Integration, Error>({
        queryKey: IntegrationKeys.integration(id),
        queryFn: () => new IntegrationApi().getIntegration({id}),
        initialData,
        enabled: enabled === undefined ? true : enabled,
    });

export const useGetIntegrationsQuery = (filters?: {
    categoryId?: number;
    integrationInstanceConfigurations?: boolean;
    tagId?: number;
    status?: IntegrationStatus;
}) =>
    useQuery<Integration[], Error>({
        queryKey: IntegrationKeys.filteredIntegrations(filters ?? {}),
        queryFn: () => new IntegrationApi().getIntegrations(filters),
    });
