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

package com.bytechef.component.openai.action;

import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.openai.constant.OpenAIConstants.DESCRIPTION;
import static com.bytechef.component.openai.constant.OpenAIConstants.FREQUENCY_PENALTY;
import static com.bytechef.component.openai.constant.OpenAIConstants.FUNCTION;
import static com.bytechef.component.openai.constant.OpenAIConstants.LOGIT_BIAS;
import static com.bytechef.component.openai.constant.OpenAIConstants.MAX_TOKENS;
import static com.bytechef.component.openai.constant.OpenAIConstants.MODEL;
import static com.bytechef.component.openai.constant.OpenAIConstants.N;
import static com.bytechef.component.openai.constant.OpenAIConstants.NAME;
import static com.bytechef.component.openai.constant.OpenAIConstants.PARAMETERS;
import static com.bytechef.component.openai.constant.OpenAIConstants.PRESENCE_PENALTY;
import static com.bytechef.component.openai.constant.OpenAIConstants.RESPONSE_FORMAT;
import static com.bytechef.component.openai.constant.OpenAIConstants.SEED;
import static com.bytechef.component.openai.constant.OpenAIConstants.STOP;
import static com.bytechef.component.openai.constant.OpenAIConstants.STREAM;
import static com.bytechef.component.openai.constant.OpenAIConstants.TEMPERATURE;
import static com.bytechef.component.openai.constant.OpenAIConstants.TOOLS;
import static com.bytechef.component.openai.constant.OpenAIConstants.TOOL_CHOICE;
import static com.bytechef.component.openai.constant.OpenAIConstants.TOP_P;
import static com.bytechef.component.openai.constant.OpenAIConstants.TYPE;
import static com.bytechef.component.openai.constant.OpenAIConstants.USER;

import com.bytechef.component.definition.ComponentDSL;

/**
 * @author Monika Domiter
 */
public abstract class AbstractChatCompletionAction {

    public static final ComponentDSL.ModifiableStringProperty MODEL_PROPERTY = string(MODEL)
        .label("Model")
        .description("ID of the model to use.")
        .required(true);

    public static final ComponentDSL.ModifiableNumberProperty FREQUENCY_PENALTY_PROPERTY = number(FREQUENCY_PENALTY)
        .label("Frequency penalty")
        .description(
            "Number between -2.0 and 2.0. Positive values penalize new tokens based on their existing frequency in " +
                "the text so far, decreasing the model's likelihood to repeat the same line verbatim.")
        .defaultValue(0)
        .minValue(-2)
        .maxValue(2)
        .required(false);

    public static final ComponentDSL.ModifiableObjectProperty LOGIT_BIAS_PROPERTY = object(LOGIT_BIAS)
        .label("Logit bias")
        .description("Modify the likelihood of specified tokens appearing in the completion.")
        .defaultValue(null)
        .required(false);

    public static final ComponentDSL.ModifiableIntegerProperty MAX_TOKENS_PROPERTY = integer(MAX_TOKENS)
        .label("Max tokens")
        .description("The maximum number of tokens to generate in the chat completion.")
        .required(false);

    public static final ComponentDSL.ModifiableIntegerProperty n = integer(N)
        .label("n")
        .description("How many chat completion choices to generate for each input message.")
        .defaultValue(1)
        .required(false);

    public static final ComponentDSL.ModifiableNumberProperty PRESENCE_PENALTY_PROPERTY = number(PRESENCE_PENALTY)
        .label("Presence penalty")
        .description(
            "Number between -2.0 and 2.0. Positive values penalize new tokens based on whether they appear in the " +
                "text so far, increasing the model's likelihood to talk about new topics.")
        .defaultValue(0)
        .minValue(-2)
        .maxValue(2)
        .required(false);

    public static final ComponentDSL.ModifiableObjectProperty RESPONSE_FORMAT_PROPERTY = object(RESPONSE_FORMAT)
        .label("Response format")
        .description(
            "An object specifying the format that the model must output. Compatible with gpt-4-1106-preview and " +
                "gpt-3.5-turbo-1106")
        .required(false);

    public static final ComponentDSL.ModifiableIntegerProperty SEED_PROPERTY = integer(SEED)
        .label("Seed")
        .description(
            "This feature is in Beta. If specified, our system will make a best effort to sample deterministically, " +
                "such that repeated requests with the same seed and parameters should return the same result. " +
                "Determinism is not guaranteed, and you should refer to the system_fingerprint response parameter to " +
                "monitor changes in the backend.")
        .required(false);

    public static final ComponentDSL.ModifiableObjectProperty STOP_PROPERTY = object(STOP)
        .label("Stop")
        .description("Up to 4 sequences where the API will stop generating further tokens.")
        .defaultValue(null)
        .required(false);

    public static final ComponentDSL.ModifiableBooleanProperty STREAM_PROPERTY = bool(STREAM)
        .label("Stream")
        .description(
            "If set, partial message deltas will be sent, like in ChatGPT. Tokens will be sent as data-only " +
                "server-sent events as they become available, with the stream terminated by a data: [DONE] message.")
        .defaultValue(false)
        .required(true);

    public static final ComponentDSL.ModifiableNumberProperty TEMPERATURE_PROPERTY = number(TEMPERATURE)
        .label("Temperature")
        .description(
            "Controls randomness:  Higher values will make the output more random, while lower values like will make " +
                "it more focused and deterministic.")
        .defaultValue(1)
        .minValue(0)
        .maxValue(2)
        .required(false);

    public static final ComponentDSL.ModifiableNumberProperty TOP_P_PROPERTY = number(TOP_P)
        .label("Top p")
        .description(
            "An alternative to sampling with temperature, called nucleus sampling,  where the model considers the " +
                "results of the tokens with top_p probability mass. So 0.1 means only the tokens comprising the top " +
                "10% probability mass are considered.")
        .defaultValue(1)
        .required(false);

    public static final ComponentDSL.ModifiableArrayProperty TOOLS_PROPERTY = array(TOOLS)
        .label("Tools")
        .description(
            "A list of tools the model may call. Currently, only functions are supported as a tool. Use this to " +
                "provide a list of functions the model may generate JSON inputs for.")
        .items(
            object().properties(
                string(TYPE)
                    .label("Type")
                    .description("The type of the tool. Currently, only function is supported.")
                    .required(true),
                object(FUNCTION)
                    .label("Function")
                    .required(true)
                    .properties(
                        string(DESCRIPTION)
                            .label("Description")
                            .description(
                                "A description of what the function does, used by the model to choose when and how " +
                                    "to call the function.")
                            .required(false),
                        string(NAME)
                            .label("Name")
                            .description(
                                "The name of the function to be called. Must be a-z, A-Z, 0-9, or contain " +
                                    "underscores and dashes")
                            .maxLength(64)
                            .required(true),
                        object(PARAMETERS)
                            .label("Parameters")
                            .description("The parameters the functions accepts, described as a JSON Schema object.")
                            .required(false))))
        .required(false);

    public static final ComponentDSL.ModifiableObjectProperty TOOL_CHOICE_PROPERTY = object(TOOL_CHOICE)
        .label("Tool choice")
        .description("")
        .required(false);

    public static final ComponentDSL.ModifiableStringProperty USER_PROPERTY = string(USER)
        .label("User")
        .description(
            "A unique identifier representing your end-user, which can help OpenAI to monitor and detect abuse.")
        .required(false);

    AbstractChatCompletionAction() {
    }
}
