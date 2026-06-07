/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.configuration.facade;

import com.bytechef.ee.platform.configuration.dto.AiDefaultModelDTO;
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

    /**
     * The deployment default chat model the agents fall back to when no model is explicitly selected — mirrors the
     * runtime {@code @Primary} {@link org.springframework.ai.chat.model.ChatModel} (anthropic when its api-key is set,
     * otherwise openai). Returns {@code null} when no provider api-key is configured or the resolved provider has no
     * configured chat model.
     */
    AiDefaultModelDTO getAiDefaultModel();

    List<AiProviderCatalogItemDTO> getAiProviderCatalog(int environment);

    List<AiProviderDTO> getAiProviders(int environment);

    void updateAiProvider(int id, boolean enabled, int environment);

    void updateAiProvider(int id, String apiKey, int environment);
}
