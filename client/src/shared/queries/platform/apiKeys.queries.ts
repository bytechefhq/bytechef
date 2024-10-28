/* eslint-disable sort-keys */
import {ApiKey, ApiKeyApi} from '@/shared/middleware/platform/user';
import {useQuery} from '@tanstack/react-query';

export const ApiKeyKeys = {
    apiKeys: ['apiKeys'] as const,
};

export const useGetApiKeysQuery = () =>
    useQuery<ApiKey[], Error>({
        queryKey: ApiKeyKeys.apiKeys,
        queryFn: () => new ApiKeyApi().getApiKeys(),
    });
