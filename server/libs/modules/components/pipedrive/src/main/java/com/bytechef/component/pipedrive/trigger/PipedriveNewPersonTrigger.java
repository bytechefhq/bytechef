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

package com.bytechef.component.pipedrive.trigger;

import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.date;
import static com.bytechef.component.definition.ComponentDSL.dateTime;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.ComponentDSL.time;

import com.bytechef.component.definition.ComponentDSL;
import com.bytechef.component.definition.ComponentDSL.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition;
import com.bytechef.component.definition.TriggerDefinition.DynamicWebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.pipedrive.util.PipedriveUtils;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@SuppressWarnings("PMD.UnusedFormalParameter")
public class PipedriveNewPersonTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = ComponentDSL.trigger("newPerson")
        .title("New Person")
        .description("Trigger off whenever a new person is added.")
        .type(TriggerDefinition.TriggerType.DYNAMIC_WEBHOOK)
        .outputSchema(
            object()
                .properties(
                    integer("id"),
                    integer("company_id"),
                    object("owner_id").properties(
                        integer("id"),
                        string("name"),
                        string("email"),
                        integer("has_pic"),
                        string("pic_hash"),
                        bool("active_flag"),
                        integer("value")),
                    object("org_id").properties(
                        integer("id"),
                        integer("people_count"),
                        integer("owner_id"),
                        string("address"),
                        bool("active_flag"),
                        string("cc_email"),
                        integer("value")),
                    string("name"),
                    string("first_name"),
                    string("last_name"),
                    integer("open_deals_count"),
                    integer("related_open_deals_count"),
                    integer("closed_deals_count"),
                    integer("related_closed_deals_count"),
                    integer("participant_open_deals_count"),
                    integer("participant_closed_deals_count"),
                    integer("email_messages_count"),
                    integer("activities_count"),
                    integer("done_activities_count"),
                    integer("undone_activities_count"),
                    integer("files_count"),
                    integer("notes_count"),
                    integer("followers_count"),
                    integer("won_deals_count"),
                    integer("related_won_deals_count"),
                    integer("lost_deals_count"),
                    integer("related_lost_deals_count"),
                    bool("active_flag"),
                    array("phone").items(
                        object().properties(
                            string("label"),
                            string("value"),
                            bool("primary"))),
                    array("email").items(
                        object().properties(
                            string("label"),
                            string("value"),
                            bool("primary"))),
                    string("primary_email"),
                    string("first_char"),
                    dateTime("update_time"),
                    dateTime("add_time"),
                    string("visible_to"),
                    string("marketing_status"),
                    object("picture_id").properties(
                        string("item_type"),
                        integer("item_id"),
                        bool("active_flag"),
                        dateTime("add_time"),
                        dateTime("update_time"),
                        integer("added_by_user_id"),
                        object("pictures").properties(
                            string("128"),
                            string("512")),
                        integer("value")),
                    date("next_activity_date"),
                    time("next_activity_time"),
                    integer("next_activity_id"),
                    integer("last_activity_id"),
                    date("last_activity_date"),
                    dateTime("last_incoming_mail_time"),
                    dateTime("last_outgoing_mail_time"),
                    integer("label"),
                    string("org_name"),
                    string("owner_name"),
                    string("cc_email")))
        .sampleOutput(
            """
                {
                    "id": 1,
                    "company_id": 12,
                    "owner_id": {
                      "id": 123,
                      "name": "Jane Doe",
                      "email": "jane@pipedrive.com",
                      "has_pic": 1,
                      "pic_hash": "2611ace8ac6a3afe2f69ed56f9e08c6b",
                      "active_flag": true,
                      "value": 123
                    },
                    "org_id": {
                      "name": "Org Name",
                      "people_count": 1,
                      "owner_id": 123,
                      "address": "Mustamäe tee 3a, 10615 Tallinn",
                      "active_flag": true,
                      "cc_email": "org@pipedrivemail.com",
                      "value": 1234
                    },
                    "name": "Will Smith",
                    "first_name": "Will",
                    "last_name": "Smith",
                    "open_deals_count": 2,
                    "related_open_deals_count": 2,
                    "closed_deals_count": 3,
                    "related_closed_deals_count": 3,
                    "participant_open_deals_count": 1,
                    "participant_closed_deals_count": 1,
                    "email_messages_count": 1,
                    "activities_count": 1,
                    "done_activities_count": 1,
                    "undone_activities_count": 2,
                    "files_count": 2,
                    "notes_count": 2,
                    "followers_count": 3,
                    "won_deals_count": 3,
                    "related_won_deals_count": 3,
                    "lost_deals_count": 1,
                    "related_lost_deals_count": 1,
                    "active_flag": true,
                    "phone": [
                      {
                        "value": "12345",
                        "primary": true,
                        "label": "work"
                      }
                    ],
                    "email": [
                      {
                        "value": "12345@email.com",
                        "primary": true,
                        "label": "work"
                      }
                    ],
                    "primary_email": "12345@email.com",
                    "first_char": "w",
                    "update_time": "2020-05-08 05:30:20",
                    "add_time": "2017-10-18 13:23:07",
                    "visible_to": "3",
                    "marketing_status": "no_consent",
                    "picture_id": {
                      "item_type": "person",
                      "item_id": 25,
                      "active_flag": true,
                      "add_time": "2020-09-08 08:17:52",
                      "update_time": "0000-00-00 00:00:00",
                      "added_by_user_id": 967055,
                      "pictures": {
                        "128": "https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb4ac8f269_128.jpg",
                        "512": "https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb4ac8f269_512.jpg"
                      },
                      "value": 4
                    },
                    "next_activity_date": "2019-11-29",
                    "next_activity_time": "11:30:00",
                    "next_activity_id": 128,
                    "last_activity_id": 34,
                    "last_activity_date": "2019-11-28",
                    "last_incoming_mail_time": "2019-05-29 18:21:42",
                    "last_outgoing_mail_time": "2019-05-30 03:45:35",
                    "label": 1,
                    "org_name": "Organization name",
                    "owner_name": "Jane Doe",
                    "cc_email": "org@pipedrivemail.com"
                  }
                """)
        .dynamicWebhookDisable(PipedriveNewPersonTrigger::dynamicWebhookDisable)
        .dynamicWebhookEnable(PipedriveNewPersonTrigger::dynamicWebhookEnable)
        .dynamicWebhookRequest(PipedriveNewPersonTrigger::dynamicWebhookRequest);

    protected static void dynamicWebhookDisable(
        Map<String, ?> inputParameters, Parameters connectionParameters, Map<String, ?> outputParameters,
        String workflowExecutionId, TriggerContext context) {

        PipedriveUtils.unsubscribeWebhook((String) outputParameters.get("id"), context);
    }

    protected static DynamicWebhookEnableOutput dynamicWebhookEnable(
        Map<String, ?> inputParameters, Parameters connectionParameters, String webhookUrl,
        String workflowExecutionId, TriggerContext context) {

        return new DynamicWebhookEnableOutput(
            Map.of("id", PipedriveUtils.subscribeWebhook("person", "added", webhookUrl, context)), null);
    }

    @SuppressWarnings("unchecked")
    protected static Map<String, ?> dynamicWebhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, DynamicWebhookEnableOutput output, TriggerContext context) {

        return (Map<String, ?>) body.getContent(new TypeReference<Map<String, ?>>() {})
            .get("current");
    }
}
