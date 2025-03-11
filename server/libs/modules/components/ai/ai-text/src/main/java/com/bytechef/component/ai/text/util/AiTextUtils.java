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

package com.bytechef.component.ai.text.util;

import static com.bytechef.component.ai.llm.Provider.AMAZON_BEDROCK_ANTHROPIC2;
import static com.bytechef.component.ai.llm.Provider.AMAZON_BEDROCK_ANTHROPIC3;
import static com.bytechef.component.ai.llm.Provider.AMAZON_BEDROCK_COHERE;
import static com.bytechef.component.ai.llm.Provider.AMAZON_BEDROCK_JURASSIC2;
import static com.bytechef.component.ai.llm.Provider.AMAZON_BEDROCK_LLAMA;
import static com.bytechef.component.ai.llm.Provider.AMAZON_BEDROCK_TITAN;
import static com.bytechef.component.ai.llm.Provider.ANTHROPIC;
import static com.bytechef.component.ai.llm.Provider.AZURE_OPEN_AI;
import static com.bytechef.component.ai.llm.Provider.GROQ;
import static com.bytechef.component.ai.llm.Provider.HUGGING_FACE;
import static com.bytechef.component.ai.llm.Provider.MISTRAL;
import static com.bytechef.component.ai.llm.Provider.NVIDIA;
import static com.bytechef.component.ai.llm.Provider.OPEN_AI;
import static com.bytechef.component.ai.llm.Provider.VERTEX_GEMINI;
import static com.bytechef.component.ai.llm.constant.LLMConstants.PROVIDER;
import static com.bytechef.component.definition.ComponentDsl.option;

import com.bytechef.component.ai.llm.Provider;
import com.bytechef.component.ai.llm.anthropic.constant.AnthropicConstants;
import com.bytechef.component.ai.llm.azure.openai.constant.AzureOpenAiConstants;
import com.bytechef.component.ai.llm.mistral.constant.MistralConstants;
import com.bytechef.component.ai.llm.vertex.gemini.constant.VertexGeminiConstants;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.openai.constant.OpenAiConstants;
import com.bytechef.config.ApplicationProperties.Ai;
import com.bytechef.config.ApplicationProperties.Ai.Provider.AmazonBedrockAnthropic2;
import com.bytechef.config.ApplicationProperties.Ai.Provider.AmazonBedrockAnthropic3;
import com.bytechef.config.ApplicationProperties.Ai.Provider.AmazonBedrockCohere;
import com.bytechef.config.ApplicationProperties.Ai.Provider.AmazonBedrockJurassic2;
import com.bytechef.config.ApplicationProperties.Ai.Provider.AmazonBedrockLlama;
import com.bytechef.config.ApplicationProperties.Ai.Provider.AmazonBedrockTitan;
import com.bytechef.config.ApplicationProperties.Ai.Provider.Anthropic;
import com.bytechef.config.ApplicationProperties.Ai.Provider.VertexGemini;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.domain.Property.Scope;
import com.bytechef.platform.configuration.service.PropertyService;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * @author Marko Kriskovic
 */
public class AiTextUtils {

    private AiTextUtils() {
    }

    public static List<? extends Option<String>> getModelOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) {

        Provider provider = Provider.valueOf(inputParameters.getRequiredString(PROVIDER));

        return switch (provider) {
//            case AMAZON_BEDROCK_ANTHROPIC2 -> AmazonBedrockConstants.ANTHROPIC2_MODELS;
//            case AMAZON_BEDROCK_ANTHROPIC3 -> AmazonBedrockConstants.ANTHROPIC3_MODELS;
//            case AMAZON_BEDROCK_COHERE -> AmazonBedrockConstants.COHERE_MODELS;
//            case AMAZON_BEDROCK_JURASSIC2 -> AmazonBedrockConstants.JURASSIC2_MODELS;
//            case AMAZON_BEDROCK_LLAMA -> AmazonBedrockConstants.LLAMA_MODELS;
//            case AMAZON_BEDROCK_TITAN -> AmazonBedrockConstants.TITAN_MODELS;
            case ANTHROPIC -> AnthropicConstants.MODELS;
            case AZURE_OPEN_AI -> AzureOpenAiConstants.IMAGE_MODELS;
            case MISTRAL -> MistralConstants.MODELS;
            case OPEN_AI -> OpenAiConstants.TEXT_MODELS;
            case VERTEX_GEMINI -> VertexGeminiConstants.MODELS;
            default -> throw new IllegalStateException("Unexpected value: " + provider);
        };
    }

    public static List<? extends Option<String>> getProviderOptions(
        Ai.Provider aiProvider, PropertyService propertyService) {

        List<String> activeProviderKeys = propertyService.getProperties(
            Arrays.stream(Provider.values())
                .map(Provider::getKey)
                .toList(),
            Scope.PLATFORM, null)
            .stream()
            .filter(property -> property.getValue() != null && property.isEnabled())
            .map(Property::getKey)
            .toList();

        return Arrays.stream(Provider.values())
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
            case AMAZON_BEDROCK_ANTHROPIC2 -> {
                if (checkAiProvider(AMAZON_BEDROCK_ANTHROPIC2.getKey(), activeProviderKeys)) {
                    yield true;
                }

                AmazonBedrockAnthropic2 amazonBedrockAnthropic2 = aiProvider.getAmazonBedrockAnthropic2();

                yield amazonBedrockAnthropic2.getApiKey() != null;
            }
            case AMAZON_BEDROCK_ANTHROPIC3 -> {
                if (checkAiProvider(AMAZON_BEDROCK_ANTHROPIC3.getKey(), activeProviderKeys)) {
                    yield true;
                }

                AmazonBedrockAnthropic3 amazonBedrockAnthropic3 = aiProvider.getAmazonBedrockAnthropic3();

                yield amazonBedrockAnthropic3.getApiKey() != null;
            }
            case AMAZON_BEDROCK_COHERE -> {
                if (checkAiProvider(AMAZON_BEDROCK_COHERE.getKey(), activeProviderKeys)) {
                    yield true;
                }

                AmazonBedrockCohere amazonBedrockCohere = aiProvider.getAmazonBedrockCohere();

                yield amazonBedrockCohere.getApiKey() != null;
            }
            case AMAZON_BEDROCK_JURASSIC2 -> {
                if (checkAiProvider(AMAZON_BEDROCK_JURASSIC2.getKey(), activeProviderKeys)) {
                    yield true;
                }

                AmazonBedrockJurassic2 amazonBedrockJurassic2 = aiProvider.getAmazonBedrockJurassic2();

                yield amazonBedrockJurassic2.getApiKey() != null;
            }
            case AMAZON_BEDROCK_LLAMA -> {
                if (checkAiProvider(AMAZON_BEDROCK_LLAMA.getKey(), activeProviderKeys)) {
                    yield true;
                }

                AmazonBedrockLlama amazonBedrockLlama = aiProvider.getAmazonBedrockLlama();

                yield amazonBedrockLlama.getApiKey() != null;
            }
            case AMAZON_BEDROCK_TITAN -> {
                if (checkAiProvider(AMAZON_BEDROCK_TITAN.getKey(), activeProviderKeys)) {
                    yield true;
                }

                AmazonBedrockTitan amazonBedrockTitan = aiProvider.getAmazonBedrockTitan();

                yield amazonBedrockTitan.getApiKey() != null;
            }
            case ANTHROPIC -> {
                if (checkAiProvider(ANTHROPIC.getKey(), activeProviderKeys)) {
                    yield true;
                }

                Anthropic anthropic = aiProvider.getAnthropic();

                yield anthropic.getApiKey() != null;
            }
            case AZURE_OPEN_AI -> {
                if (checkAiProvider(AZURE_OPEN_AI.getKey(), activeProviderKeys)) {
                    yield true;
                }

                Ai.Provider.AzureOpenAi azureOpenAi = aiProvider.getAzureOpenAi();

                yield azureOpenAi.getApiKey() != null;
            }
            case HUGGING_FACE -> {
                if (checkAiProvider(HUGGING_FACE.getKey(), activeProviderKeys)) {
                    yield true;
                }

                Ai.Provider.HuggingFace huggingFace = aiProvider.getHuggingFace();

                yield huggingFace.getApiKey() != null;
            }
            case GROQ -> {
                if (checkAiProvider(GROQ.getKey(), activeProviderKeys)) {
                    yield true;
                }

                Ai.Provider.Groq groq = aiProvider.getGroq();

                yield groq.getApiKey() != null;
            }
            case MISTRAL -> {
                if (checkAiProvider(MISTRAL.getKey(), activeProviderKeys)) {
                    yield true;
                }

                Ai.Provider.Mistral mistral = aiProvider.getMistral();

                yield mistral.getApiKey() != null;
            }
            case NVIDIA -> {
                if (checkAiProvider(NVIDIA.getKey(), activeProviderKeys)) {
                    yield true;
                }

                Ai.Provider.Nvidia nvidia = aiProvider.getNvidia();

                yield nvidia.getApiKey() != null;
            }
            case OPEN_AI -> {
                if (checkAiProvider(OPEN_AI.getKey(), activeProviderKeys)) {
                    yield true;
                }

                Ai.Provider.OpenAi openAi = aiProvider.getOpenAi();

                yield openAi.getApiKey() != null;
            }
            case VERTEX_GEMINI -> {
                if (checkAiProvider(VERTEX_GEMINI.getKey(), activeProviderKeys)) {
                    yield true;
                }

                VertexGemini vertexGemini = aiProvider.getVertexGemini();

                yield vertexGemini.getApiKey() != null;
            }
            default -> false;
        };
    }

    public record Criteria(String criterion, double lowestScore, double highestScore, boolean isDecimal) {
    }
}
