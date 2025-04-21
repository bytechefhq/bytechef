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

package com.bytechef.component.calendly.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDsl;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class CalendlyCancelEventAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("cancelEvent")
        .title("Cancel Event")
        .description("Cancels specified event.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/scheduled_events/{eventId}/cancellation", "bodyContentType", BodyContentType.JSON, "mimeType",
                "application/json"

            ))
        .properties(string("eventId").label("Event")
            .description("Event to be canceled.")
            .required(true)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)),
            string("reason").maxLength(10000)
                .metadata(
                    Map.of(
                        "type", PropertyType.BODY))
                .label("Reason")
                .description("Reason for cancellation.")
                .required(false),
            string("scope").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Scope")
                .required(false))
        .output(
            outputSchema(object()
                .properties(object("body")
                    .properties(string("canceled_by").description("Name of the person whom canceled.")
                        .required(false),
                        string("reason").description("Reason for cancellation.")
                            .required(false),
                        string("canceler_type").description("Type of user who canceled the event. Host or invitee.")
                            .required(false),
                        dateTime("created_at").description("The moment when the cancellation was created.")
                            .required(false))
                    .required(false))
                .metadata(
                    Map.of(
                        "responseType", ResponseType.JSON))));

    private CalendlyCancelEventAction() {
    }
}
