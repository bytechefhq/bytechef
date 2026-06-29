/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.agent.catalog;

import com.bytechef.component.ai.llm.Provider;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.context.EnvironmentContext;
import com.bytechef.platform.configuration.domain.Environment;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Always-present {@link EmbeddingModel} that resolves the underlying provider model at call time from the UI-activated
 * provider for the current {@link EnvironmentContext} environment. Lets the application boot without a
 * statically-configured embedding provider; the Knowledge Base starts working as soon as a provider is activated.
 *
 * <p>
 * Built delegates are cached per {@code (environment, apiKey)}. The cache is bounded by size and idle-TTL so that
 * rotated or disabled keys are evicted rather than accumulating for the life of this singleton — important in
 * multi-tenant deployments where each tenant contributes its own per-environment key.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@Primary
@ConditionalOnEEVersion
public class CatalogEmbeddingModel implements EmbeddingModel {

    private static final int MAX_CACHED_DELEGATES = 256;
    private static final Duration DELEGATE_CACHE_TTL = Duration.ofHours(1);

    private final ProviderApiKeyResolver providerApiKeyResolver;
    private final CatalogEmbeddingModelFactory catalogEmbeddingModelFactory;
    private final ApplicationProperties applicationProperties;
    private final Cache<String, EmbeddingModel> delegateCache = Caffeine.newBuilder()
        .maximumSize(MAX_CACHED_DELEGATES)
        .expireAfterAccess(DELEGATE_CACHE_TTL)
        .build();

    @SuppressFBWarnings("EI")
    public CatalogEmbeddingModel(
        ProviderApiKeyResolver providerApiKeyResolver, CatalogEmbeddingModelFactory catalogEmbeddingModelFactory,
        ApplicationProperties applicationProperties) {

        this.providerApiKeyResolver = providerApiKeyResolver;
        this.catalogEmbeddingModelFactory = catalogEmbeddingModelFactory;
        this.applicationProperties = applicationProperties;
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        return resolveDelegate().call(request);
    }

    @Override
    public float[] embed(Document document) {
        return resolveDelegate().embed(document);
    }

    @Override
    public float[] embed(String text) {
        return resolveDelegate().embed(text);
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        return resolveDelegate().embed(texts);
    }

    @Override
    public List<float[]> embed(List<Document> documents, EmbeddingOptions options, BatchingStrategy batchingStrategy) {
        return resolveDelegate().embed(documents, options, batchingStrategy);
    }

    @Override
    public int dimensions() {
        return resolveDelegate().dimensions();
    }

    private EmbeddingModel resolveDelegate() {
        Environment environment = EnvironmentContext.getCurrentEnvironment();

        String model = applicationProperties.getAi()
            .getProvider()
            .getEmbedding()
            .getOpenAi()
            .getOptions()
            .getModel();

        String apiKey = providerApiKeyResolver.resolve(Provider.OPEN_AI, environment.ordinal());

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                "No embedding provider is activated for environment " + environment + ". Activate the OpenAI " +
                    "provider in the UI (or set bytechef.ai.provider.openai.api-key) so the Knowledge Base can embed " +
                    "documents.");
        }

        return delegateCache.get(
            environment.ordinal() + ":" + apiKey,
            ignoredKey -> catalogEmbeddingModelFactory.createEmbeddingModel(Provider.OPEN_AI, model, apiKey));
    }
}
