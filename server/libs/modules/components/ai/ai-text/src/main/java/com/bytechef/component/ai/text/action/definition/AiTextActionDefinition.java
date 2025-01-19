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

package com.bytechef.component.ai.text.action.definition;

import static com.bytechef.component.ai.llm.constant.Provider.AMAZON_BEDROCK_ANTHROPIC2;
import static com.bytechef.component.ai.llm.constant.Provider.AMAZON_BEDROCK_ANTHROPIC3;
import static com.bytechef.component.ai.llm.constant.Provider.AMAZON_BEDROCK_COHERE;
import static com.bytechef.component.ai.llm.constant.Provider.AMAZON_BEDROCK_JURASSIC2;
import static com.bytechef.component.ai.llm.constant.Provider.AMAZON_BEDROCK_LLAMA;
import static com.bytechef.component.ai.llm.constant.Provider.AMAZON_BEDROCK_TITAN;
import static com.bytechef.component.ai.llm.constant.Provider.ANTHROPIC;
import static com.bytechef.component.ai.llm.constant.Provider.AZURE_OPEN_AI;
import static com.bytechef.component.ai.llm.constant.Provider.GROQ;
import static com.bytechef.component.ai.llm.constant.Provider.HUGGING_FACE;
import static com.bytechef.component.ai.llm.constant.Provider.MISTRAL;
import static com.bytechef.component.ai.llm.constant.Provider.NVIDIA;
import static com.bytechef.component.ai.llm.constant.Provider.OPEN_AI;
import static com.bytechef.component.ai.llm.constant.Provider.VERTEX_GEMINI;
import static com.bytechef.component.ai.text.constant.AiTextConstants.PROVIDER;
import static com.bytechef.component.definition.Authorization.TOKEN;

import com.bytechef.component.ai.llm.ChatModel;
import com.bytechef.component.ai.llm.amazon.bedrock.action.AmazonBedrockAnthropic2ChatAction;
import com.bytechef.component.ai.llm.amazon.bedrock.action.AmazonBedrockAnthropic3ChatAction;
import com.bytechef.component.ai.llm.amazon.bedrock.action.AmazonBedrockCohereChatAction;
import com.bytechef.component.ai.llm.amazon.bedrock.action.AmazonBedrockJurassic2ChatAction;
import com.bytechef.component.ai.llm.amazon.bedrock.action.AmazonBedrockLlamaChatAction;
import com.bytechef.component.ai.llm.amazon.bedrock.action.AmazonBedrockTitanChatAction;
import com.bytechef.component.ai.llm.anthropic.action.AnthropicChatAction;
import com.bytechef.component.ai.llm.azure.openai.action.AzureOpenAiChatAction;
import com.bytechef.component.ai.llm.constant.LLMConstants;
import com.bytechef.component.ai.llm.constant.Provider;
import com.bytechef.component.ai.llm.groq.action.GroqChatAction;
import com.bytechef.component.ai.llm.hugging.face.action.HuggingFaceChatAction;
import com.bytechef.component.ai.llm.mistral.action.MistralChatAction;
import com.bytechef.component.ai.llm.nvidia.action.NvidiaChatAction;
import com.bytechef.component.ai.llm.vertex.gemini.action.VertexGeminiChatAction;
import com.bytechef.component.ai.text.action.AiTextAction;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.openai.action.OpenAiChatAction;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.Ai.Provider.AmazonBedrockAnthropic2;
import com.bytechef.config.ApplicationProperties.Ai.Provider.AmazonBedrockAnthropic3;
import com.bytechef.config.ApplicationProperties.Ai.Provider.AmazonBedrockCohere;
import com.bytechef.config.ApplicationProperties.Ai.Provider.AmazonBedrockJurassic2;
import com.bytechef.config.ApplicationProperties.Ai.Provider.AmazonBedrockLlama;
import com.bytechef.config.ApplicationProperties.Ai.Provider.AmazonBedrockTitan;
import com.bytechef.config.ApplicationProperties.Ai.Provider.Anthropic;
import com.bytechef.config.ApplicationProperties.Ai.Provider.AzureOpenAi;
import com.bytechef.config.ApplicationProperties.Ai.Provider.Groq;
import com.bytechef.config.ApplicationProperties.Ai.Provider.HuggingFace;
import com.bytechef.config.ApplicationProperties.Ai.Provider.Mistral;
import com.bytechef.config.ApplicationProperties.Ai.Provider.Nvidia;
import com.bytechef.config.ApplicationProperties.Ai.Provider.OpenAi;
import com.bytechef.config.ApplicationProperties.Ai.Provider.VertexGemini;
import com.bytechef.platform.component.definition.AbstractActionDefinitionWrapper;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.service.PropertyService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Marko Kriskovic
 */
public class AiTextActionDefinition extends AbstractActionDefinitionWrapper {

    private final ApplicationProperties.Ai.Provider aiProvider;
    private final AiTextAction aiTextAction;
    private final PropertyService propertyService;

    @SuppressFBWarnings("EI")
    public AiTextActionDefinition(
        ActionDefinition actionDefinition, ApplicationProperties.Ai.Provider aiProvider, AiTextAction aiTextAction,
        PropertyService propertyService) {

        super(actionDefinition);

        this.aiProvider = aiProvider;
        this.aiTextAction = aiTextAction;
        this.propertyService = propertyService;
    }

    @Override
    public Optional<PerformFunction> getPerform() {
        return Optional.of((SingleConnectionPerformFunction) this::perform);
    }

    protected String perform(
        Parameters inputParameters, Parameters connectionParameter, ActionContext context) {

        Map<String, String> modelConnectionParametersMap = new HashMap<>();

        List<String> activeProviderKeys = propertyService.getProperties(
            LLMConstants.PROVIDERS.stream()
                .map(Provider::getKey)
                .toList())
            .stream()
            .filter(property -> property.getValue() != null && property.isEnabled())
            .map(Property::getKey)
            .toList();

        ChatModel chatModel = getChatModel(inputParameters, activeProviderKeys, modelConnectionParametersMap);

        Parameters modelConnectionParameters = ParametersFactory.createParameters(modelConnectionParametersMap);

        Parameters modelInputParameters = aiTextAction.createParameters(inputParameters);

        Object response = chatModel.getResponse(modelInputParameters, modelConnectionParameters, context);

        return response.toString();
    }

    private String getAiProviderToken(String key, List<String> activeProviderKeys) {
        return activeProviderKeys.stream()
            .filter(key::equals)
            .findFirst()
            .map(propertyService::getProperty)
            .map(property -> (String) property.get("apiKey"))
            .orElse(null);
    }

    private ChatModel getChatModel(
        Parameters inputParameters, List<String> activeProviderKeys, Map<String, String> modelConnectionParametersMap) {

        return switch (Provider.valueOf(inputParameters.getRequiredString(PROVIDER))) {
            case AMAZON_BEDROCK_ANTHROPIC2 -> getAmazonBedrockAnthropic2ChatModel(
                activeProviderKeys, modelConnectionParametersMap);
            case AMAZON_BEDROCK_ANTHROPIC3 -> getAmazonBedrockAnthropic3ChatModel(
                activeProviderKeys, modelConnectionParametersMap);
            case AMAZON_BEDROCK_COHERE -> getAmazonBedrockCohereChatModel(
                activeProviderKeys, modelConnectionParametersMap);
            case AMAZON_BEDROCK_JURASSIC2 -> getAmazonBedrockJurassic2ChatModel(
                activeProviderKeys, modelConnectionParametersMap);
            case AMAZON_BEDROCK_LLAMA -> getAmazonBedrockLlamaChatModel(
                activeProviderKeys, modelConnectionParametersMap);
            case AMAZON_BEDROCK_TITAN -> getAmazonBedrockTitanChatModel(
                activeProviderKeys, modelConnectionParametersMap);
            case ANTHROPIC -> getAnthropicChatModel(activeProviderKeys, modelConnectionParametersMap);
            case AZURE_OPEN_AI -> getAzureOpenAiChatModel(activeProviderKeys, modelConnectionParametersMap);
            case GROQ -> getGroqChatModel(activeProviderKeys, modelConnectionParametersMap);
            case HUGGING_FACE -> getHuggingFaceChatModel(activeProviderKeys, modelConnectionParametersMap);
            case MISTRAL -> getMistralChatModel(activeProviderKeys, modelConnectionParametersMap);
            case NVIDIA -> getNvidiaChatModel(activeProviderKeys, modelConnectionParametersMap);
            case OPEN_AI -> getOpenAiChatModel(activeProviderKeys, modelConnectionParametersMap);
            case VERTEX_GEMINI -> getVertexGeminiChatModel(activeProviderKeys, modelConnectionParametersMap);
        };
    }

    private ChatModel getAmazonBedrockAnthropic2ChatModel(
        List<String> activeProviderKeys, Map<String, String> modelConnectionParametersMap) {

        String token = getAiProviderToken(AMAZON_BEDROCK_ANTHROPIC2.getKey(), activeProviderKeys);

        if (token == null) {
            AmazonBedrockAnthropic2 amazonBedrockAnthropic2 = aiProvider.getAmazonBedrockAnthropic2();

            token = amazonBedrockAnthropic2.getApiKey();
        }

        modelConnectionParametersMap.put(TOKEN, token);

        return AmazonBedrockAnthropic2ChatAction.CHAT_MODEL;
    }

    private ChatModel getAmazonBedrockAnthropic3ChatModel(
        List<String> activeProviderKeys, Map<String, String> modelConnectionParametersMap) {

        String token = getAiProviderToken(AMAZON_BEDROCK_ANTHROPIC3.getKey(), activeProviderKeys);

        if (token == null) {
            AmazonBedrockAnthropic3 amazonBedrockAnthropic3 = aiProvider.getAmazonBedrockAnthropic3();

            token = amazonBedrockAnthropic3.getApiKey();
        }

        modelConnectionParametersMap.put(TOKEN, token);

        return AmazonBedrockAnthropic3ChatAction.CHAT_MODEL;
    }

    private ChatModel getAmazonBedrockCohereChatModel(
        List<String> activeProviderKeys, Map<String, String> modelConnectionParametersMap) {

        String token = getAiProviderToken(AMAZON_BEDROCK_COHERE.getKey(), activeProviderKeys);

        if (token == null) {
            AmazonBedrockCohere amazonBedrockCohere = aiProvider.getAmazonBedrockCohere();

            token = amazonBedrockCohere.getApiKey();
        }

        modelConnectionParametersMap.put(TOKEN, token);

        return AmazonBedrockCohereChatAction.CHAT_MODEL;
    }

    private ChatModel getAmazonBedrockJurassic2ChatModel(
        List<String> activeProviderKeys, Map<String, String> modelConnectionParametersMap) {

        String token = getAiProviderToken(AMAZON_BEDROCK_JURASSIC2.getKey(), activeProviderKeys);

        if (token == null) {
            AmazonBedrockJurassic2 amazonBedrockJurassic2 = aiProvider.getAmazonBedrockJurassic2();

            token = amazonBedrockJurassic2.getApiKey();
        }

        modelConnectionParametersMap.put(TOKEN, token);

        return AmazonBedrockJurassic2ChatAction.CHAT_MODEL;
    }

    private ChatModel getAmazonBedrockLlamaChatModel(
        List<String> activeProviderKeys, Map<String, String> modelConnectionParametersMap) {

        String token = getAiProviderToken(AMAZON_BEDROCK_LLAMA.getKey(), activeProviderKeys);

        if (token == null) {
            AmazonBedrockLlama amazonBedrockLlama = aiProvider.getAmazonBedrockLlama();

            token = amazonBedrockLlama.getApiKey();
        }

        modelConnectionParametersMap.put(TOKEN, token);

        return AmazonBedrockLlamaChatAction.CHAT_MODEL;
    }

    private ChatModel getAmazonBedrockTitanChatModel(
        List<String> activeProviderKeys, Map<String, String> modelConnectionParametersMap) {

        String token = getAiProviderToken(AMAZON_BEDROCK_TITAN.getKey(), activeProviderKeys);

        if (token == null) {
            AmazonBedrockTitan amazonBedrockTitan = aiProvider.getAmazonBedrockTitan();

            token = amazonBedrockTitan.getApiKey();
        }

        modelConnectionParametersMap.put(TOKEN, token);

        return AmazonBedrockTitanChatAction.CHAT_MODEL;
    }

    private ChatModel getAnthropicChatModel(
        List<String> activeProviderKeys, Map<String, String> modelConnectionParametersMap) {

        String token = getAiProviderToken(ANTHROPIC.getKey(), activeProviderKeys);

        if (token == null) {
            Anthropic anthropic = aiProvider.getAnthropic();

            token = anthropic.getApiKey();
        }

        modelConnectionParametersMap.put(TOKEN, token);

        return AnthropicChatAction.CHAT_MODEL;
    }

    private ChatModel getAzureOpenAiChatModel(
        List<String> activeProviderKeys, Map<String, String> modelConnectionParametersMap) {

        String token = getAiProviderToken(AZURE_OPEN_AI.getKey(), activeProviderKeys);

        if (token == null) {
            AzureOpenAi azureOpenAi = aiProvider.getAzureOpenAi();

            token = azureOpenAi.getApiKey();
        }

        modelConnectionParametersMap.put(TOKEN, token);

        return AzureOpenAiChatAction.CHAT_MODEL;
    }

    private ChatModel getGroqChatModel(
        List<String> activeProviderKeys, Map<String, String> modelConnectionParametersMap) {

        String token = getAiProviderToken(GROQ.getKey(), activeProviderKeys);

        if (token == null) {
            Groq groq = aiProvider.getGroq();

            token = groq.getApiKey();
        }

        modelConnectionParametersMap.put(TOKEN, token);

        return GroqChatAction.CHAT_MODEL;
    }

    private ChatModel getHuggingFaceChatModel(
        List<String> activeProviderKeys, Map<String, String> modelConnectionParametersMap) {

        String token = getAiProviderToken(HUGGING_FACE.getKey(), activeProviderKeys);

        if (token == null) {
            HuggingFace huggingFace = aiProvider.getHuggingFace();

            token = huggingFace.getApiKey();
        }

        modelConnectionParametersMap.put(TOKEN, token);

        return HuggingFaceChatAction.CHAT_MODEL;
    }

    private ChatModel getMistralChatModel(
        List<String> activeProviderKeys, Map<String, String> modelConnectionParametersMap) {

        String token = getAiProviderToken(MISTRAL.getKey(), activeProviderKeys);

        if (token == null) {
            Mistral mistral = aiProvider.getMistral();

            token = mistral.getApiKey();
        }

        modelConnectionParametersMap.put(TOKEN, token);

        return MistralChatAction.CHAT_MODEL;
    }

    private ChatModel getNvidiaChatModel(
        List<String> activeProviderKeys, Map<String, String> modelConnectionParametersMap) {

        String token = getAiProviderToken(NVIDIA.getKey(), activeProviderKeys);

        if (token == null) {
            Nvidia nvidia = aiProvider.getNvidia();

            token = nvidia.getApiKey();
        }

        modelConnectionParametersMap.put(TOKEN, token);

        return NvidiaChatAction.CHAT_MODEL;
    }

    private ChatModel getOpenAiChatModel(
        List<String> activeProviderKeys, Map<String, String> modelConnectionParametersMap) {

        String token = getAiProviderToken(OPEN_AI.getKey(), activeProviderKeys);

        if (token == null) {
            OpenAi openAi = aiProvider.getOpenAi();

            token = openAi.getApiKey();
        }

        modelConnectionParametersMap.put(TOKEN, token);

        return OpenAiChatAction.CHAT_MODEL;
    }

    private ChatModel getVertexGeminiChatModel(
        List<String> activeProviderKeys, Map<String, String> modelConnectionParametersMap) {

        String token = getAiProviderToken(VERTEX_GEMINI.getKey(), activeProviderKeys);

        if (token == null) {
            VertexGemini vertexGemini = aiProvider.getVertexGemini();

            token = vertexGemini.getApiKey();
        }

        modelConnectionParametersMap.put(TOKEN, token);

        return VertexGeminiChatAction.CHAT_MODEL;
    }
}
