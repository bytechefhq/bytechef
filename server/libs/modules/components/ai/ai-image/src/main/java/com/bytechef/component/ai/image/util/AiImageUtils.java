/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.ai.image.util;

import static com.bytechef.component.ai.image.constant.AiImageConstants.PROVIDER;
import static com.bytechef.component.ai.llm.constant.Provider.AZURE_OPEN_AI;
import static com.bytechef.component.ai.llm.constant.Provider.OPEN_AI;
import static com.bytechef.component.ai.llm.constant.Provider.STABILITY;
import static com.bytechef.component.definition.ComponentDsl.option;

import com.bytechef.component.ai.llm.azure.openai.constant.AzureOpenAiConstants;
import com.bytechef.component.ai.llm.constant.LLMConstants;
import com.bytechef.component.ai.llm.constant.Provider;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.openai.constant.OpenAiConstants;
import com.bytechef.config.ApplicationProperties.Ai;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.service.PropertyService;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * @author Marko Kriskovic
 */
public class AiImageUtils {

    private AiImageUtils() {
    }

    public static List<? extends Option<String>> getModelOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) {

        Provider provider = Provider.valueOf(inputParameters.getRequiredString(PROVIDER));

        return switch (provider) {
            case AZURE_OPEN_AI -> AzureOpenAiConstants.IMAGE_MODELS;
            case OPEN_AI -> OpenAiConstants.IMAGE_MODELS;
            default -> throw new IllegalStateException("Unexpected value: " + provider);
        };
    }

    public static List<? extends Option<String>> getProviderOptions(
        Ai.Provider aiProvider, PropertyService propertyService) {

        List<String> activeProviderKeys = propertyService.getProperties(
            LLMConstants.PROVIDERS.stream()
                .map(Provider::getKey)
                .toList())
            .stream()
            .filter(property -> property.getValue() != null && property.isEnabled())
            .map(Property::getKey)
            .toList();

        return LLMConstants.PROVIDERS.stream()
            .filter(filter(aiProvider, activeProviderKeys))
            .map(provider -> option(provider.getLabel(), String.valueOf(provider)))
            .toList();
    }

    private static boolean checkAiProvider(String key, List<String> activeProviderKeys) {
        return activeProviderKeys.stream()
            .anyMatch(key::equals);
    }

    private static Predicate<Provider> filter(Ai.Provider aiProvider, List<String> activeProviderKeys) {
        return provider -> switch (provider) {
            case AZURE_OPEN_AI -> {
                if (checkAiProvider(AZURE_OPEN_AI.getKey(), activeProviderKeys)) {
                    yield true;
                }

                Ai.Provider.AzureOpenAi azureOpenAi = aiProvider.getAzureOpenAi();

                yield azureOpenAi.getApiKey() != null;
            }
            case OPEN_AI -> {
                if (checkAiProvider(OPEN_AI.getKey(), activeProviderKeys)) {
                    yield true;
                }

                Ai.Provider.OpenAi openAi = aiProvider.getOpenAi();

                yield openAi.getApiKey() != null;
            }
            case STABILITY -> {
                if (checkAiProvider(STABILITY.getKey(), activeProviderKeys)) {
                    yield true;
                }

                Ai.Provider.Stability stability = aiProvider.getStability();

                yield stability.getApiKey() != null;
            }
            default -> false;
        };
    }
}
