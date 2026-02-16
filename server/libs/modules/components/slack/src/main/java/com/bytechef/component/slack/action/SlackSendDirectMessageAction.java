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

package com.bytechef.component.slack.action;

import static com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.slack.constant.SlackConstants.CHANNEL;
import static com.bytechef.component.slack.constant.SlackConstants.CHAT_POST_MESSAGE_RESPONSE_PROPERTY;
import static com.bytechef.component.slack.constant.SlackConstants.POST_AT;
import static com.bytechef.component.slack.constant.SlackConstants.TEXT;
import static com.bytechef.component.slack.constant.SlackConstants.TEXT_PROPERTY;
import static com.bytechef.component.slack.util.SlackSendMessageUtils.sendMessage;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.slack.util.SlackUtils;

/**
 * @author Mario Cvjetojevic
 * @author Monika Kušter
 */
public class SlackSendDirectMessageAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("sendDirectMessage")
        .title("Send Direct Message")
        .description(
            "Sends a direct message to another user in a workspace. If it hasn't already, a direct message " +
                "conversation will be created.")
        .properties(
            string(CHANNEL)
                .label("User ID")
                .description("ID of the user to send the direct message to.")
                .options((OptionsFunction<String>) SlackUtils::getUserIdOptions)
                .required(true),
            dateTime(POST_AT)
                .label("Post at")
                .description("Date and time when the message should be sent.")
                .required(false),
            TEXT_PROPERTY)
        .output(outputSchema(CHAT_POST_MESSAGE_RESPONSE_PROPERTY))
        .help("", "https://docs.bytechef.io/reference/components/slack_v1#send-direct-message")
        .perform(SlackSendDirectMessageAction::perform);

    private SlackSendDirectMessageAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return sendMessage(
            inputParameters.getRequiredString(CHANNEL),
            inputParameters.getRequiredString(TEXT),
            inputParameters.getLocalDateTime(POST_AT),
            null,
            context);
    }
}
