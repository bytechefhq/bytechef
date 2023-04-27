
/*
 * Copyright 2021 <your company/name>.
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

import com.bytechef.component.pipedrive.util.PipedriveUtils;
import com.bytechef.hermes.component.Context.Connection;
import com.bytechef.hermes.component.definition.ComponentDSL;
import com.bytechef.hermes.component.definition.TriggerDefinition;
import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookDisableContext;
import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookEnableOutput;
import com.bytechef.hermes.component.definition.TriggerDefinition.WebhookOutput;

import java.util.Map;

import static com.bytechef.hermes.definition.DefinitionDSL.array;
import static com.bytechef.hermes.definition.DefinitionDSL.bool;
import static com.bytechef.hermes.definition.DefinitionDSL.date;
import static com.bytechef.hermes.definition.DefinitionDSL.dateTime;

import static com.bytechef.hermes.definition.DefinitionDSL.integer;
import static com.bytechef.hermes.definition.DefinitionDSL.object;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

/**
 * @author Ivica Cardic
 */
public class PipedriveNewDealTrigger {

    public static final TriggerDefinition TRIGGER_DEFINITION = ComponentDSL.trigger("newDeal")
        .title("New Deal")
        .description("Trigger off whenever a new deal is added.")
        .type(TriggerDefinition.TriggerType.DYNAMIC_WEBHOOK)
        .outputSchema(
            object()
                .properties(
                    integer("id"),
                    object("creator_user_id").properties(
                        integer("id"),
                        string("name"),
                        string("email"),
                        bool("has_pic"),
                        string("pic_hash"),
                        bool("active_flag"),
                        integer("value")),
                    object("user_id").properties(
                        integer("id"),
                        string("name"),
                        string("email"),
                        bool("has_pic"),
                        string("pic_hash"),
                        bool("active_flag"),
                        integer("value")),
                    object("person_id").properties(
                        bool("active_flag"),
                        string("name"),
                        array("email").items(
                            object().properties(
                                string("label"),
                                string("value"),
                                bool("primary"))),
                        array("phone").items(
                            object().properties(
                                string("label"),
                                string("value"),
                                bool("primary"))),
                        integer("value")),
                    object("org_id").properties(
                        string("name"),
                        integer("people_count"),
                        integer("owner_id"),
                        string("address"),
                        bool("active_flag"),
                        string("cc_email"),
                        integer("value")),
                    integer("stage_id"),
                    string("title"),
                    integer("value"),
                    string("currency"),
                    dateTime("add_time"),
                    dateTime("update_time"),
                    dateTime("stage_change_time"),
                    bool("active"),
                    bool("deleted"),
                    string("status"),
                    integer("probability"),
                    date("next_activity_date"),
                    string("next_activity_time"),
                    integer("next_activity_id"),
                    integer("last_activity_id"),
                    date("last_activity_date"),
                    string("lost_reason"),
                    string("visible_to"),
                    string("close_time"),
                    integer("pipeline_id"),
                    dateTime("won_time"),
                    dateTime("first_won_time"),
                    string("lost_time"),
                    integer("products_count"),
                    integer("files_count"),
                    integer("notes_count"),
                    integer("followers_count"),
                    integer("email_messages_count"),
                    integer("activities_count"),
                    integer("done_activities_count"),
                    integer("undone_activities_count"),
                    integer("participants_count"),
                    date("expected_close_date"),
                    dateTime("last_incoming_mail_time"),
                    dateTime("last_outgoing_mail_time"),
                    integer("label"),
                    integer("stage_order_nr"),
                    string("person_name"),
                    string("org_name"),
                    string("next_activity_subject"),
                    string("next_activity_type"),
                    string("next_activity_duration"),
                    string("next_activity_note"),
                    string("formatted_value"),
                    integer("weighted_value"),
                    string("formatted_weighted_value"),
                    string("weighted_value_currency"),
                    string("rotten_time"),
                    string("owner_name"),
                    string("cc_email"),
                    string("org_hidden"),
                    bool("person_hidden")))
        .exampleOutput("""
            {
                "id": 1,
                "creator_user_id":
                {
                    "id": 8877,
                    "name": "Creator",
                    "email": "john.doe@pipedrive.com",
                    "has_pic": false,
                    "pic_hash": null,
                    "active_flag": true,
                    "value": 8877
                },
                "user_id":
                {
                    "id": 8877,
                    "name": "Creator",
                    "email": "john.doe@pipedrive.com",
                    "has_pic": false,
                    "pic_hash": null,
                    "active_flag": true,
                    "value": 8877
                },
                "person_id":
                {
                    "active_flag": true,
                    "name": "Person",
                    "email":
                    [
                        {
                            "label": "work",
                            "value": "person@pipedrive.com",
                            "primary": true
                        }
                    ],
                    "phone":
                    [
                        {
                            "label": "work",
                            "value": "37244499911",
                            "primary": true
                        }
                    ],
                    "value": 1101
                },
                "org_id":
                {
                    "name": "Organization",
                    "people_count": 2,
                    "owner_id": 8877,
                    "address": "",
                    "active_flag": true,
                    "cc_email": "org@pipedrivemail.com",
                    "value": 5
                },
                "stage_id": 2,
                "title": "Deal One",
                "value": 5000,
                "currency": "EUR",
                "add_time": "2019-05-29 04:21:51",
                "update_time": "2019-11-28 16:19:50",
                "stage_change_time": "2019-11-28 15:41:22",
                "active": true,
                "deleted": false,
                "status": "open",
                "probability": null,
                "next_activity_date": "2019-11-29",
                "next_activity_time": "11:30:00",
                "next_activity_id": 128,
                "last_activity_id": null,
                "last_activity_date": null,
                "lost_reason": null,
                "visible_to": "1",
                "close_time": null,
                "pipeline_id": 1,
                "won_time": "2019-11-27 11:40:36",
                "first_won_time": "2019-11-27 11:40:36",
                "lost_time": "",
                "products_count": 0,
                "files_count": 0,
                "notes_count": 2,
                "followers_count": 0,
                "email_messages_count": 4,
                "activities_count": 1,
                "done_activities_count": 0,
                "undone_activities_count": 1,
                "participants_count": 1,
                "expected_close_date": "2019-06-29",
                "last_incoming_mail_time": "2019-05-29 18:21:42",
                "last_outgoing_mail_time": "2019-05-30 03:45:35",
                "label": "11",
                "stage_order_nr": 2,
                "person_name": "Person",
                "org_name": "Organization",
                "next_activity_subject": "Call",
                "next_activity_type": "call",
                "next_activity_duration": "00:30:00",
                "next_activity_note": "Note content",
                "formatted_value": "€5,000",
                "weighted_value": 5000,
                "formatted_weighted_value": "€5,000",
                "weighted_value_currency": "EUR",
                "rotten_time": null,
                "owner_name": "Creator",
                "cc_email": "company+deal1@pipedrivemail.com",
                "org_hidden": false,
                "person_hidden": false
            }
            """)
        .dynamicWebhookEnable(PipedriveNewDealTrigger::dynamicWebhookEnable)
        .dynamicWebhookDisable(PipedriveNewDealTrigger::dynamicWebhookDisable)
        .dynamicWebhookRequest(PipedriveNewDealTrigger::dynamicWebhookRequest);

    @SuppressWarnings("unchecked")
    private static WebhookOutput
        dynamicWebhookRequest(TriggerDefinition.DynamicWebhookRequestContext context) {
        TriggerDefinition.WebhookBody body = context.body();

        Map<String, Object> content = (Map<String, Object>) body.getContent();

        return WebhookOutput.map((Map<String, Object>) content.get("current"));
    }

    private static void dynamicWebhookDisable(DynamicWebhookDisableContext context) {
        Connection connection = context.connection();
        DynamicWebhookEnableOutput enableOutput = context.dynamicWebhookEnableOutput();

        PipedriveUtils.unsubscribeWebhook(connection.getBaseUri(), (String) enableOutput.getParameter("id"));
    }

    private static DynamicWebhookEnableOutput
        dynamicWebhookEnable(TriggerDefinition.DynamicWebhookEnableContext context) {
        Connection connection = context.connection();

        return new DynamicWebhookEnableOutput(
            Map.of(
                "id",
                PipedriveUtils.subscribeWebhook(
                    connection.getBaseUri(), "deal", "added", context.webhookUrl())),
            null);
    }
}
