import {ApiConnector, ApiConnectorApi} from '@/ee/shared/middleware/platform/api-connector';

/* eslint-disable sort-keys */
import {useQuery} from '@tanstack/react-query';

export const ApiConnectorKeys = {
    apiConnectors: ['apiConnectors'] as const,
};

export const useGetApiConnectorsQuery = () =>
    useQuery<ApiConnector[], Error>({
        queryKey: ApiConnectorKeys.apiConnectors,
        queryFn: () => new ApiConnectorApi().getApiConnectors(),
    });
