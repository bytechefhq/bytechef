
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

import static com.bytechef.hermes.definition.DefinitionDSL.array;
import static com.bytechef.hermes.definition.DefinitionDSL.bool;
import static com.bytechef.hermes.definition.DefinitionDSL.dateTime;

import static com.bytechef.hermes.definition.DefinitionDSL.integer;
import static com.bytechef.hermes.definition.DefinitionDSL.object;
import static com.bytechef.hermes.definition.DefinitionDSL.string;
import static com.bytechef.hermes.definition.DefinitionDSL.time;

/**
 * @author Ivica Cardic
 */
public class PipedriveNewActivityTrigger {

    public static final TriggerDefinition TRIGGER_DEFINITION = ComponentDSL.trigger("newActivity")
        .title("New Activity")
        .description("Trigger off whenever a new activity is added.")
        .type(TriggerDefinition.TriggerType.DYNAMIC_WEBHOOK)
        .outputSchema(
            object()
                .properties(
                    integer("id"),
                    integer("company_id"),
                    integer("user_id"),
                    bool("done"),
                    string("type"),
                    string("reference_type"),
                    integer("reference_id"),
                    string("conference_meeting_client"),
                    string("conference_meeting_url"),
                    string("conference_meeting_id"),
                    string("due_date"),
                    time("due_time"),
                    time("duration"),
                    bool("busy_flag"),
                    dateTime("add_time"),
                    dateTime("marked_as_done_time"),
                    dateTime("last_notification_time"),
                    integer("last_notification_user_id"),
                    integer("notification_language_id"),
                    string("subject"),
                    string("public_description"),
                    string("calendar_sync_include_context"),
                    string("location"),
                    integer("org_id"),
                    integer("person_id"),
                    integer("deal_id"),
                    string("lead_id"),
                    bool("active_flag"),
                    dateTime("update_time"),
                    integer("update_user_id"),
                    string("gcal_event_id"),
                    string("google_calendar_id"),
                    string("google_calendar_etag"),
                    string("source_timezone"),
                    string("rec_rule"),
                    string("rec_rule_extension"),
                    integer("rec_master_activity_id"),
                    array("series"),
                    string("note"),
                    integer("created_by_user_id"),
                    string("location_subpremise"),
                    string("location_street_number"),
                    string("location_route"),
                    string("location_sublocality"),
                    string("location_locality"),
                    string("location_admin_area_level_1"),
                    string("location_admin_area_level_2"),
                    string("location_country"),
                    string("location_postal_code"),
                    string("location_formatted_address"),
                    array("attendees").items(
                        object().properties(
                            string("email_address"),
                            integer("is_organizer"),
                            string("name"),
                            integer("person_id"),
                            string("status"),
                            string("user_id"))),
                    array("participants").items(
                        object().properties(
                            integer("person_id"),
                            bool("primary_flag"))),
                    string("org_name"),
                    string("person_name"),
                    string("deal_title"),
                    string("owner_name"),
                    string("person_dropbox_bcc"),
                    string("deal_dropbox_bcc"),
                    integer("assigned_to_user_id"),
                    object("file").properties(
                        string("id"),
                        string("clean_name"),
                        string("url"))))
        .exampleOutput("""
            {
                "id": 8,
                "company_id": 22122,
                "user_id": 1234,
                "done": false,
                "type": "deadline",
                "reference_type": "scheduler-service",
                "reference_id": 7,
                "conference_meeting_client": "871b8bc88d3a1202",
                "conference_meeting_url": "https://pipedrive.zoom.us/link",
                "conference_meeting_id": "01758746701",
                "due_date": "2020-06-09",
                "due_time": "10:00",
                "duration": "01:00",
                "busy_flag": true,
                "add_time": "2020-06-08 12:37:56",
                "marked_as_done_time": "2020-08-08 08:08:38",
                "last_notification_time": "2020-08-08 12:37:56",
                "last_notification_user_id": 7655,
                "notification_language_id": 1,
                "subject": "Deadline",
                "public_description": "This is a description",
                "calendar_sync_include_context": "",
                "location": "Mustamäe tee 3, Tallinn, Estonia",
                "org_id": 5,
                "person_id": 1101,
                "deal_id": 300,
                "lead_id": "46c3b0e1-db35-59ca-1828-4817378dff71",
                "project_id": null,
                "active_flag": true,
                "update_time": "2020-08-08 12:37:56",
                "update_user_id": 5596,
                "gcal_event_id": "",
                "google_calendar_id": "",
                "google_calendar_etag": "",
                "source_timezone": "",
                "rec_rule": "RRULE:FREQ=WEEKLY;BYDAY=WE",
                "rec_rule_extension": "",
                "rec_master_activity_id": 1,
                "series":
                [],
                "note": "A note for the activity",
                "created_by_user_id": 1234,
                "location_subpremise": "",
                "location_street_number": "3",
                "location_route": "Mustamäe tee",
                "location_sublocality": "Kristiine",
                "location_locality": "Tallinn",
                "location_admin_area_level_1": "Harju maakond",
                "location_admin_area_level_2": "",
                "location_country": "Estonia",
                "location_postal_code": "10616",
                "location_formatted_address": "Mustamäe tee 3, 10616 Tallinn, Estonia",
                "attendees":
                [
                    {
                        "email_address": "attendee@pipedrivemail.com",
                        "is_organizer": 0,
                        "name": "Attendee",
                        "person_id": 25312,
                        "status": "noreply",
                        "user_id": null
                    }
                ],
                "participants":
                [
                    {
                        "person_id": 17985,
                        "primary_flag": false
                    },
                    {
                        "person_id": 1101,
                        "primary_flag": true
                    }
                ],
                "org_name": "Organization",
                "person_name": "Person",
                "deal_title": "Deal",
                "owner_name": "Creator",
                "person_dropbox_bcc": "company@pipedrivemail.com",
                "deal_dropbox_bcc": "company+deal300@pipedrivemail.com",
                "assigned_to_user_id": 1235,
                "file":
                {
                    "id": "376892,",
                    "clean_name": "Audio 10:55:07.m4a",
                    "url": "https://pipedrive-files.s3-eu-west-1.amazonaws.com/Audio-recording.m4a"
                }
            }
            """)
        .dynamicWebhookEnable(PipedriveNewActivityTrigger::dynamicWebhookEnable)
        .dynamicWebhookDisable(PipedriveNewActivityTrigger::dynamicWebhookDisable)
        .dynamicWebhookRequest(PipedriveNewActivityTrigger::dynamicWebhookRequest);

    @SuppressWarnings("unchecked")
    private static WebhookOutput
        dynamicWebhookRequest(TriggerDefinition.DynamicWebhookRequestFunction.Context context) {
        TriggerDefinition.WebhookBody body = context.body();

        Map<String, Object> content = (Map<String, Object>) body.getContent();

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
                    connection.getBaseUri(), "activity", "added", context.webhookUrl())),
            null);
    }
}
