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

package com.bytechef.component.sendgrid.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.CUSTOM_UNSUBSCRIBE_URL;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.HTML_CONTENT;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.LIST_IDS;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.NAME;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.SENDER_ID;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.SUBJECT;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.SUPPRESSION_GROUP_ID;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.sendgrid.util.SendgridUtils;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class SendgridCreateSingleSendAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createSingleSend")
        .title("Create Single Send")
        .description(
            "Creates a draft Marketing Campaigns Single Send. Use Schedule Single Send to send it.")
        .properties(
            string(NAME)
                .label("Name")
                .description("The name of the Single Send.")
                .minLength(1)
                .maxLength(100)
                .required(true),
            string(SUBJECT)
                .label("Subject")
                .description("The subject line of the email.")
                .required(true),
            string(HTML_CONTENT)
                .label("HTML Content")
                .description("The HTML content of the email.")
                .controlType(ControlType.RICH_TEXT)
                .required(true),
            integer(SENDER_ID)
                .label("Sender ID")
                .description("The ID of the verified sender the email is sent from.")
                .options((OptionsFunction<Long>) SendgridUtils::getSenderIdOptions)
                .required(true),
            array(LIST_IDS)
                .label("List IDs")
                .description("The IDs of the contact lists the Single Send is sent to.")
                .items(string())
                .options((OptionsFunction<String>) SendgridUtils::getListIdOptions)
                .maxItems(50)
                .required(true),
            integer(SUPPRESSION_GROUP_ID)
                .label("Unsubscribe Group ID")
                .description(
                    "The ID of the unsubscribe group recipients can opt out of. Scheduling requires either an " +
                        "unsubscribe group or a custom unsubscribe URL.")
                .options((OptionsFunction<Long>) SendgridUtils::getSuppressionGroupIdOptions)
                .required(false),
            string(CUSTOM_UNSUBSCRIBE_URL)
                .label("Custom Unsubscribe URL")
                .description("The URL of your own unsubscribe page, used instead of an unsubscribe group.")
                .controlType(ControlType.URL)
                .required(false)
                .advancedOption(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("id")
                            .description("The ID of the Single Send. Pass it to Schedule Single Send."),
                        string("name")
                            .description("The name of the Single Send."),
                        string("status")
                            .description("The status of the Single Send: draft, scheduled or triggered."),
                        string("send_at")
                            .description("The scheduled send time, when one is set."),
                        object("send_to")
                            .properties(
                                array("list_ids")
                                    .items(string())
                                    .description("The IDs of the contact lists the Single Send is sent to.")),
                        object("email_config")
                            .properties(
                                string("subject")
                                    .description("The subject line of the email."),
                                string("html_content")
                                    .description("The HTML content of the email."),
                                integer("sender_id")
                                    .description("The ID of the sender."),
                                integer("suppression_group_id")
                                    .description("The ID of the unsubscribe group."),
                                string("custom_unsubscribe_url")
                                    .description("The custom unsubscribe URL.")),
                        string("created_at")
                            .description("The time the Single Send was created."),
                        string("updated_at")
                            .description("The time the Single Send was last updated."))))
        .help("", "https://docs.bytechef.io/reference/components/sendgrid_v1#create-single-send")
        .perform(SendgridCreateSingleSendAction::perform);

    private SendgridCreateSingleSendAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        Map<String, Object> emailConfig = new HashMap<>();

        emailConfig.put("subject", inputParameters.getRequiredString(SUBJECT));
        emailConfig.put("html_content", inputParameters.getRequiredString(HTML_CONTENT));
        emailConfig.put("sender_id", inputParameters.getRequiredLong(SENDER_ID));

        Long suppressionGroupId = inputParameters.getLong(SUPPRESSION_GROUP_ID);

        if (suppressionGroupId != null) {
            emailConfig.put("suppression_group_id", suppressionGroupId);
        }

        String customUnsubscribeUrl = inputParameters.getString(CUSTOM_UNSUBSCRIBE_URL);

        if (customUnsubscribeUrl != null) {
            emailConfig.put("custom_unsubscribe_url", customUnsubscribeUrl);
        }

        return context.http(http -> http.post("/marketing/singlesends"))
            .body(
                Body.of(
                    "name", inputParameters.getRequiredString(NAME),
                    "send_to", Map.of("list_ids", inputParameters.getRequiredList(LIST_IDS, String.class)),
                    "email_config", emailConfig))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
