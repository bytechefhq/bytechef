/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.agent.catalog;

import com.bytechef.component.ai.llm.Provider;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.ai.EmbeddingProviderStatusProvider;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

/**
 * EE {@link EmbeddingProviderStatusProvider}: embeddings are active for an environment when the OpenAI API key resolves
 * (from the activated provider property, falling back to static config) — the same predicate the runtime embedding
 * model uses.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class CatalogEmbeddingProviderStatusProvider implements EmbeddingProviderStatusProvider {

    private final ProviderApiKeyResolver providerApiKeyResolver;

    @SuppressFBWarnings("EI")
    public CatalogEmbeddingProviderStatusProvider(ProviderApiKeyResolver providerApiKeyResolver) {
        this.providerApiKeyResolver = providerApiKeyResolver;
    }

    @Override
    public boolean isEmbeddingActive(int environment) {
        String apiKey = providerApiKeyResolver.resolve(Provider.OPEN_AI, environment);

        return apiKey != null && !apiKey.isBlank();
    }
}
