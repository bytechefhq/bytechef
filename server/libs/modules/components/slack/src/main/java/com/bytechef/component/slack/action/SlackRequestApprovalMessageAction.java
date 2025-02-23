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

package com.bytechef.component.slack.action;

import static com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.slack.constant.SlackConstants.CHANNEL;
import static com.bytechef.component.slack.constant.SlackConstants.CHAT_POST_MESSAGE_RESPONSE_PROPERTY;
import static com.bytechef.component.slack.constant.SlackConstants.TEXT;
import static com.bytechef.component.slack.constant.SlackConstants.TEXT_PROPERTY;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionContext.Approval;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.slack.util.SlackUtils;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class SlackRequestApprovalMessageAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("requestApprovalMessage")
        .title("Request Approval in a Channel")
        .description("Send approval message to a channel and then wait until the message is approved or disapproved.")
        .properties(
            string(CHANNEL)
                .label("Channel")
                .description("Channel, private group, or IM channel to send message to.")
                .options((ActionOptionsFunction<String>) SlackUtils::getChannelOptions)
                .required(true),
            TEXT_PROPERTY)
        .output(outputSchema(CHAT_POST_MESSAGE_RESPONSE_PROPERTY))
        .perform(SlackRequestApprovalMessageAction::perform);

    private SlackRequestApprovalMessageAction() {
    }

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        Approval.Links links = actionContext.approval(Approval::generateLinks);

        return actionContext
            .http(http -> http.post("/chat.postMessage"))
            .body(
                Http.Body.of(
                    CHANNEL, inputParameters.getRequiredString(CHANNEL),
                    TEXT,
                    inputParameters.getRequiredString(TEXT)
                        + "\n\n Approve: ${approvalLink}\n\n Disapprove: ${disapprovalLink}",
                    "blocks", List.of(
                        Map.of("type", "section", TEXT,
                            Map.of("type", "mrkdwn", TEXT, inputParameters.getRequiredString(TEXT))),
                        Map.of("type", "actions", "block_id", "actions", "elements", List.of(
                            Map.of("type", "button", "text", Map.of("type", "plain_text", "text", "Approve"), "style",
                                "primary", "url", approval.approvalLink()),
                            Map.of("type", "button", "text", Map.of("type", "plain_text", "text", "Disapprove"),
                                "style", "danger", "url", approval.disapprovalLink()))))))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
