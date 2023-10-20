
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
import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookDisableConsumer.Context;
import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookEnableOutput;
import com.bytechef.hermes.component.definition.TriggerDefinition.WebhookOutput;

import java.util.Map;

import static com.bytechef.hermes.definition.DefinitionDSL.bool;
import static com.bytechef.hermes.definition.DefinitionDSL.date;
import static com.bytechef.hermes.definition.DefinitionDSL.dateTime;

import static com.bytechef.hermes.definition.DefinitionDSL.integer;
import static com.bytechef.hermes.definition.DefinitionDSL.object;
import static com.bytechef.hermes.definition.DefinitionDSL.string;
import static com.bytechef.hermes.definition.DefinitionDSL.time;

/**
 * @author Ivica Cardic
 */
public class PipedriveNewOrganizationTrigger {

    public static final TriggerDefinition TRIGGER_DEFINITION = ComponentDSL.trigger("newOrganization")
        .title("New Organization")
        .description("Trigger off whenever a new organization is added.")
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
                    string("name"),
                    integer("open_deals_count"),
                    integer("related_open_deals_count"),
                    integer("closed_deals_count"),
                    integer("related_closed_deals_count"),
                    integer("email_messages_count"),
                    integer("people_count"),
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
                    string("country_code"),
                    string("first_char"),
                    dateTime("update_time"),
                    dateTime("add_time"),
                    string("visible_to"),
                    date("next_activity_date"),
                    time("next_activity_time"),
                    integer("next_activity_id"),
                    integer("last_activity_id"),
                    date("last_activity_date"),
                    integer("label"),
                    string("address"),
                    string("address_subpremise"),
                    string("address_street_number"),
                    string("address_route"),
                    string("address_sublocality"),
                    string("address_locality"),
                    string("address_admin_area_level_1"),
                    string("address_admin_area_level_2"),
                    string("address_country"),
                    string("address_postal_code"),
                    string("address_formatted_address"),
                    string("owner_name"),
                    string("cc_email")))
        .exampleOutput(
            """
                {
                    "id": 1,
                    "company_id": 77,
                    "owner_id":
                    {
                        "id": 10,
                        "name": "Will Smith",
                        "email": "will.smith@pipedrive.com",
                        "has_pic": 0,
                        "pic_hash": "2611ace8ac6a3afe2f69ed56f9e08c6b",
                        "active_flag": true,
                        "value": 10
                    },
                    "name": "Bolt",
                    "open_deals_count": 1,
                    "related_open_deals_count": 2,
                    "closed_deals_count": 3,
                    "related_closed_deals_count": 1,
                    "email_messages_count": 2,
                    "people_count": 1,
                    "activities_count": 2,
                    "done_activities_count": 1,
                    "undone_activities_count": 0,
                    "files_count": 0,
                    "notes_count": 0,
                    "followers_count": 1,
                    "won_deals_count": 0,
                    "related_won_deals_count": 0,
                    "lost_deals_count": 0,
                    "related_lost_deals_count": 0,
                    "active_flag": true,
                    "picture_id":
                    {
                        "item_type": "person",
                        "item_id": 25,
                        "active_flag": true,
                        "add_time": "2020-09-08 08:17:52",
                        "update_time": "0000-00-00 00:00:00",
                        "added_by_user_id": 967055,
                        "pictures":
                        {
                            "128": "https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cf14ac8f269_128.jpg",
                            "512": "https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb2ac8f269_512.jpg"
                        },
                        "value": 101
                    },
                    "country_code": "USA",
                    "first_char": "b",
                    "update_time": "2020-09-08 12:14:11",
                    "add_time": "2020-02-25 10:04:08",
                    "visible_to": "3",
                    "next_activity_date": "2019-11-29",
                    "next_activity_time": "11:30:00",
                    "next_activity_id": 128,
                    "last_activity_id": 34,
                    "last_activity_date": "2019-11-28",
                    "label": 7,
                    "address": "Mustamäe tee 3a, 10615 Tallinn",
                    "address_subpremise": "",
                    "address_street_number": "3a",
                    "address_route": "Mustamäe tee",
                    "address_sublocality": "Kristiine",
                    "address_locality": "Tallinn",
                    "address_admin_area_level_1": "Harju maakond",
                    "address_admin_area_level_2": "",
                    "address_country": "Estonia",
                    "address_postal_code": "10616",
                    "address_formatted_address": "Mustamäe tee 3a, 10616 Tallinn, Estonia",
                    "owner_name": "John Doe",
                    "cc_email": "org@pipedrivemail.com"
                }
                """)
        .dynamicWebhookEnable(PipedriveNewOrganizationTrigger::dynamicWebhookEnable)
        .dynamicWebhookDisable(PipedriveNewOrganizationTrigger::dynamicWebhookDisable)
        .dynamicWebhookRequest(PipedriveNewOrganizationTrigger::dynamicWebhookRequest);

    @SuppressWarnings("unchecked")
    private static WebhookOutput
        dynamicWebhookRequest(TriggerDefinition.DynamicWebhookRequestFunction.Context context) {
        TriggerDefinition.WebhookBody body = context.body();

        Map<String, Object> content = (Map<String, Object>) body.getContent();
        new WebhookOutput.MapOutput(Map.of());
        return WebhookOutput.map((Map<String, Object>) content.get("current"));
    }

    private static void dynamicWebhookDisable(Context context) {
        Connection connection = context.connection();
        DynamicWebhookEnableOutput enableOutput = context.dynamicWebhookEnableOutput();

        PipedriveUtils.unsubscribeWebhook(connection.getBaseUri(), (String) enableOutput.getParameter("id"));
    }

    private static DynamicWebhookEnableOutput
        dynamicWebhookEnable(TriggerDefinition.DynamicWebhookEnableFunction.Context context) {
        Connection connection = context.connection();

        return new DynamicWebhookEnableOutput(
            Map.of(
                "id",
                PipedriveUtils.subscribeWebhook(
                    connection.getBaseUri(), "organization", "added", context.webhookUrl())),
            null);
    }
}
