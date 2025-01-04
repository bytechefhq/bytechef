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

package com.bytechef.component.ai.text.analysis.action.definition;

import static com.bytechef.component.ai.text.analysis.constant.AiTextAnalysisConstants.MODEL_PROVIDER;
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
import com.bytechef.component.ai.llm.groq.action.GroqChatAction;
import com.bytechef.component.ai.llm.hugging.face.action.HuggingFaceChatAction;
import com.bytechef.component.ai.llm.mistral.action.MistralChatAction;
import com.bytechef.component.ai.llm.nvidia.action.NvidiaChatAction;
import com.bytechef.component.ai.llm.vertex.gemini.action.VertexGeminiChatAction;
import com.bytechef.component.ai.text.analysis.action.AiTextAnalysisAction;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.openai.action.OpenAiChatAction;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.definition.AbstractActionDefinitionWrapper;
import com.bytechef.platform.component.definition.ParametersFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author Marko Kriskovic
 */
public class AiTextAnalysisActionDefinition extends AbstractActionDefinitionWrapper {

    private final ApplicationProperties.Ai.Component component;
    private final AiTextAnalysisAction aiTextAnalysisAction;

    @SuppressFBWarnings("EI")
    public AiTextAnalysisActionDefinition(
        ActionDefinition actionDefinition, ApplicationProperties.Ai.Component component,
        AiTextAnalysisAction aiTextAnalysisAction) {

        super(actionDefinition);

        this.component = component;
        this.aiTextAnalysisAction = aiTextAnalysisAction;
    }

    @Override
    public Optional<PerformFunction> getPerform() {
        return Optional.of((SingleConnectionPerformFunction) this::perform);
    }

    protected String perform(
        Parameters inputParameters, Parameters connectionParameter, ActionContext context) {

        Map<String, String> modelConnectionParametersMap = new HashMap<>();

        final ApplicationProperties.Ai.Component.AmazonBedrock amazonBedrock = component.getAmazonBedrock();

        ChatModel chatModel = switch (inputParameters.getRequiredInteger(MODEL_PROVIDER)) {
            case 0 -> {
                modelConnectionParametersMap.put(TOKEN, amazonBedrock.getApiKey());

                yield AmazonBedrockAnthropic2ChatAction.CHAT_MODEL;
            }
            case 1 -> {
                modelConnectionParametersMap.put(TOKEN, amazonBedrock.getApiKey());

                yield AmazonBedrockAnthropic3ChatAction.CHAT_MODEL;
            }
            case 2 -> {
                modelConnectionParametersMap.put(TOKEN, amazonBedrock.getApiKey());

                yield AmazonBedrockCohereChatAction.CHAT_MODEL;
            }
            case 3 -> {
                modelConnectionParametersMap.put(TOKEN, amazonBedrock.getApiKey());

                yield AmazonBedrockJurassic2ChatAction.CHAT_MODEL;
            }
            case 4 -> {
                modelConnectionParametersMap.put(TOKEN, amazonBedrock.getApiKey());

                yield AmazonBedrockLlamaChatAction.CHAT_MODEL;
            }
            case 5 -> {
                modelConnectionParametersMap.put(TOKEN, amazonBedrock.getApiKey());

                yield AmazonBedrockTitanChatAction.CHAT_MODEL;
            }
            case 6 -> {
                modelConnectionParametersMap.put(TOKEN, component.getAnthropic()
                    .getApiKey());

                yield AnthropicChatAction.CHAT_MODEL;
            }
            case 7 -> {
                ApplicationProperties.Ai.Component.AzureOpenAi azureOpenAi = component.getAzureOpenAi();

                modelConnectionParametersMap.put(TOKEN, azureOpenAi.getApiKey());

                yield AzureOpenAiChatAction.CHAT_MODEL;
            }
            case 8 -> {
                ApplicationProperties.Ai.Component.Groq groq = component.getGroq();

                modelConnectionParametersMap.put(TOKEN, groq.getApiKey());

                yield GroqChatAction.CHAT_MODEL;
            }
            case 9 -> {
                ApplicationProperties.Ai.Component.Nvidia nvidia = component.getNvidia();

                modelConnectionParametersMap.put(TOKEN, nvidia.getApiKey());

                yield NvidiaChatAction.CHAT_MODEL;
            }
            case 10 -> {
                ApplicationProperties.Ai.Component.HuggingFace huggingFace = component.getHuggingFace();

                modelConnectionParametersMap.put(TOKEN, huggingFace.getApiKey());

                yield HuggingFaceChatAction.CHAT_MODEL;
            }
            case 11 -> {
                ApplicationProperties.Ai.Component.Mistral mistral = component.getMistral();

                modelConnectionParametersMap.put(TOKEN, mistral.getApiKey());

                yield MistralChatAction.CHAT_MODEL;
            }
            case 12 -> {
                ApplicationProperties.Ai.Component.OpenAi openAi = component.getOpenAi();

                modelConnectionParametersMap.put(TOKEN, openAi.getApiKey());

                yield OpenAiChatAction.CHAT_MODEL;
            }
            case 13 -> {
                ApplicationProperties.Ai.Component.VertexGemini vertexGemini = component.getVertexGemini();

                modelConnectionParametersMap.put(TOKEN, vertexGemini.getApiKey());

                yield VertexGeminiChatAction.CHAT_MODEL;
            }
            default -> throw new IllegalArgumentException("Invalid connection provider");
        };

        Parameters modelConnectionParameters = ParametersFactory.createParameters(modelConnectionParametersMap);

        Parameters modelInputParameters = aiTextAnalysisAction.createParameters(inputParameters);

        Object response = chatModel.getResponse(modelInputParameters, modelConnectionParameters, context);

        return response.toString();
    }
}
