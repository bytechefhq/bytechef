import {IntegrationInstanceApi, ProvisionableIntegration} from '@/ee/shared/middleware/embedded/configuration';
import {useQuery} from '@tanstack/react-query';

export const ProvisionableIntegrationKeys = {
    provisionableIntegrations: (connectedUserId: number) =>
        ['connectedUsers', connectedUserId, 'provisionableIntegrations'] as const,
};

export const useGetProvisionableIntegrationsQuery = (connectedUserId: number, enabled?: boolean) =>
    useQuery<ProvisionableIntegration[], Error>({
        enabled: enabled ?? true,
        queryFn: () => new IntegrationInstanceApi().getProvisionableIntegrations({connectedUserId}),
        queryKey: ProvisionableIntegrationKeys.provisionableIntegrations(connectedUserId),
    });
