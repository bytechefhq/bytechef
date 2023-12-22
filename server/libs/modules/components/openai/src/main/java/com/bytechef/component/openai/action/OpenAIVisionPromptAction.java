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

import static com.bytechef.component.openai.constant.OpenAIConstants.CONTENT;
import static com.bytechef.component.openai.constant.OpenAIConstants.DETAIL;
import static com.bytechef.component.openai.constant.OpenAIConstants.FREQUENCY_PENALTY;
import static com.bytechef.component.openai.constant.OpenAIConstants.IMAGE_URL;
import static com.bytechef.component.openai.constant.OpenAIConstants.LOGIT_BIAS;
import static com.bytechef.component.openai.constant.OpenAIConstants.MAX_TOKENS;
import static com.bytechef.component.openai.constant.OpenAIConstants.MESSAGES;
import static com.bytechef.component.openai.constant.OpenAIConstants.MODEL;
import static com.bytechef.component.openai.constant.OpenAIConstants.N;
import static com.bytechef.component.openai.constant.OpenAIConstants.NAME;
import static com.bytechef.component.openai.constant.OpenAIConstants.PRESENCE_PENALTY;
import static com.bytechef.component.openai.constant.OpenAIConstants.RESPONSE_FORMAT;
import static com.bytechef.component.openai.constant.OpenAIConstants.ROLE;
import static com.bytechef.component.openai.constant.OpenAIConstants.SEED;
import static com.bytechef.component.openai.constant.OpenAIConstants.STOP;
import static com.bytechef.component.openai.constant.OpenAIConstants.STREAM;
import static com.bytechef.component.openai.constant.OpenAIConstants.TEMPERATURE;
import static com.bytechef.component.openai.constant.OpenAIConstants.TOOLS;
import static com.bytechef.component.openai.constant.OpenAIConstants.TOOL_CHOICE;
import static com.bytechef.component.openai.constant.OpenAIConstants.TOP_P;
import static com.bytechef.component.openai.constant.OpenAIConstants.TYPE;
import static com.bytechef.component.openai.constant.OpenAIConstants.URL;
import static com.bytechef.component.openai.constant.OpenAIConstants.USER;
import static com.bytechef.component.openai.constant.OpenAIConstants.VISION_PROMPT;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.array;
import static com.bytechef.hermes.component.definition.ComponentDSL.bool;
import static com.bytechef.hermes.component.definition.ComponentDSL.integer;
import static com.bytechef.hermes.component.definition.ComponentDSL.number;
import static com.bytechef.hermes.component.definition.ComponentDSL.object;
import static com.bytechef.hermes.component.definition.ComponentDSL.option;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;

import com.bytechef.component.openai.util.OpenAIUtils;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.hermes.component.definition.OutputSchemaDataSource;

/**
 * @author Monika Domiter
 */
public class OpenAIVisionPromptAction {
    public static final ModifiableActionDefinition ACTION_DEFINITION = action(VISION_PROMPT)
        .title("Vision Prompt")
        .description("Ask GPT a question about an image")
        .properties(
            array(MESSAGES)
                .label("Messages")
                .description("A list of messages comprising the conversation so far.")
                .items(
                    object().properties(
                        array(CONTENT)
                            .label("Content")
                            .description("The contents of the message.")
                            .items(
                                object().properties(
                                    string(TYPE)
                                        .label("Type")
                                        .description("The type of the content part.")
                                        .required(true),
                                    object(IMAGE_URL)
                                        .label("Image url")
                                        .properties(
                                            string(URL)
                                                .label("URL")
                                                .description(
                                                    "Either a URL of the image or the base64 encoded image data.")
                                                .required(true),
                                            string(DETAIL)
                                                .label("Detail")
                                                .description("Specifies the detail level of the image.")
                                                .defaultValue("auto"))))
                            .required(true),
                        string(ROLE)
                            .label("Role")
                            .description("The role of the messages author")
                            .options(
                                option("system", "system"),
                                option("user", "user"),
                                option("assistant", "assistant"),
                                option("tool", "tool"))
                            .required(true),
                        string(NAME)
                            .label("Name")
                            .description("An optional name for the participant. " +
                                "Provides the model information to differentiate between participants of the same role.")
                            .required(false)))
                .required(true),
            string(MODEL)
                .label("Model")
                .description("ID of the model to use.")
                .required(true),
            number(FREQUENCY_PENALTY)
                .label("Frequency penalty")
                .description("Number between -2.0 and 2.0. " +
                    "Positive values penalize new tokens based on their existing frequency in the text so far, " +
                    "decreasing the model's likelihood to repeat the same line verbatim.")
                .defaultValue(0)
                .minValue(-2)
                .maxValue(2)
                .required(false),
            object(LOGIT_BIAS)
                .label("Logit bias")
                .description("Modify the likelihood of specified tokens appearing in the completion.")
                .defaultValue(null)
                .required(false),
            integer(MAX_TOKENS)
                .label("Max tokens")
                .description("The maximum number of tokens to generate in the chat completion.")
                .required(false),
            integer(N)
                .label("n")
                .description("How many chat completion choices to generate for each input message.")
                .defaultValue(1)
                .required(false),
            number(PRESENCE_PENALTY)
                .label("Presence penalty")
                .description("Number between -2.0 and 2.0. " +
                    "Positive values penalize new tokens based on whether they appear in the text so far," +
                    " increasing the model's likelihood to talk about new topics.")
                .defaultValue(0)
                .minValue(-2)
                .maxValue(2)
                .required(false),
            object(RESPONSE_FORMAT)
                .label("Response format")
                .description("An object specifying the format that the model must output.")
                .required(false),
            integer(SEED)
                .label("Seed")
                .description("")
                .required(false),
            object(STOP)
                .label("Stop")
                .description("Up to 4 sequences where the API will stop generating further tokens.")
                .defaultValue(null)
                .required(false),
            bool(STREAM)
                .label("Stream")
                .description("")
                .defaultValue(false)
                .required(false),
            number(TEMPERATURE)
                .label("Temperature")
                .description("Controls randomness:  Higher values will make the output more random, " +
                    "while lower values like will make it more focused and deterministic.")
                .defaultValue(1)
                .minValue(0)
                .maxValue(2)
                .required(false),
            number(TOP_P)
                .label("Top p")
                .description("")
                .defaultValue(1)
                .required(false),
            array(TOOLS)
                .label("Tools")
                .description("")
                .items(
                    object().properties(
                        string("type")
                            .label("Type")
                            .description("The type of the tool. Currently, only function is supported.")
                            .required(true),
                        object("function")
                            .label("Function")
                            .required(true)
                            .properties(
                                string("description")
                                    .label("Description")
                                    .description("A description of what the function does, " +
                                        "used by the model to choose when and how to call the function.")
                                    .required(false),
                                string("name")
                                    .label("Name")
                                    .description("The name of the function to be called. " +
                                        "Must be a-z, A-Z, 0-9, or contain underscores and dashes")
                                    .maxLength(64)
                                    .required(true),
                                object("parameters")
                                    .label("Parameters")
                                    .required(false))))
                .required(false),
            object(TOOL_CHOICE)
                .label("Tool choice")
                .description("")
                .required(false),
            string(USER)
                .label("User")
                .description("A unique identifier representing your end-user, " +
                    "which can help OpenAI to monitor and detect abuse.")
                .required(false))
        .outputSchema((OutputSchemaDataSource.ActionOutputSchemaFunction) OpenAIUtils::getOutputSchemaResponse)
        .perform(OpenAIAskChatGPTAction::perform);

    private OpenAIVisionPromptAction() {
    }

}
