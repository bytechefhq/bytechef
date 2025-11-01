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
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.zoom.util.ZoomUtils;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class ZoomAddMeetingRegistrantAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("addMeetingRegistrant")
        .title("Add Meeting Registrant")
        .description("Create and submit a user's registration to a meeting.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/meetings/{meetingId}/registrants", "bodyContentType", BodyContentType.JSON, "mimeType",
                "application/json"

            ))
        .properties(integer("meetingId").label("Meeting ID")
            .description("ID of the meeting where the registrant will be added.")
            .required(true)
            .options((ActionDefinition.OptionsFunction<Long>) ZoomUtils::getMeetingIdOptions)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)),
            string("first_name").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("First Name")
                .description("First name of the registrant.")
                .required(true),
            string("last_name").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Last Name")
                .description("Last name of the registrant.")
                .required(false),
            string("email").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Email")
                .description("Email of the registrant.")
                .required(true),
            string("address").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Address")
                .description("Address of the registrant.")
                .required(false),
            string("city").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("City")
                .description("City of the registrant.")
                .required(false),
            string("state").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("State")
                .required(false),
            string("zip").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Zip")
                .required(false),
            string("country").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Country")
                .description("Country of the registrant.")
                .required(false),
            string("phone").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Phone")
                .description("Phone number of the registrant.")
                .required(false),
            string("comments").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Comments")
                .description("Additional comment about the registrant.")
                .required(false),
            string("industry").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Industry")
                .required(false),
            string("job_title").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Job Title")
                .required(false),
            string("no_of_employees").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Number of Employees")
                .options(option("1-20", "1-20"), option("21-50", "21-50"), option("51-100", "51-100"),
                    option("101-500", "101-500"), option("501-1,000", "501-1,000"),
                    option("1,001-5,000", "1,001-5,000"), option("5,001-10,000", "5,001-10,000"),
                    option("More than 10,000", "More than 10,000"))
                .required(false),
            string("org").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Organization")
                .required(false),
            string("purchasing_time_frame").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Purchasing Time Frame")
                .options(option("Within a month", "Within a month"), option("1-3 months", "1-3 months"),
                    option("4-6 months", "4-6 months"), option("More than 6 months", "More than 6 months"),
                    option("No timeframe", "No timeframe"))
                .required(false),
            string("role_in_purchase_process").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Role In Purchase Process")
                .options(option("Decision Maker", "Decision Maker"),
                    option("Evaluator/Recommender", "Evaluator/Recommender"), option("Influencer", "Influencer"),
                    option("Not involved", "Not involved"))
                .required(false),
            string("language").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Language")
                .required(false),
            bool("auto_approve").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Auto Approve")
                .required(false))
        .output(outputSchema(object().properties(integer("id").description("ID of the meeting.")
            .required(false),
            string("join_url").description("Join URL for the meeting.")
                .required(false),
            string("registrant_id").description("ID of the user that registered for the meeting.")
                .required(false),
            string("start_time").description("Start time of the meeting.")
                .required(false),
            string("topic").description("Topic of the meeting.")
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
            integer("participant_pin_code").description("Pin code for participation.")
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private ZoomAddMeetingRegistrantAction() {
    }
}
