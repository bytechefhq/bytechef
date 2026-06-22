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

package com.bytechef.component.ai.universal.text.util;

import static com.bytechef.component.ai.llm.constant.LLMConstants.PROVIDER;
import static com.bytechef.component.definition.ComponentDsl.option;

import com.bytechef.component.ai.llm.LLMModelRegistry;
import com.bytechef.component.ai.llm.anthropic.constant.AnthropicConstants;
import com.bytechef.component.ai.llm.gemini.constant.GeminiConstants;
import com.bytechef.component.ai.llm.mistral.constant.MistralConstants;
import com.bytechef.component.ai.llm.openai.constant.OpenAiConstants;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.config.ApplicationProperties.Ai;
import com.bytechef.platform.ai.llm.Provider;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.domain.Property.Scope;
import com.bytechef.platform.configuration.service.PropertyService;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author Marko Kriskovic
 */
public class AiTextUtils {

    private AiTextUtils() {
    }

    public static List<? extends Option<String>> getModelOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, ActionContext context) {

        if (!inputParameters.containsKey(PROVIDER)) {
            return List.of();
        }

        Provider provider = Provider.valueOf(inputParameters.getRequiredString(PROVIDER));

        return switch (provider) {
            case ANTHROPIC -> AnthropicConstants.MODELS;
            case MISTRAL -> MistralConstants.CHAT_MODELS;
            case OPEN_AI -> OpenAiConstants.CHAT_MODELS;
            case VERTEX_GEMINI -> GeminiConstants.MODELS;
            default -> throw new IllegalStateException("Unexpected value: " + provider);
        };
    }

    public static List<? extends Option<String>> getProviderOptions(
        Ai.Provider aiProvider, PropertyService propertyService, Long environmentId) {

        List<String> activeProviderKeys = propertyService.getProperties(
            Arrays.stream(Provider.values())
                .map(Provider::getKey)
                .toList(),
            Scope.PLATFORM, null, environmentId)
            .stream()
            .filter(property -> property.getValue() != null && property.isEnabled())
            .map(Property::getKey)
            .toList();

        return Arrays.stream(Provider.values())
            .filter(provider -> isSelectable(provider, aiProvider, activeProviderKeys))
            .map(provider -> option(provider.getLabel(), String.valueOf(provider)))
            .toList();
    }

    static boolean isSelectable(Provider provider, Ai.Provider aiProvider, List<String> activeProviderKeys) {
        if (!LLMModelRegistry.hasChatModel(provider)) {
            return false;
        }

        return activeProviderKeys.contains(provider.getKey()) ||
            aiProvider.getProviderApiKey(provider.getKey()) != null;
    }
}
