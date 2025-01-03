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

import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.text.analysis.constant.AiTextAnalysisConstants.FORMAT;
import static com.bytechef.component.ai.text.analysis.constant.AiTextAnalysisConstants.MODEL_PROVIDER;
import static com.bytechef.component.ai.text.analysis.constant.AiTextAnalysisConstants.TEXT;
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
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.openai.action.OpenAiChatAction;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.definition.AbstractActionDefinitionWrapper;
import com.bytechef.platform.component.definition.ParametersFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Marko Kriskovic
 */
public class AiTextAnalysisActionDefinition extends AbstractActionDefinitionWrapper {

    private final ApplicationProperties.Ai.Component component;

    @SuppressFBWarnings("EI")
    public AiTextAnalysisActionDefinition(ActionDefinition actionDefinition,
        ApplicationProperties.Ai.Component component) {
        super(actionDefinition);

        this.component = component;
    }

    @Override
    public Optional<PerformFunction> getPerform() {
        return Optional.of((SingleConnectionPerformFunction) this::perform);
    }

    protected String perform(
        Parameters inputParameters, Parameters connectionParameter, ActionContext context) {

        Map<String, String> modelConnectionParametersMap = new HashMap<>();

        ChatModel chatModel = switch (inputParameters.getRequiredInteger(MODEL_PROVIDER)) {
            case 0 -> {
                modelConnectionParametersMap.put(TOKEN, component.getAmazonBedrock()
                    .getApiKey());

                yield AmazonBedrockAnthropic2ChatAction.CHAT_MODEL;
            }
            case 1 -> {
                modelConnectionParametersMap.put(TOKEN, component.getAmazonBedrock()
                    .getApiKey());

                yield AmazonBedrockAnthropic3ChatAction.CHAT_MODEL;
            }
            case 2 -> {
                modelConnectionParametersMap.put(TOKEN, component.getAmazonBedrock()
                    .getApiKey());

                yield AmazonBedrockCohereChatAction.CHAT_MODEL;
            }
            case 3 -> {
                modelConnectionParametersMap.put(TOKEN, component.getAmazonBedrock()
                    .getApiKey());

                yield AmazonBedrockJurassic2ChatAction.CHAT_MODEL;
            }
            case 4 -> {
                modelConnectionParametersMap.put(TOKEN, component.getAmazonBedrock()
                    .getApiKey());

                yield AmazonBedrockLlamaChatAction.CHAT_MODEL;
            }
            case 5 -> {
                modelConnectionParametersMap.put(TOKEN, component.getAmazonBedrock()
                    .getApiKey());

                yield AmazonBedrockTitanChatAction.CHAT_MODEL;
            }
            case 6 -> {
                modelConnectionParametersMap.put(TOKEN, component.getAnthropic()
                    .getApiKey());

                yield AnthropicChatAction.CHAT_MODEL;
            }
            case 7 -> {
                modelConnectionParametersMap.put(TOKEN, component.getAzureOpenAi()
                    .getApiKey());

                yield AzureOpenAiChatAction.CHAT_MODEL;
            }
            case 8 -> {
                modelConnectionParametersMap.put(TOKEN, component.getGroq()
                    .getApiKey());

                yield GroqChatAction.CHAT_MODEL;
            }
            case 9 -> {
                modelConnectionParametersMap.put(TOKEN, component.getNvidia()
                    .getApiKey());

                yield NvidiaChatAction.CHAT_MODEL;
            }
            case 10 -> {
                modelConnectionParametersMap.put(TOKEN, component.getHuggingFace()
                    .getApiKey());

                yield HuggingFaceChatAction.CHAT_MODEL;
            }
            case 11 -> {
                modelConnectionParametersMap.put(TOKEN, component.getMistral()
                    .getApiKey());

                yield MistralChatAction.CHAT_MODEL;
            }
            case 12 -> {
                modelConnectionParametersMap.put(TOKEN, component.getOpenAi()
                    .getApiKey());

                yield OpenAiChatAction.CHAT_MODEL;
            }
            case 13 -> {
                modelConnectionParametersMap.put(TOKEN, component.getVertexGemini()
                    .getApiKey());

                yield VertexGeminiChatAction.CHAT_MODEL;
            }
            default -> throw new IllegalArgumentException("Invalid connection provider");
        };

        Parameters modelConnectionParameters = ParametersFactory.createParameters(modelConnectionParametersMap);

        Map<String, Object> modelInputParametersMap = new HashMap<>();

        String prompt = switch (inputParameters.getRequiredInteger(FORMAT)) {
            case 0 -> "You will receive a text. Make a structured summary of that text with sections.";
            case 1 -> "You will receive a text. Make a brief title summarizing the content in 4-7 words.";
            case 2 -> "You will receive a text. Summarize it in a single, concise sentence.";
            case 3 -> "You will receive a text. Create a bullet list recap.";
            case 4 -> "You will receive a text." + inputParameters.getString("prompt");
            default -> throw new IllegalArgumentException("Invalid format");
        };

        modelInputParametersMap.put("messages",
            List.of(
                Map.of("content", prompt, "role", "system"),
                Map.of("content", inputParameters.getString(TEXT), "role", "user")));
        modelInputParametersMap.put("model", inputParameters.getString(MODEL));

        Parameters modelInputParameters = ParametersFactory.createParameters(modelInputParametersMap);

        Object response = chatModel.getResponse(modelInputParameters, modelConnectionParameters, context);

        return response.toString();
    }
}
