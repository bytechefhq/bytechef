/* eslint-disable sort-keys */
import {ApiKey, ApiKeyApi} from '@/shared/middleware/automation/user';
import {useQuery} from '@tanstack/react-query';

export const ApiKeyKeys = {
    apiKeys: ['apiKeys'] as const,
};

export const useGeApiKeysQuery = () =>
    useQuery<ApiKey[], Error>({
        queryKey: ApiKeyKeys.apiKeys,
        queryFn: () => new ApiKeyApi().getApiKeys(),
    });
