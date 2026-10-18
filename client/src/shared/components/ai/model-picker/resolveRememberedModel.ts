import {readLastUsedModel, writeLastUsedModel} from '@/shared/components/ai/model-picker/lastUsedModel';
import {AiProviderCatalogQuery} from '@/shared/middleware/graphql';

const isAvailableModel = (
    providerCatalog: AiProviderCatalogQuery['aiProviderCatalog'],
    providerKey: string,
    modelName: string
): boolean => {
    const provider = providerCatalog.find((candidate) => candidate?.key === providerKey);

    if (provider == null || !provider.enabled) {
        return false;
    }

    return provider.supportsModelById || provider.models.some((model) => model.name === modelName);
};

/**
 * The workspace's remembered model when the provider catalog still offers it. A remembered model the catalog no longer
 * offers is forgotten and null is returned, as is null while the catalog has not loaded.
 */
export const resolveRememberedModel = (
    workspaceId: number | null | undefined,
    providerCatalog: AiProviderCatalogQuery['aiProviderCatalog'] | undefined
): {model: string; provider: string} | null => {
    if (workspaceId == null || providerCatalog == null) {
        return null;
    }

    const lastUsedModel = readLastUsedModel(workspaceId);

    if (lastUsedModel == null) {
        return null;
    }

    if (!isAvailableModel(providerCatalog, lastUsedModel.provider, lastUsedModel.model)) {
        writeLastUsedModel(workspaceId, null, null);

        return null;
    }

    return lastUsedModel;
};
