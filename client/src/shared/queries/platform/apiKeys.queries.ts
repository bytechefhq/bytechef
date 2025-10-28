/* eslint-disable sort-keys */
import {ApiKey, ApiKeyApi} from '@/shared/middleware/platform/security';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQuery} from '@tanstack/react-query';

export const ApiKeyKeys = {
    apiKeys: ['apiKeys'] as const,
};

export const useGetApiKeysQuery = () => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    return useQuery<ApiKey[], Error>({
        queryKey: ApiKeyKeys.apiKeys,
        queryFn: () =>
            new ApiKeyApi().getApiKeys({
                environmentId: currentEnvironmentId,
            }),
    });
};
