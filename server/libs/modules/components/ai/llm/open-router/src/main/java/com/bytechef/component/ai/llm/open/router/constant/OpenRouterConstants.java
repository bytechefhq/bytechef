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

import static com.bytechef.component.ai.llm.constant.LLMConstants.FREQUENCY_PENALTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.LOGIT_BIAS;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MAX_TOKENS;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.PRESENCE_PENALTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.REASONING;
import static com.bytechef.component.ai.llm.constant.LLMConstants.SEED;
import static com.bytechef.component.ai.llm.constant.LLMConstants.STOP;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TEMPERATURE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_K;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_P;
import static com.bytechef.component.ai.llm.constant.LLMConstants.VERBOSITY;
import static com.bytechef.component.ai.llm.open.router.util.OpenRouterUtils.getOpenRouterModels;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.llm.ChatModel;
import com.bytechef.component.definition.ComponentDsl.ModifiableArrayProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableBooleanProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableIntegerProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableNumberProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
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

    public static final ModifiableStringProperty CHAT_MODEL_PROPERTY = string(MODEL)
        .label("Model")
        .description("ID of the model to use.")
        .options(getOpenRouterModels())
        .optionsLookupDependsOn(SUPPORTED_PARAMETERS)
        .required(true);

    public static final ModifiableNumberProperty FREQUENCY_PENALTY_PROPERTY = number(FREQUENCY_PENALTY)
        .label("Frequency Penalty")
        .description(
            "Number between -2.0 and 2.0. Positive values penalize new tokens based on their existing frequency in " +
                "the text so far, decreasing the model's likelihood to repeat the same line verbatim.")
        .displayCondition("contains(%s, 'frequency_penalty')".formatted(SUPPORTED_PARAMETERS))
        .defaultValue(0)
        .minValue(-2)
        .maxValue(2)
        .advancedOption(true);

    public static final ModifiableObjectProperty LOGIT_BIAS_PROPERTY = object(LOGIT_BIAS)
        .label("Logit Bias")
        .description("Modify the likelihood of specified tokens appearing in the completion.")
        .displayCondition("contains(%s, 'logit_bias')".formatted(SUPPORTED_PARAMETERS))
        .additionalProperties(number())
        .advancedOption(true);

    public static final ModifiableBooleanProperty LOGPROBS_PROPERTY = bool(LOGPROBS)
        .label("Logprobs")
        .description("Return log probabilities.")
        .displayCondition("contains(%s, 'logprobs')".formatted(SUPPORTED_PARAMETERS))
        .required(false);

    public static final ModifiableIntegerProperty MAX_TOKENS_PROPERTY = integer(MAX_TOKENS)
        .label("Max Tokens")
        .description("The maximum number of tokens to generate in the chat completion.")
        .displayCondition("contains(%s, 'max_tokens')".formatted(SUPPORTED_PARAMETERS))
        .advancedOption(true);

    public static final ModifiableIntegerProperty MAX_COMPLETION_TOKENS_PROPERTY = integer(MAX_COMPLETION_TOKENS)
        .label("Max Completion Tokens")
        .description("Maximum tokens in completion.")
        .displayCondition("contains(%s, 'max_completion_tokens')".formatted(SUPPORTED_PARAMETERS))
        .required(false);

    public static final ModifiableNumberProperty PRESENCE_PENALTY_PROPERTY = number(PRESENCE_PENALTY)
        .label("Presence Penalty")
        .description(
            "Number between -2.0 and 2.0. Positive values penalize new tokens based on whether they appear in the " +
                "text so far, increasing the model's likelihood to talk about new topics.")
        .displayCondition("contains(%s, 'presence_penalty')".formatted(SUPPORTED_PARAMETERS))
        .defaultValue(0)
        .minValue(-2)
        .maxValue(2)
        .advancedOption(true);

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

    public static final ModifiableIntegerProperty SEED_PROPERTY = integer(SEED)
        .label("Seed")
        .description("Keeping the same seed would output the same response.")
        .advancedOption(true)
        .displayCondition("contains(%s, 'seed')".formatted(SUPPORTED_PARAMETERS));

    public static final ModifiableArrayProperty STOP_PROPERTY = array(STOP)
        .label("Stop")
        .description("Up to 4 sequences where the API will stop generating further tokens.")
        .items(string())
        .advancedOption(true)
        .displayCondition("contains(%s, 'stop')".formatted(SUPPORTED_PARAMETERS));

    public static final ModifiableArrayProperty SUPPORTED_PARAMETERS_PROPERTY = array(SUPPORTED_PARAMETERS)
        .label("Supported parameters")
        .description("Filter models by supported parameter")
        .items(string())
        .options(getSupportedParametersOptions())
        .defaultValue("response_format")
        .required(true);

    public static final ModifiableNumberProperty TEMPERATURE_PROPERTY = number(TEMPERATURE)
        .label("Temperature")
        .description(
            "Controls randomness:  Higher values will make the output more random, while lower values like will " +
                "make it more focused and deterministic.")
        .defaultValue(1)
        .minValue(0)
        .maxValue(2)
        .advancedOption(true)
        .displayCondition("contains(%s, 'temperature')".formatted(SUPPORTED_PARAMETERS));

    public static final ModifiableIntegerProperty TOP_K_PROPERTY = integer(TOP_K)
        .label("Top K")
        .description("Specify the number of token choices the generative uses to generate the next token.")
        .defaultValue(1)
        .advancedOption(true)
        .displayCondition("contains(%s, 'top_k')".formatted(SUPPORTED_PARAMETERS));

    public static final ModifiableIntegerProperty TOP_LOGPROBS_PROPERTY = integer(TOP_LOGPROBS)
        .label("Top Logprobs")
        .description("Number of top log probabilities to return (0-20).")
        .displayCondition("contains(%s, 'top_logprobs')".formatted(SUPPORTED_PARAMETERS))
        .minValue(0)
        .maxValue(20)
        .required(false);

    public static final ModifiableNumberProperty TOP_P_PROPERTY = number(TOP_P)
        .label("Top P")
        .description(
            "An alternative to sampling with temperature, called nucleus sampling,  where the model considers the " +
                "results of the tokens with top_p probability mass. So 0.1 means only the tokens comprising the top " +
                "10% probability mass are considered.")
        .defaultValue(1)
        .advancedOption(true)
        .displayCondition("contains(%s, 'top_p')".formatted(SUPPORTED_PARAMETERS));

    public static final ModifiableStringProperty VERBOSITY_PROPERTY = string(VERBOSITY)
        .label("Verbosity")
        .description("Adjusts response verbosity. Lower levels yield shorter answers.")
        .options(
            option("low", "low"),
            option("medium", "medium"),
            option("high", "high"))
        .displayCondition("response.responseFormat == '%s'".formatted(ChatModel.ResponseFormat.TEXT.name()))
        .advancedOption(true)
        .displayCondition("contains(%s, 'verbosity')".formatted(SUPPORTED_PARAMETERS));

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
