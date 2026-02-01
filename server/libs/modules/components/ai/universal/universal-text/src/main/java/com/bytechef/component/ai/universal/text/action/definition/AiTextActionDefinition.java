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

package com.bytechef.component.ai.universal.text.action.definition;

import static com.bytechef.component.ai.llm.Provider.ANTHROPIC;
import static com.bytechef.component.ai.llm.Provider.AZURE_OPEN_AI;
import static com.bytechef.component.ai.llm.Provider.DEEPSEEK;
import static com.bytechef.component.ai.llm.Provider.GROQ;
import static com.bytechef.component.ai.llm.Provider.HUGGING_FACE;
import static com.bytechef.component.ai.llm.Provider.MISTRAL;
import static com.bytechef.component.ai.llm.Provider.NVIDIA;
import static com.bytechef.component.ai.llm.Provider.OPEN_AI;
import static com.bytechef.component.ai.llm.Provider.PERPLEXITY;
import static com.bytechef.component.ai.llm.Provider.VERTEX_GEMINI;
import static com.bytechef.component.ai.llm.constant.LLMConstants.PROVIDER;
import static com.bytechef.component.definition.Authorization.TOKEN;

import com.bytechef.component.ai.llm.ChatModel;
import com.bytechef.component.ai.llm.Provider;
import com.bytechef.component.ai.llm.anthropic.action.AnthropicChatAction;
import com.bytechef.component.ai.llm.azure.openai.action.AzureOpenAiChatAction;
import com.bytechef.component.ai.llm.deepseek.action.DeepSeekChatAction;
import com.bytechef.component.ai.llm.hugging.face.action.HuggingFaceChatAction;
import com.bytechef.component.ai.llm.mistral.action.MistralChatAction;
import com.bytechef.component.ai.llm.nvidia.action.NvidiaChatAction;
import com.bytechef.component.ai.llm.openai.action.OpenAiChatAction;
import com.bytechef.component.ai.llm.perplexity.action.PerplexityChatAction;
import com.bytechef.component.ai.llm.vertex.gemini.action.VertexGeminiChatAction;
import com.bytechef.component.ai.universal.text.action.AiTextAction;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.Ai.Provider.Anthropic;
import com.bytechef.config.ApplicationProperties.Ai.Provider.AzureOpenAi;
import com.bytechef.config.ApplicationProperties.Ai.Provider.DeepSeek;
import com.bytechef.config.ApplicationProperties.Ai.Provider.Groq;
import com.bytechef.config.ApplicationProperties.Ai.Provider.HuggingFace;
import com.bytechef.config.ApplicationProperties.Ai.Provider.Mistral;
import com.bytechef.config.ApplicationProperties.Ai.Provider.Nvidia;
import com.bytechef.config.ApplicationProperties.Ai.Provider.OpenAi;
import com.bytechef.config.ApplicationProperties.Ai.Provider.Perplexity;
import com.bytechef.config.ApplicationProperties.Ai.Provider.VertexGemini;
import com.bytechef.platform.component.definition.AbstractActionDefinitionWrapper;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.domain.Property.Scope;
import com.bytechef.platform.configuration.service.PropertyService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
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
    public Optional<BasePerformFunction> getPerform() {
        return Optional.of((PerformFunction) this::perform);
    }

    protected Object perform(Parameters inputParameters, Parameters connectionParameter, ActionContext context) {
        Map<String, String> modelConnectionParametersMap = new HashMap<>();

        List<String> providers = Arrays.stream(Provider.values())
            .map(Provider::getKey)
            .toList();

        List<String> activeProviderKeys = propertyService.getProperties(providers, Scope.PLATFORM, null)
            .stream()
            .filter(property -> property.getValue() != null && property.isEnabled())
            .map(Property::getKey)
            .toList();

        ChatModelResult chatModelResult = getChatModel(inputParameters, activeProviderKeys);
        Parameters modelInputParameters = aiTextAction.createParameters(inputParameters);

        modelConnectionParametersMap.put(TOKEN, chatModelResult.token);

        return chatModelResult.chatModel.getResponse(
            modelInputParameters, ParametersFactory.create(modelConnectionParametersMap), context, false,
            modelInputParameters.containsPath("response.responseFormat"));
    }

    private String getAiProviderToken(String key, List<String> activeProviderKeys) {
        return activeProviderKeys.stream()
            .filter(key::equals)
            .findFirst()
            .map(curKey -> propertyService.getProperty(curKey, Scope.PLATFORM, null))
            .map(property -> (String) property.get("apiKey"))
            .orElse(null);
    }

    private ChatModelResult getChatModel(Parameters inputParameters, List<String> activeProviderKeys) {
        return switch (Provider.valueOf(inputParameters.getRequiredString(PROVIDER))) {
//            case AMAZON_BEDROCK_ANTHROPIC2 -> getAmazonBedrockAnthropic2ChatModel(activeProviderKeys);
//            case AMAZON_BEDROCK_ANTHROPIC3 -> getAmazonBedrockAnthropic3ChatModel(activeProviderKeys);
//            case AMAZON_BEDROCK_COHERE -> getAmazonBedrockCohereChatModel(activeProviderKeys);
//            case AMAZON_BEDROCK_JURASSIC2 -> getAmazonBedrockJurassic2ChatModel(activeProviderKeys);
//            case AMAZON_BEDROCK_LLAMA -> getAmazonBedrockLlamaChatModel(activeProviderKeys);
//            case AMAZON_BEDROCK_TITAN -> getAmazonBedrockTitanChatModel(activeProviderKeys);
            case ANTHROPIC -> getAnthropicChatModel(activeProviderKeys);
            case AZURE_OPEN_AI -> getAzureOpenAiChatModel(activeProviderKeys);
            case DEEPSEEK -> getDeepSeekModel(activeProviderKeys);
            case GROQ -> getGroqChatModel(activeProviderKeys);
            case HUGGING_FACE -> getHuggingFaceChatModel(activeProviderKeys);
            case MISTRAL -> getMistralChatModel(activeProviderKeys);
            case NVIDIA -> getNvidiaChatModel(activeProviderKeys);
            case OPEN_AI -> getOpenAiChatModel(activeProviderKeys);
            case PERPLEXITY -> getPerplexityChatModel(activeProviderKeys);
            case VERTEX_GEMINI -> getVertexGeminiChatModel(activeProviderKeys);
            default -> throw new IllegalArgumentException("Invalid provider");
        };
    }

//    private ChatModelResult getAmazonBedrockAnthropic2ChatModel(
//        List<String> activeProviderKeys, Map<String, String> modelConnectionParametersMap) {
//
//        String token = getAiProviderToken(AMAZON_BEDROCK_ANTHROPIC2.getKey(), activeProviderKeys);
//
//        if (token == null) {
//            AmazonBedrockAnthropic2 amazonBedrockAnthropic2 = aiProvider.getAmazonBedrockAnthropic2();
//
//            token = amazonBedrockAnthropic2.getApiKey();
//        }
//
//        modelConnectionParametersMap.put(TOKEN, token);
//
//        return AmazonBedrockAnthropic2ChatAction.CHAT_MODEL;
//    }
//
//    private ChatModelResult getAmazonBedrockAnthropic3ChatModel(
//        List<String> activeProviderKeys, Map<String, String> modelConnectionParametersMap) {
//
//        String token = getAiProviderToken(AMAZON_BEDROCK_ANTHROPIC3.getKey(), activeProviderKeys);
//
//        if (token == null) {
//            AmazonBedrockAnthropic3 amazonBedrockAnthropic3 = aiProvider.getAmazonBedrockAnthropic3();
//
//            token = amazonBedrockAnthropic3.getApiKey();
//        }
//
//        modelConnectionParametersMap.put(TOKEN, token);
//
//        return AmazonBedrockAnthropic3ChatAction.CHAT_MODEL;
//    }
//
//    private ChatModelResult getAmazonBedrockCohereChatModel(
//        List<String> activeProviderKeys, Map<String, String> modelConnectionParametersMap) {
//
//        String token = getAiProviderToken(AMAZON_BEDROCK_COHERE.getKey(), activeProviderKeys);
//
//        if (token == null) {
//            AmazonBedrockCohere amazonBedrockCohere = aiProvider.getAmazonBedrockCohere();
//
//            token = amazonBedrockCohere.getApiKey();
//        }
//
//        modelConnectionParametersMap.put(TOKEN, token);
//
//        return AmazonBedrockCohereChatAction.CHAT_MODEL;
//    }
//
//    private ChatModelResult getAmazonBedrockJurassic2ChatModel(
//        List<String> activeProviderKeys, Map<String, String> modelConnectionParametersMap) {
//
//        String token = getAiProviderToken(AMAZON_BEDROCK_JURASSIC2.getKey(), activeProviderKeys);
//
//        if (token == null) {
//            AmazonBedrockJurassic2 amazonBedrockJurassic2 = aiProvider.getAmazonBedrockJurassic2();
//
//            token = amazonBedrockJurassic2.getApiKey();
//        }
//
//        modelConnectionParametersMap.put(TOKEN, token);
//
//        return AmazonBedrockJurassic2ChatAction.CHAT_MODEL;
//    }
//
//    private ChatModelResult getAmazonBedrockLlamaChatModel(
//        List<String> activeProviderKeys, Map<String, String> modelConnectionParametersMap) {
//
//        String token = getAiProviderToken(AMAZON_BEDROCK_LLAMA.getKey(), activeProviderKeys);
//
//        if (token == null) {
//            AmazonBedrockLlama amazonBedrockLlama = aiProvider.getAmazonBedrockLlama();
//
//            token = amazonBedrockLlama.getApiKey();
//        }
//
//        modelConnectionParametersMap.put(TOKEN, token);
//
//        return AmazonBedrockLlamaChatAction.CHAT_MODEL;
//    }
//
//    private ChatModelResult getAmazonBedrockTitanChatModel(
//        List<String> activeProviderKeys, Map<String, String> modelConnectionParametersMap) {
//
//        String token = getAiProviderToken(AMAZON_BEDROCK_TITAN.getKey(), activeProviderKeys);
//
//        if (token == null) {
//            AmazonBedrockTitan amazonBedrockTitan = aiProvider.getAmazonBedrockTitan();
//
//            token = amazonBedrockTitan.getApiKey();
//        }
//
//        modelConnectionParametersMap.put(TOKEN, token);
//
//        return AmazonBedrockTitanChatAction.CHAT_MODEL;
//    }

    private ChatModelResult getAnthropicChatModel(List<String> activeProviderKeys) {
        String token = getAiProviderToken(ANTHROPIC.getKey(), activeProviderKeys);

        if (token == null) {
            Anthropic anthropic = aiProvider.getAnthropic();

            token = anthropic.getApiKey();
        }

        return new ChatModelResult(AnthropicChatAction.CHAT_MODEL, token);
    }

    private ChatModelResult getAzureOpenAiChatModel(List<String> activeProviderKeys) {
        String token = getAiProviderToken(AZURE_OPEN_AI.getKey(), activeProviderKeys);

        if (token == null) {
            AzureOpenAi azureOpenAi = aiProvider.getAzureOpenAi();

            token = azureOpenAi.getApiKey();
        }

        return new ChatModelResult(AzureOpenAiChatAction.CHAT_MODEL, token);
    }

    private ChatModelResult getDeepSeekModel(List<String> activeProviderKeys) {
        String token = getAiProviderToken(DEEPSEEK.getKey(), activeProviderKeys);

        if (token == null) {
            DeepSeek deepSeek = aiProvider.getDeepSeek();

            token = deepSeek.getApiKey();
        }

        return new ChatModelResult(DeepSeekChatAction.CHAT_MODEL, token);
    }

    private ChatModelResult getGroqChatModel(List<String> activeProviderKeys) {
        String token = getAiProviderToken(GROQ.getKey(), activeProviderKeys);

        if (token == null) {
            Groq groq = aiProvider.getGroq();

            token = groq.getApiKey();
        }

        return new ChatModelResult(PerplexityChatAction.CHAT_MODEL, token);
    }

    private ChatModelResult getHuggingFaceChatModel(List<String> activeProviderKeys) {
        String token = getAiProviderToken(HUGGING_FACE.getKey(), activeProviderKeys);

        if (token == null) {
            HuggingFace huggingFace = aiProvider.getHuggingFace();

            token = huggingFace.getApiKey();
        }

        return new ChatModelResult(HuggingFaceChatAction.CHAT_MODEL, token);
    }

    private ChatModelResult getMistralChatModel(List<String> activeProviderKeys) {
        String token = getAiProviderToken(MISTRAL.getKey(), activeProviderKeys);

        if (token == null) {
            Mistral mistral = aiProvider.getMistral();

            token = mistral.getApiKey();
        }

        return new ChatModelResult(MistralChatAction.CHAT_MODEL, token);
    }

    private ChatModelResult getNvidiaChatModel(List<String> activeProviderKeys) {
        String token = getAiProviderToken(NVIDIA.getKey(), activeProviderKeys);

        if (token == null) {
            Nvidia nvidia = aiProvider.getNvidia();

            token = nvidia.getApiKey();
        }

        return new ChatModelResult(NvidiaChatAction.CHAT_MODEL, token);
    }

    private ChatModelResult getOpenAiChatModel(List<String> activeProviderKeys) {
        String token = getAiProviderToken(OPEN_AI.getKey(), activeProviderKeys);

        if (token == null) {
            OpenAi openAi = aiProvider.getOpenAi();

            token = openAi.getApiKey();
        }

        return new ChatModelResult(OpenAiChatAction.CHAT_MODEL, token);
    }

    private ChatModelResult getPerplexityChatModel(List<String> activeProviderKeys) {
        String token = getAiProviderToken(PERPLEXITY.getKey(), activeProviderKeys);

        if (token == null) {
            Perplexity perplexity = aiProvider.getPerplexity();

            token = perplexity.getApiKey();
        }

        return new ChatModelResult(PerplexityChatAction.CHAT_MODEL, token);
    }

    private ChatModelResult getVertexGeminiChatModel(List<String> activeProviderKeys) {
        String token = getAiProviderToken(VERTEX_GEMINI.getKey(), activeProviderKeys);

        if (token == null) {
            VertexGemini vertexGemini = aiProvider.getVertexGemini();

            token = vertexGemini.getApiKey();
        }

        return new ChatModelResult(VertexGeminiChatAction.CHAT_MODEL, token);
    }

    record ChatModelResult(ChatModel chatModel, String token) {
    }
}
