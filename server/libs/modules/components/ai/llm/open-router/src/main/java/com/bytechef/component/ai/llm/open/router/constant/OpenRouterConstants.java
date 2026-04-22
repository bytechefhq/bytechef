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

package com.bytechef.component.ai.llm.open.router.constant;

import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.open.router.util.OpenRouterUtils.getOpenRouterModels;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.ComponentDsl.ModifiableBooleanProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableIntegerProperty;
import com.bytechef.component.definition.Option;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Marko Kriskovic
 */
public class OpenRouterConstants {

    public static final String LOGPROBS = "logprobs";
    public static final String MAX_COMPLETION_TOKENS = "maxCompletionTokens";
    public static final String SUPPORTED_PARAMETERS = "supportedParameters";
    public static final String TOP_LOGPROBS = "topLogprobs";

    public static final ModifiableBooleanProperty LOGPROBS_PROPERTY = bool(LOGPROBS)
        .label("Logprobs")
        .description("Return log probabilities.")
        .required(false);

    public static final ModifiableIntegerProperty MAX_COMPLETION_TOKENS_PROPERTY = integer(MAX_COMPLETION_TOKENS)
        .label("Max Completion Tokens")
        .description("Maximum tokens in completion.")
        .required(false);

    public static final ModifiableIntegerProperty TOP_LOGPROBS_PROPERTY = integer(TOP_LOGPROBS)
        .label("Top Logprobs")
        .description("Number of top log probabilities to return (0-20).")
        .minValue(0)
        .maxValue(20)
        .required(false);

    public static final ComponentDsl.ModifiableStringProperty CHAT_MODEL_PROPERTY = string(MODEL)
        .label("Model")
        .description("ID of the model to use.")
        .options(getOpenRouterModels())
        .optionsLookupDependsOn(SUPPORTED_PARAMETERS)
        .required(true);

    public static final ComponentDsl.ModifiableArrayProperty SUPPORTED_PARAMETERS_PROPERTY = array(SUPPORTED_PARAMETERS)
        .label("Supported parameters")
        .description("Filter models by supported parameter")
        .items(string())
        .options(getSupportedParametersOptions())
        .defaultValue("response_format")
        .required(true);

    private OpenRouterConstants() {
    }

    private static String[] getSupportedParametersString() {
        return new String[] {
            "frequency_penalty",
            "include_reasoning",
            "logit_bias", "logprobs",
            "max_completion_tokens", "max_tokens", "min_p",
            "parallel_tool_calls", "presence_penalty",
            "reasoning", "reasoning_effort", "response_format", "repetition_penalty",
            "seed", "stop", "structured_outputs",
            "temperature", "tools", "tool_choice", "top_a", "top_k", "top_p", "top_logprobs",
            "verbosity",
            "web_search_options"
        };
    }

    private static List<Option<String>> getSupportedParametersOptions() {
        return Arrays.stream(getSupportedParametersString())
            .map(param -> option(param, param))
            .collect(Collectors.toList());
    }
}
