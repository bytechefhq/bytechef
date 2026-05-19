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

package com.bytechef.component.ai.llm.router.open.router.constant;

import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.REASONING;
import static com.bytechef.component.ai.llm.router.constant.RouterConstants.SUPPORTED_PARAMETERS;
import static com.bytechef.component.ai.llm.router.open.router.util.OpenRouterUtils.getOpenRouterEmbeddingModels;
import static com.bytechef.component.ai.llm.router.open.router.util.OpenRouterUtils.getOpenRouterModels;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.llm.ChatModel;
import com.bytechef.component.definition.ComponentDsl.ModifiableArrayProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.bytechef.component.definition.Option;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Marko Kriskovic
 */
public class OpenRouterConstants {

    public static final String BASE_URL = "https://openrouter.ai/api/v1";

    public static final ModifiableStringProperty CHAT_MODEL_PROPERTY = string(MODEL)
        .label("Model")
        .description("ID of the model to use.")
        .options(getOpenRouterModels("text"))
        .optionsLookupDependsOn(SUPPORTED_PARAMETERS)
        .required(true);

    public static final ModifiableStringProperty EMBEDDING_MODEL_PROPERTY = string(MODEL)
        .label("Model")
        .description("ID of the model to use.")
        .options(getOpenRouterEmbeddingModels())
        .required(true);

    public static final ModifiableStringProperty IMAGE_MODEL_PROPERTY = string(MODEL)
        .label("Model")
        .description("ID of the model to use.")
        .options(getOpenRouterModels("image"))
        .required(true);

    public static final ModifiableStringProperty REASONING_PROPERTY = string(REASONING)
        .label("Reasoning effort")
        .description(
            "Constrains effort on reasoning. Reducing reasoning effort can result in faster responses and fewer " +
                "tokens used on reasoning in a response. For reasoning models for gpt-5 and o-series models only.")
        .options(
            option("none", "none"),
            option("minimal", "minimal"),
            option("low", "low"),
            option("medium", "medium"),
            option("high", "high"),
            option("maximal", "xhigh"))
        .displayCondition("response.responseFormat == '%s'".formatted(ChatModel.ResponseFormat.TEXT.name()))
        .advancedOption(true)
        .displayCondition("contains(%s, 'reasoning')".formatted(SUPPORTED_PARAMETERS));

    public static final ModifiableStringProperty SPEECH_MODEL_PROPERTY = string(MODEL)
        .label("Model")
        .description("ID of the model to use.")
        .options(getOpenRouterModels("speech"))
        .required(true);

    public static final ModifiableArrayProperty SUPPORTED_PARAMETERS_PROPERTY = array(SUPPORTED_PARAMETERS)
        .label("Supported parameters")
        .description("Filter models by supported parameter")
        .items(string())
        .options(getSupportedParametersOptions())
        .defaultValue("response_format")
        .required(true);

    public static final ModifiableStringProperty TRANSCRIPTION_MODEL_PROPERTY = string(MODEL)
        .label("Model")
        .description("ID of the model to use.")
        .options(getOpenRouterModels("transcription"))
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
