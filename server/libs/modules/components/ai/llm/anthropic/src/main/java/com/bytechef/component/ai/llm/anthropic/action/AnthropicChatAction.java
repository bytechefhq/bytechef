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

package com.bytechef.component.ai.llm.anthropic.action;

import static com.bytechef.component.ai.llm.anthropic.constant.AnthropicConstants.ASK_PROPERTIES;
import static com.bytechef.component.ai.llm.anthropic.constant.AnthropicConstants.DEFAULT_MAX_TOKENS;
import static com.bytechef.component.ai.llm.constant.LLMConstants.ASK;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MAX_TOKENS;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.REASONING_EFFORT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.STOP;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TEMPERATURE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.THINKING;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_K;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_P;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDsl.action;

import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClientAsync;
import com.anthropic.models.messages.OutputConfig;
import com.bytechef.component.ai.llm.ChatModel;
import com.bytechef.component.ai.llm.util.ModelUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import org.springframework.ai.anthropic.AnthropicCacheOptions;
import org.springframework.ai.anthropic.AnthropicCacheStrategy;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;

/**
 * @author Marko Kriskovic
 */
public class AnthropicChatAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(ASK)
        .title("Ask")
        .description("Ask anything you want.")
        .properties(ASK_PROPERTIES)
        .output(ModelUtils::output)
        .help("", "https://docs.bytechef.io/reference/components/anthropic_v1#ask")
        .perform(AnthropicChatAction::perform);

    public static final ChatModel CHAT_MODEL = (inputParameters, connectionParameters, responseFormatRequired) -> {
        AnthropicChatOptions.Builder optionsBuilder = AnthropicChatOptions.builder()
            .model(inputParameters.getRequiredString(MODEL))
            .maxTokens(inputParameters.getInteger(MAX_TOKENS, DEFAULT_MAX_TOKENS))
            .stopSequences(inputParameters.getList(STOP, new TypeReference<>() {}))
            .cacheOptions(
                AnthropicCacheOptions.builder()
                    .strategy(AnthropicCacheStrategy.CONVERSATION_HISTORY)
                    .build());

        if (inputParameters.getBoolean(THINKING, false)) {
            optionsBuilder.thinkingAdaptive()
                .effort(OutputConfig.Effort.of(inputParameters.getString(REASONING_EFFORT, "medium")));
        } else {
            optionsBuilder.topK(inputParameters.getInteger(TOP_K));

            // Anthropic rejects requests that include both `temperature` and `top_p`; pick one.
            Double temperature = inputParameters.getDouble(TEMPERATURE);

            if (temperature != null) {
                optionsBuilder.temperature(temperature);
            } else {
                Double topP = inputParameters.getDouble(TOP_P);

                if (topP != null) {
                    optionsBuilder.topP(topP);
                }
            }
        }

        String apiKey = connectionParameters.getRequiredString(TOKEN);

        return AnthropicChatModel.builder()
            .anthropicClient(
                AnthropicOkHttpClient.builder()
                    .apiKey(apiKey)
                    .build())
            .anthropicClientAsync(
                AnthropicOkHttpClientAsync.builder()
                    .apiKey(apiKey)
                    .build())
            .options(optionsBuilder.build())
            .build();
    };

    private AnthropicChatAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        return CHAT_MODEL.getResponse(inputParameters, connectionParameters, context);
    }
}
