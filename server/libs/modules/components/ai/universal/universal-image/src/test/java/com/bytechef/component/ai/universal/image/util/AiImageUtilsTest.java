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

package com.bytechef.component.ai.universal.image.util;

import static com.bytechef.platform.ai.llm.Provider.ANTHROPIC;
import static com.bytechef.platform.ai.llm.Provider.OPEN_AI;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Option;
import com.bytechef.config.ApplicationProperties.Ai;
import com.bytechef.platform.ai.llm.Provider;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.domain.Property.Scope;
import com.bytechef.platform.configuration.service.PropertyService;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class AiImageUtilsTest {

    private static final Long ENVIRONMENT_ID = 3L;

    private static final List<String> SWITCH_PROVIDER_OPTIONS = List.of(
        "AZURE_OPEN_AI", "OPEN_AI", "STABILITY");

    private final PropertyService propertyService = mock(PropertyService.class);

    @Test
    void testIsSelectableWhenConfiguredApiKeyPresent() {
        Ai.Provider aiProvider = new Ai.Provider();

        aiProvider.getOpenAi()
            .setApiKey("sk-open-ai");

        assertTrue(AiImageUtils.isSelectable(OPEN_AI, aiProvider, List.of()));
    }

    @Test
    void testIsSelectableWhenActiveProviderKeyPresent() {
        assertTrue(AiImageUtils.isSelectable(OPEN_AI, new Ai.Provider(), List.of(OPEN_AI.getKey())));
    }

    @Test
    void testNotSelectableWhenImageProviderHasNoKey() {
        assertFalse(AiImageUtils.isSelectable(OPEN_AI, new Ai.Provider(), List.of()));
    }

    @Test
    void testNotSelectableForNonImageProviderEvenWithKey() {
        Ai.Provider aiProvider = new Ai.Provider();

        aiProvider.getAnthropic()
            .setApiKey("sk-anthropic");

        assertFalse(AiImageUtils.isSelectable(ANTHROPIC, aiProvider, List.of(ANTHROPIC.getKey())));
    }

    @Test
    void testProviderOptionsMatchTheReplacedSwitchWhenEveryApiKeyIsConfigured() {
        when(propertyService.getProperties(anyList(), eq(Scope.PLATFORM), isNull(), eq(ENVIRONMENT_ID)))
            .thenReturn(List.of());

        List<? extends Option<String>> providerOptions = AiImageUtils.getProviderOptions(
            createAiProviderWithEveryApiKey(), propertyService, ENVIRONMENT_ID);

        assertEquals(SWITCH_PROVIDER_OPTIONS, getValues(providerOptions));
    }

    @Test
    void testProviderOptionsMatchTheReplacedSwitchWhenEveryProviderIsActive() {
        List<Property> properties = Arrays.stream(Provider.values())
            .map(provider -> createProperty(provider.getKey(), true))
            .toList();

        when(propertyService.getProperties(anyList(), eq(Scope.PLATFORM), isNull(), eq(ENVIRONMENT_ID)))
            .thenReturn(properties);

        List<? extends Option<String>> providerOptions = AiImageUtils.getProviderOptions(
            new Ai.Provider(), propertyService, ENVIRONMENT_ID);

        assertEquals(SWITCH_PROVIDER_OPTIONS, getValues(providerOptions));
    }

    @Test
    void testProviderOptionsAreEmptyWhenNoProviderIsActiveOrConfigured() {
        List<Property> properties = Arrays.stream(Provider.values())
            .map(provider -> createProperty(provider.getKey(), false))
            .toList();

        when(propertyService.getProperties(anyList(), eq(Scope.PLATFORM), isNull(), eq(ENVIRONMENT_ID)))
            .thenReturn(properties);

        List<? extends Option<String>> providerOptions = AiImageUtils.getProviderOptions(
            new Ai.Provider(), propertyService, ENVIRONMENT_ID);

        assertEquals(List.of(), getValues(providerOptions));
    }

    private static Ai.Provider createAiProviderWithEveryApiKey() {
        Ai.Provider aiProvider = new Ai.Provider();

        aiProvider.getAnthropic()
            .setApiKey("anthropic-key");
        aiProvider.getAzureOpenAi()
            .setApiKey("azure-open-ai-key");
        aiProvider.getDeepSeek()
            .setApiKey("deepseek-key");
        aiProvider.getGroq()
            .setApiKey("groq-key");
        aiProvider.getMistral()
            .setApiKey("mistral-key");
        aiProvider.getNvidia()
            .setApiKey("nvidia-key");
        aiProvider.getOllama()
            .setApiKey("ollama-key");
        aiProvider.getOpenAi()
            .setApiKey("open-ai-key");
        aiProvider.getPerplexity()
            .setApiKey("perplexity-key");
        aiProvider.getStability()
            .setApiKey("stability-key");
        aiProvider.getVertexGemini()
            .setApiKey("vertex-gemini-key");

        return aiProvider;
    }

    private static Property createProperty(String key, boolean enabled) {
        Property property = new Property();

        property.setEnabled(enabled);
        property.setKey(key);
        property.setValue(Map.of("apiKey", key + "-property-key"));

        return property;
    }

    private static List<String> getValues(List<? extends Option<String>> options) {
        return options.stream()
            .map(Option::getValue)
            .toList();
    }
}
