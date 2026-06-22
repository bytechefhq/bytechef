/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.connection.aiprovider;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.ai.llm.Provider;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.domain.Property.Scope;
import com.bytechef.platform.configuration.service.PropertyService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class AiProviderConnectionSourceImpl implements AiProviderConnectionSource {

    private static final List<Provider> SUPPORTED_PROVIDERS = List.of(
        Provider.OPEN_AI, Provider.ANTHROPIC, Provider.GROQ, Provider.MISTRAL, Provider.NVIDIA, Provider.PERPLEXITY,
        Provider.DEEPSEEK);

    private final PropertyService propertyService;
    private final ApplicationProperties applicationProperties;

    @SuppressFBWarnings("EI2")
    public AiProviderConnectionSourceImpl(
        PropertyService propertyService, ApplicationProperties applicationProperties) {

        this.propertyService = propertyService;
        this.applicationProperties = applicationProperties;
    }

    @Override
    public List<Provider> getSupportedProviders() {
        return SUPPORTED_PROVIDERS;
    }

    @Override
    public boolean isEnabled(Provider provider, int environmentId) {
        Optional<Property> property = propertyService.fetchProperty(
            provider.getKey(), Scope.PLATFORM, null, (long) environmentId);

        boolean enabledByProperty = property.map(Property::isEnabled)
            .orElse(false);

        return enabledByProperty || getConfigApiKey(provider) != null;
    }

    @Override
    public Optional<String> getApiKey(Provider provider, int environmentId) {
        Optional<Property> property = propertyService.fetchProperty(
            provider.getKey(), Scope.PLATFORM, null, (long) environmentId);

        if (property.isPresent() && property.get()
            .isEnabled()) {

            Object apiKey = property.get()
                .get("apiKey");

            if (apiKey instanceof String stringApiKey && !stringApiKey.isBlank()) {
                return Optional.of(stringApiKey);
            }
        }

        String configApiKey = getConfigApiKey(provider);

        return configApiKey == null ? Optional.empty() : Optional.of(configApiKey);
    }

    private String getConfigApiKey(Provider provider) {
        ApplicationProperties.Ai.Provider configProvider = applicationProperties.getAi()
            .getProvider();

        String apiKey = switch (provider) {
            case OPEN_AI -> configProvider.getOpenAi()
                .getApiKey();
            case ANTHROPIC -> configProvider.getAnthropic()
                .getApiKey();
            case GROQ -> configProvider.getGroq()
                .getApiKey();
            case MISTRAL -> configProvider.getMistral()
                .getApiKey();
            case NVIDIA -> configProvider.getNvidia()
                .getApiKey();
            case PERPLEXITY -> configProvider.getPerplexity()
                .getApiKey();
            case DEEPSEEK -> configProvider.getDeepSeek()
                .getApiKey();
            default -> null;
        };

        return apiKey == null || apiKey.isBlank() ? null : apiKey;
    }
}
