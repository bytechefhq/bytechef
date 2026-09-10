import {AiProvider, AiProviderApi} from '@/shared/middleware/platform/configuration';

/* eslint-disable sort-keys */
import {useQuery} from '@tanstack/react-query';

export const AiProviderKeys = {
    aiProviders: (environment: number) => ['aiProviders', environment] as const,
};

export const useGetAiProvidersQuery = (environment: number) =>
    useQuery<AiProvider[], Error>({
        queryKey: AiProviderKeys.aiProviders(environment),
        queryFn: () => new AiProviderApi().getAiProviders({environment}),
    });
