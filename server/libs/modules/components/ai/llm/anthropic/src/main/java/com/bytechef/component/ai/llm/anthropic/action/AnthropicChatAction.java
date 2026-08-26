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
import static com.bytechef.component.ai.llm.constant.LLMConstants.ASK;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MAX_TOKENS;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.REASONING_EFFORT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.STOP;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TEMPERATURE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.THINKING;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_K;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_P;
import static com.bytechef.component.ai.llm.constant.LLMConstants.WEB_SEARCH;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDsl.action;

import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClientAsync;
import com.bytechef.component.ai.llm.ChatModel;
import com.bytechef.component.ai.llm.util.ModelUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.Map;
import org.springframework.ai.anthropic.AnthropicCacheOptions;
import org.springframework.ai.anthropic.AnthropicCacheStrategy;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.AnthropicWebSearchTool;

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

    /**
     * Anthropic expresses extended thinking as a token budget rather than an effort word, so the shared
     * {@code reasoningEffort} property is translated here. The three values are the budgets Anthropic's own guidance
     * suggests for light, ordinary and hard reasoning; they are a starting point per effort level, not a hard contract.
     */
    private static final Map<String, Integer> THINKING_BUDGET_TOKENS = Map.of(
        "low", 2048, "medium", 8192, "high", 24576);

    /** Anthropic rejects a thinking budget below this. */
    private static final int MIN_THINKING_BUDGET_TOKENS = 1024;

    public static final ChatModel CHAT_MODEL = (inputParameters, connectionParameters, responseFormatRequired) -> {
        AnthropicChatOptions.Builder optionsBuilder = AnthropicChatOptions.builder()
            .model(inputParameters.getRequiredString(MODEL))
            .maxTokens(inputParameters.getInteger(MAX_TOKENS))
            .stopSequences(inputParameters.getList(STOP, new TypeReference<>() {}))
            .cacheOptions(
                AnthropicCacheOptions.builder()
                    .strategy(AnthropicCacheStrategy.CONVERSATION_HISTORY)
                    .build());

        // Anthropic's own server-side search — the model searches inside the completion and never emits a tool
        // call, so nothing needs a connection or a tool callback. Attached only when asked for: an always-present
        // web_search tool would be billed per search and would change answers for every agent.
        if (inputParameters.getBoolean(WEB_SEARCH, false)) {
            optionsBuilder.webSearchTool(
                AnthropicWebSearchTool.builder()
                    .build());
        }

        // Extended thinking is mutually exclusive with the sampling knobs: Anthropic pins temperature to 1 and
        // rejects `top_p`/`top_k` outright while thinking is on, so those are left unset rather than sent and
        // refused. The budget must also stay under `max_tokens`, which is why it is clamped rather than passed
        // through — an agent configured for high effort against a small completion budget should still run.
        if (inputParameters.getBoolean(THINKING, false)) {
            optionsBuilder.thinkingEnabled(resolveThinkingBudgetTokens(inputParameters));
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

    private static long resolveThinkingBudgetTokens(Parameters inputParameters) {
        int budgetTokens = THINKING_BUDGET_TOKENS.getOrDefault(
            inputParameters.getString(REASONING_EFFORT, "medium"), THINKING_BUDGET_TOKENS.get("medium"));

        Integer maxTokens = inputParameters.getInteger(MAX_TOKENS);

        if (maxTokens == null) {
            return budgetTokens;
        }

        if (maxTokens <= MIN_THINKING_BUDGET_TOKENS) {
            throw new IllegalArgumentException(
                "Thinking needs a Max Tokens above %d, so the reasoning budget can fit inside it, but Max Tokens is %d."
                    .formatted(MIN_THINKING_BUDGET_TOKENS, maxTokens));
        }

        return Math.min(budgetTokens, maxTokens - 1);
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        return CHAT_MODEL.getResponse(inputParameters, connectionParameters, context);
    }
}
