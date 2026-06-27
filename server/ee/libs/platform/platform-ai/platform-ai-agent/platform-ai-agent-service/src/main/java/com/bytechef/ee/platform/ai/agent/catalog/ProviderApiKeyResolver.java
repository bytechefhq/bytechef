/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.agent.catalog;

import com.bytechef.component.ai.llm.Provider;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.domain.Property.Scope;
import com.bytechef.platform.configuration.service.PropertyService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Resolves a catalog provider's API key for an environment: the UI-activated platform property
 * ({@link Property#isEnabled()}) wins, falling back to the statically-configured key. Shared by the chat client
 * resolver and the embedding model so the two cannot diverge.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class ProviderApiKeyResolver {

    private final PropertyService propertyService;
    private final ApplicationProperties applicationProperties;

    @SuppressFBWarnings("EI")
    public ProviderApiKeyResolver(PropertyService propertyService, ApplicationProperties applicationProperties) {
        this.propertyService = propertyService;
        this.applicationProperties = applicationProperties;
    }

    public @Nullable String resolve(Provider provider, int environment) {
        Optional<Property> property = propertyService.fetchProperty(
            provider.getKey(), Scope.PLATFORM, null, (long) environment);

        return property
            .filter(Property::isEnabled)
            .map(enabledProperty -> enabledProperty.get("apiKey"))
            .map(Object::toString)
            .filter(apiKey -> !apiKey.isBlank())
            .orElseGet(() -> configApiKey(provider));
    }

    private @Nullable String configApiKey(Provider provider) {
        ApplicationProperties.Ai.Provider configProvider = applicationProperties.getAi()
            .getProvider();

        return switch (provider) {
            case OPEN_AI -> configProvider.getOpenAi()
                .getApiKey();
            case ANTHROPIC -> configProvider.getAnthropic()
                .getApiKey();
            case MISTRAL -> configProvider.getMistral()
                .getApiKey();
            case VERTEX_GEMINI -> configProvider.getVertexGemini()
                .getApiKey();
            case GROQ -> configProvider.getGroq()
                .getApiKey();
            case PERPLEXITY -> configProvider.getPerplexity()
                .getApiKey();
            case NVIDIA -> configProvider.getNvidia()
                .getApiKey();
            case DEEPSEEK -> configProvider.getDeepSeek()
                .getApiKey();
            default -> null;
        };
    }
}
