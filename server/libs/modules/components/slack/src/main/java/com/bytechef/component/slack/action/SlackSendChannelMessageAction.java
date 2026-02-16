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
public class SlackSendChannelMessageAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("sendChannelMessage")
        .title("Send Channel Message")
        .description("Sends a message to a public or private channel.")
        .properties(
            string(CHANNEL)
                .label("Channel ID")
                .description("ID of the channel, private group, or IM channel to send message to.")
                .options((OptionsFunction<String>) SlackUtils::getChannelIdOptions)
                .required(true),
            dateTime(POST_AT)
                .label("Post at")
                .description("Date and time when the message should be sent.")
                .required(false),
            TEXT_PROPERTY)
        .output(outputSchema(CHAT_POST_MESSAGE_RESPONSE_PROPERTY))
        .help("", "https://docs.bytechef.io/reference/components/slack_v1#send-channel-message")
        .perform(SlackSendChannelMessageAction::perform);

    private SlackSendChannelMessageAction() {
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
