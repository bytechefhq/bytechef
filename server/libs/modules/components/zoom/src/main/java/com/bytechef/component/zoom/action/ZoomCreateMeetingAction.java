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

package com.bytechef.component.zoom.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.nullable;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.time;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDsl;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class ZoomCreateMeetingAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("createMeeting")
        .title("Create Meeting")
        .description("Creates a meeting.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/users/me/meetings", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(string("topic").metadata(
            Map.of(
                "type", PropertyType.BODY))
            .label("Topic")
            .description("The meeting's topic.")
            .required(true),
            number("duration").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Duration")
                .description("Duration of the meeting in minutes.")
                .required(false),
            string("auto_recording").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Auto Recording")
                .options(option("Local", "local"), option("Cloud", "cloud"), option("None", "none"))
                .required(false),
            string("audio").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Audio")
                .description("How participants join the audio portion of the meeting.")
                .required(false),
            string("agenda").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Agenda")
                .description("The meeting's agenda. This value has a maximum length of 2,000 characters.")
                .required(false),
            string("password").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Password")
                .description(
                    "The password required to join the meeting. By default, a password can only have a maximum length of 10 characters and only contain alphanumeric characters and the @, -, _, and * characters.")
                .required(false),
            object("settings").properties(string("schedule_for").label("Schedule For")
                .description("The email address or user ID of the user to schedule a meeting for.")
                .required(false),
                integer("approval_type").label("Approval Type")
                    .required(false))
                .metadata(
                    Map.of(
                        "type", PropertyType.BODY))
                .label("Settings")
                .required(false),
            string("join_url").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Join Url")
                .description("URL for participants to join the meeting.")
                .required(false))
        .output(outputSchema(object().properties(string("assistant_id").description("Unique ID of the assistant.")
            .required(false),
            string("host_email").description("Email of the host.")
                .required(false),
            integer("id").description("ID of the meeting.")
                .required(false),
            string("registration_url").description("URL for meeting registration.")
                .required(false),
            string("agenda").description("Agenda of the meeting.")
                .required(false),
            string("created_at").description("Creation time of the meeting.")
                .required(false),
            integer("duration").description("Duration of the meeting in minutes.")
                .required(false),
            string("encrypted_password").description("Encrypted meeting password.")
                .required(false),
            string("pstn_password").description("PSTN password for phone participants.")
                .required(false),
            string("h323_password").description("H.323/SIP room system password.")
                .required(false),
            string("join_url").description("URL to join the meeting.")
                .required(false),
            string("chat_join_url").description("Chat join URL for the meeting.")
                .required(false),
            array("occurrences")
                .items(object().properties(integer("duration").description("Duration of the occurrence.")
                    .required(false),
                    string("occurrence_id").description("ID of the occurrence.")
                        .required(false),
                    string("start_time").description("Start time of the occurrence.")
                        .required(false),
                    string("status").description("Status of the occurrence.")
                        .required(false)))
                .required(false),
            string("password").description("Password to join the meeting.")
                .required(false),
            string("pmi").description("Personal Meeting ID.")
                .required(false),
            bool("pre_schedule").description("Indicates if meeting is prescheduled.")
                .required(false),
            object("recurrence").properties(string("end_date_time").description("End date and time of recurrence.")
                .required(false),
                integer("end_times").description("Number of times the meeting occurs.")
                    .required(false),
                integer("monthly_day").required(false), integer("monthly_week").required(false),
                integer("monthly_week_day").required(false), integer("repeat_interval").required(false),
                integer("type").description("Recurrence type.")
                    .required(false),
                string("weekly_days").required(false))
                .required(false),
            object("settings").additionalProperties(
                array(), bool(), date(), dateTime(), integer(), nullable(), number(), object(), string(), time())
                .description("Meeting settings configuration.")
                .required(false),
            string("start_time").description("Scheduled start time.")
                .required(false),
            string("start_url").description("URL for host to start the meeting.")
                .required(false),
            string("timezone").description("Meeting timezone.")
                .required(false),
            string("topic").description("Meeting topic.")
                .required(false),
            array("tracking_fields")
                .items(object().properties(string("field").required(false), string("value").required(false),
                    bool("visible").required(false)))
                .required(false),
            integer("type").description("Type of meeting.")
                .required(false),
            string("dynamic_host_key").description("Dynamic host key for the meeting.")
                .required(false),
            string("creation_source").description("Source of creation (e.g., open_api).")
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private ZoomCreateMeetingAction() {
    }
}
