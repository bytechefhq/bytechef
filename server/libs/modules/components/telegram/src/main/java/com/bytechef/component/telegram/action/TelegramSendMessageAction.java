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

package com.bytechef.component.telegram.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.telegram.constant.TelegramConstants.CHAT_ID;
import static com.bytechef.component.telegram.constant.TelegramConstants.DIRECT_MESSAGES_TOPIC_ID;
import static com.bytechef.component.telegram.constant.TelegramConstants.MESSAGE_OUTPUT_PROPERTIES;
import static com.bytechef.component.telegram.constant.TelegramConstants.TEXT;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;

/**
 * @author Monika Kušter
 */
public class TelegramSendMessageAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("sendMessage")
        .title("Send Message")
        .description("Sends a message through a Telegram bot.")
        .properties(
            string(CHAT_ID)
                .label("Chat ID")
                .description("Unique identifier for the target chat or username of the target channel.")
                .required(true),
            string(TEXT)
                .label("Text")
                .description("Text of the message to be sent.")
                .maxLength(4096)
                .controlType(ControlType.TEXT_AREA)
                .required(true),
            string(DIRECT_MESSAGES_TOPIC_ID)
                .label("Direct Messages Topic ID")
                .description(
                    "Identifier of the direct messages topic to which the message will be sent; required if the " +
                        "message is sent to a direct messages chat.")
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        bool("ok"),
                        object("result")
                            .properties(MESSAGE_OUTPUT_PROPERTIES))))
        .perform(TelegramSendMessageAction::perform);

    private TelegramSendMessageAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context.http(http -> http.post("/sendMessage"))
            .body(
                Http.Body.of(
                    CHAT_ID, inputParameters.getRequiredString(CHAT_ID),
                    TEXT, inputParameters.getRequiredString(TEXT),
                    DIRECT_MESSAGES_TOPIC_ID, inputParameters.getString(DIRECT_MESSAGES_TOPIC_ID)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody();
    }
}
