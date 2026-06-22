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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.ai.llm.Provider;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.domain.Property.Scope;
import com.bytechef.platform.configuration.service.PropertyService;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiProviderConnectionSourceImplTest {

    @Mock
    private PropertyService propertyService;

    @Mock
    private ApplicationProperties applicationProperties;

    @Test
    void testSupportedProvidersExcludeGemini() {
        AiProviderConnectionSourceImpl source =
            new AiProviderConnectionSourceImpl(propertyService, applicationProperties);

        assertThat(source.getSupportedProviders())
            .containsExactlyInAnyOrder(
                Provider.OPEN_AI, Provider.ANTHROPIC, Provider.GROQ, Provider.MISTRAL, Provider.NVIDIA,
                Provider.PERPLEXITY, Provider.DEEPSEEK)
            .doesNotContain(Provider.VERTEX_GEMINI);
    }

    @Test
    void testEnabledViaPropertyAndApiKeyReturned() {
        Property property = new Property();

        property.setKey(Provider.OPEN_AI.getKey());
        property.setEnabled(true);
        property.setValue(Map.of("apiKey", "sk-prop"));

        when(propertyService.fetchProperty(Provider.OPEN_AI.getKey(), Scope.PLATFORM, null, 0L))
            .thenReturn(Optional.of(property));

        AiProviderConnectionSourceImpl source =
            new AiProviderConnectionSourceImpl(propertyService, applicationProperties);

        assertThat(source.isEnabled(Provider.OPEN_AI, 0)).isTrue();
        assertThat(source.getApiKey(Provider.OPEN_AI, 0)).contains("sk-prop");
    }

    @Test
    void testDisabledWhenNoPropertyAndNoConfigKey() {
        when(propertyService.fetchProperty(Provider.GROQ.getKey(), Scope.PLATFORM, null, 1L))
            .thenReturn(Optional.empty());
        when(applicationProperties.getAi()).thenReturn(emptyAi());

        AiProviderConnectionSourceImpl source =
            new AiProviderConnectionSourceImpl(propertyService, applicationProperties);

        assertThat(source.isEnabled(Provider.GROQ, 1)).isFalse();
        assertThat(source.getApiKey(Provider.GROQ, 1)).isEmpty();
    }

    @Test
    void testDisabledPropertyFallsBackToConfigApiKey() {
        Property property = new Property();

        property.setKey(Provider.OPEN_AI.getKey());
        property.setEnabled(false);
        property.setValue(Map.of("apiKey", "sk-prop-should-be-ignored"));

        when(propertyService.fetchProperty(Provider.OPEN_AI.getKey(), Scope.PLATFORM, null, 0L))
            .thenReturn(Optional.of(property));

        ApplicationProperties.Ai ai = new ApplicationProperties.Ai();
        ApplicationProperties.Ai.Provider provider = new ApplicationProperties.Ai.Provider();
        ApplicationProperties.Ai.Provider.OpenAi openAi = new ApplicationProperties.Ai.Provider.OpenAi();

        openAi.setApiKey("sk-config-key");
        provider.setOpenAi(openAi);
        ai.setProvider(provider);

        when(applicationProperties.getAi()).thenReturn(ai);

        AiProviderConnectionSourceImpl source =
            new AiProviderConnectionSourceImpl(propertyService, applicationProperties);

        assertThat(source.getApiKey(Provider.OPEN_AI, 0)).contains("sk-config-key");
    }

    @Test
    void testEnabledPropertyWithBlankApiKeyFallsBackToConfigApiKey() {
        Property property = new Property();

        property.setKey(Provider.OPEN_AI.getKey());
        property.setEnabled(true);
        property.setValue(Map.of("apiKey", ""));

        when(propertyService.fetchProperty(Provider.OPEN_AI.getKey(), Scope.PLATFORM, null, 0L))
            .thenReturn(Optional.of(property));

        ApplicationProperties.Ai ai = new ApplicationProperties.Ai();
        ApplicationProperties.Ai.Provider provider = new ApplicationProperties.Ai.Provider();
        ApplicationProperties.Ai.Provider.OpenAi openAi = new ApplicationProperties.Ai.Provider.OpenAi();

        openAi.setApiKey("sk-config-key");
        provider.setOpenAi(openAi);
        ai.setProvider(provider);

        when(applicationProperties.getAi()).thenReturn(ai);

        AiProviderConnectionSourceImpl source =
            new AiProviderConnectionSourceImpl(propertyService, applicationProperties);

        assertThat(source.getApiKey(Provider.OPEN_AI, 0)).contains("sk-config-key");
    }

    private static ApplicationProperties.Ai emptyAi() {
        ApplicationProperties.Ai ai = new ApplicationProperties.Ai();
        ApplicationProperties.Ai.Provider provider = new ApplicationProperties.Ai.Provider();

        provider.setGroq(new ApplicationProperties.Ai.Provider.Groq());
        ai.setProvider(provider);

        return ai;
    }
}
