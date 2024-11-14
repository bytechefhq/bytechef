import {ApiClient, ApiClientApi} from '@/middleware/automation/api-platform';

/* eslint-disable sort-keys */
import {useQuery} from '@tanstack/react-query';

export const ApiClientKeys = {
    apiClients: ['apiClients'] as const,
};

export const useGetApiClientsQuery = () =>
    useQuery<ApiClient[], Error>({
        queryKey: ApiClientKeys.apiClients,
        queryFn: () => new ApiClientApi().getApiClients(),
    });
