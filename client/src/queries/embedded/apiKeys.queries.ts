/* eslint-disable sort-keys */
import {ApiKeyApi, ApiKeyModel} from '@/middleware/embedded/user';
import {useQuery} from '@tanstack/react-query';

export const ApiKeyKeys = {
    apiKeys: ['apiKeys'] as const,
};

export const useGeApiKeysQuery = () =>
    useQuery<ApiKeyModel[], Error>({
        queryKey: ApiKeyKeys.apiKeys,
        queryFn: () => new ApiKeyApi().getApiKeys(),
    });
