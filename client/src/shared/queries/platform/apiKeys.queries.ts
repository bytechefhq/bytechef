import {useEnvironmentStore} from '@/pages/automation/stores/useEnvironmentStore';

/* eslint-disable sort-keys */
import {ApiKey, ApiKeyApi} from '@/shared/middleware/platform/security';
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
