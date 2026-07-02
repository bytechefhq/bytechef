/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.agent.catalog;

import com.bytechef.ee.platform.configuration.facade.AiProviderFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.ai.EmbeddingProviderStatusProvider;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

/**
 * EE {@link EmbeddingProviderStatusProvider}: embeddings are active for an environment when a default embedding
 * provider is activated and its API key resolves — the same predicate the runtime embedding model uses to build its
 * delegate.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class CatalogEmbeddingProviderStatusProvider implements EmbeddingProviderStatusProvider {

    private final AiProviderFacade aiProviderFacade;

    @SuppressFBWarnings("EI")
    public CatalogEmbeddingProviderStatusProvider(AiProviderFacade aiProviderFacade) {
        this.aiProviderFacade = aiProviderFacade;
    }

    @Override
    public boolean isEmbeddingActive(int environment) {
        return aiProviderFacade.getAiDefaultEmbeddingModelApiKey(environment) != null;
    }
}
