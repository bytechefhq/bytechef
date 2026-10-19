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

export const findAvailableRememberedModel = (
    workspaceId: number | null | undefined,
    providerCatalog: AiProviderCatalogQuery['aiProviderCatalog'] | undefined
): {model: string; provider: string} | null => {
    if (workspaceId == null || providerCatalog == null) {
        return null;
    }

    const lastUsedModel = readLastUsedModel(workspaceId);

    if (lastUsedModel == null || !isAvailableModel(providerCatalog, lastUsedModel.provider, lastUsedModel.model)) {
        return null;
    }

    return lastUsedModel;
};

export const resolveRememberedModel = (
    workspaceId: number | null | undefined,
    providerCatalog: AiProviderCatalogQuery['aiProviderCatalog'] | undefined
): {model: string; provider: string} | null => {
    const availableRememberedModel = findAvailableRememberedModel(workspaceId, providerCatalog);

    if (availableRememberedModel == null && workspaceId != null && providerCatalog != null) {
        writeLastUsedModel(workspaceId, null, null);
    }

    return availableRememberedModel;
};
