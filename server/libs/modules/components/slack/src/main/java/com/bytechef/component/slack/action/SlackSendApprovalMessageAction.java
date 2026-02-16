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
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.slack.constant.SlackConstants.CHANNEL;
import static com.bytechef.component.slack.constant.SlackConstants.CHAT_POST_MESSAGE_RESPONSE_PROPERTY;
import static com.bytechef.component.slack.constant.SlackConstants.TEXT;
import static com.bytechef.component.slack.constant.SlackConstants.TEXT_PROPERTY;
import static com.bytechef.component.slack.constant.SlackConstants.TYPE;
import static com.bytechef.component.slack.util.SlackSendMessageUtils.sendMessage;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionContext.Approval;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.slack.util.SlackUtils;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class SlackSendApprovalMessageAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("sendApprovalMessage")
        .title("Send Approval Message")
        .description("Sends approval message to a channel.")
        .properties(
            string(CHANNEL)
                .label("Channel")
                .description("Channel, private group, or IM channel to send message to.")
                .options((OptionsFunction<String>) SlackUtils::getChannelIdOptions)
                .required(true),
            TEXT_PROPERTY)
        .output(outputSchema(CHAT_POST_MESSAGE_RESPONSE_PROPERTY))
        .help("", "https://docs.bytechef.io/reference/components/slack_v1#send-approval-message")
        .perform(SlackSendApprovalMessageAction::perform);

    private SlackSendApprovalMessageAction() {
    }

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        Approval.Links links = actionContext.approval(Approval::generateLinks);
        String text = "%s%n%n Approve: ${approvalLink}%n%n Disapprove: ${disapprovalLink}"
            .formatted(inputParameters.getRequiredString(TEXT));
        List<Map<String, Object>> blocks = List.of(
            Map.of(
                TYPE, "section", TEXT,
                Map.of(TYPE, "mrkdwn", TEXT, inputParameters.getRequiredString(TEXT))),
            Map.of(
                TYPE, "actions", "block_id", "actions", "elements",
                List.of(
                    Map.of(
                        TYPE, "button", TEXT, Map.of(TYPE, "plain_text", TEXT, "Approve"),
                        "style", "primary", "url", links.approvalLink()),
                    Map.of(
                        TYPE, "button", TEXT, Map.of(TYPE, "plain_text", TEXT, "Disapprove"),
                        "style", "danger", "url", links.disapprovalLink()))));

        return sendMessage(inputParameters.getRequiredString(CHANNEL), text, null, blocks, actionContext);
    }
}
