/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.configuration.facade;

import com.bytechef.ee.platform.configuration.dto.AiDefaultModelDTO;
import com.bytechef.ee.platform.configuration.dto.AiDefaultModelWithApiKeyDTO;
import com.bytechef.ee.platform.configuration.dto.AiProviderCatalogItemDTO;
import com.bytechef.ee.platform.configuration.dto.AiProviderDTO;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiProviderFacade {

    void deleteAiProvider(int id, int environment);

    AiDefaultModelDTO getAiDefaultChatModel(int environmentId);

    /**
     * Resolves the default chat model for a specific provider, applying the same eligibility rules as
     * {@link #getAiDefaultChatModel(int)} (provider must be enabled, a chat provider, and have a configured model) but
     * scoped to a single provider key. Returns {@code null} when the provider is not eligible in the environment.
     */
    AiDefaultModelDTO getAiDefaultChatModel(String providerKey, int environmentId);

    AiDefaultModelWithApiKeyDTO getAiDefaultChatModelApiKey(int environmentId);

    AiDefaultModelDTO getAiDefaultEmbeddingModel(int environmentId);

    AiDefaultModelWithApiKeyDTO getAiDefaultEmbeddingModelApiKey(int environmentId);

    List<AiProviderCatalogItemDTO> getAiChatProviderCatalog(int environment);

    List<AiProviderDTO> getAiProviders(int environment);

    String getApiKey(String provider, int environment);

    String getUrl(String provider, int environment);

    void updateAiProvider(int id, boolean enabled, int environment);

    void updateAiProvider(int id, String apiKey, String url, int environment);
}
