import {useAiDefaultModelQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';

export interface HasEnabledAiProviderStateI {
    hasEnabledAiProvider: boolean;
    isPending: boolean;
}

/**
 * Reports whether at least one AI provider is enabled for the current environment, using the same
 * `aiDefaultModel` GraphQL query (and react-query cache entry) the AI Copilot panel uses for its
 * "No AI providers enabled" empty state: when no provider is enabled, no default model resolves.
 * While the query is pending, callers should treat AI features as available so users don't see a
 * disabled-state flash on load. Query errors are treated the same way as pending, so a transient
 * network or GraphQL failure fails open instead of disabling AI features.
 */
export const useHasEnabledAiProvider = (): HasEnabledAiProviderStateI => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const {data, isError, isPending} = useAiDefaultModelQuery({environment: String(currentEnvironmentId)});

    return {
        hasEnabledAiProvider: data?.aiDefaultModel != null,
        // A failed availability check must not disable AI features behind a misleading "enable a
        // provider" tooltip, so errors fail open like pending; the gated action itself still
        // surfaces its own server-side error if no provider truly exists.
        isPending: isPending || Boolean(isError),
    };
};
