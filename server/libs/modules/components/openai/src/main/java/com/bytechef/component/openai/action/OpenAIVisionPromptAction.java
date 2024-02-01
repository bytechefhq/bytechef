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

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.openai.constant.OpenAIConstants.CONTENT;
import static com.bytechef.component.openai.constant.OpenAIConstants.DETAIL;
import static com.bytechef.component.openai.constant.OpenAIConstants.IMAGE_URL;
import static com.bytechef.component.openai.constant.OpenAIConstants.MESSAGES;
import static com.bytechef.component.openai.constant.OpenAIConstants.NAME;
import static com.bytechef.component.openai.constant.OpenAIConstants.ROLE;
import static com.bytechef.component.openai.constant.OpenAIConstants.TYPE;
import static com.bytechef.component.openai.constant.OpenAIConstants.URL;
import static com.bytechef.component.openai.constant.OpenAIConstants.VISION_PROMPT;

import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.openai.util.OpenAIUtils;

/**
 * @author Monika Domiter
 */
public class OpenAIVisionPromptAction extends AbstractChatCompletionAction {

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
                            .description(
                                "An optional name for the participant. Provides the model information to " +
                                    "differentiate between participants of the same role.")
                            .required(false)))
                .required(true),
            MODEL_PROPERTY,
            FREQUENCY_PENALTY_PROPERTY,
            LOGIT_BIAS_PROPERTY,
            MAX_TOKENS_PROPERTY,
            n,
            PRESENCE_PENALTY_PROPERTY,
            RESPONSE_FORMAT_PROPERTY,
            SEED_PROPERTY,
            STOP_PROPERTY,
            STREAM_PROPERTY,
            TEMPERATURE_PROPERTY,
            TOP_P_PROPERTY,
            TOOLS_PROPERTY,
            TOOL_CHOICE_PROPERTY,
            USER_PROPERTY)
        .output(OpenAIUtils::getOutput)
        .perform(OpenAIAskChatGPTAction::perform);

    private OpenAIVisionPromptAction() {
    }
}
