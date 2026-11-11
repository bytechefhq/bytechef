import {ApiKeysProviderStateI} from '@/shared/components/api-keys/providers/apiKeysProvider';
import {getApiKeyMutationHooks} from '@/shared/components/api-keys/utils/getApiKeyMutationHooks';
import {ApiKey, PlatformType, useApiKeysQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {UseQueryResult, useQueryClient} from '@tanstack/react-query';

const useEmbeddedApiKeysProviderValue = (): ApiKeysProviderStateI => {
    const environmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const queryClient = useQueryClient();

    return {
        ...getApiKeyMutationHooks({environmentId, queryClient, queryKey: ['apiKeys'], type: PlatformType.Embedded}),
        useApiKeysQuery: () => {
            const {
                data: {apiKeys} = {},
                error,
                isLoading,
            } = useApiKeysQuery({
                environmentId: `${environmentId}`,
                type: PlatformType.Embedded,
            });

            return {
                data: apiKeys,
                error,
                isLoading,
            } as UseQueryResult<ApiKey[], Error>;
        },
    };
};

export default useEmbeddedApiKeysProviderValue;
