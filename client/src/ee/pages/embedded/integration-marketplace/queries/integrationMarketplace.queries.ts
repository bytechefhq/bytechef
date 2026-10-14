import {IntegrationApi, IntegrationBasic} from '@bytechef/embedded-api';
import {useQuery} from '@tanstack/react-query';

export const IntegrationMarketplaceKeys = {
    integrations: ['integrationMarketplace', 'integrations'] as const,
};

export const useGetMarketplaceIntegrationsQuery = () =>
    useQuery<IntegrationBasic[]>({
        queryFn: () => new IntegrationApi().getFrontendIntegrations({}),
        queryKey: IntegrationMarketplaceKeys.integrations,
    });
