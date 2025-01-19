import {AiProvider, AiProviderApi} from '@/ee/shared/middleware/platform/configuration';

/* eslint-disable sort-keys */
import {useQuery} from '@tanstack/react-query';

export const AiProviderKeys = {
    aiProviders: ['aiProviders'] as const,
};

export const useGetAiProvidersQuery = () =>
    useQuery<AiProvider[], Error>({
        queryKey: AiProviderKeys.aiProviders,
        queryFn: () => new AiProviderApi().getAiProviders(),
    });
