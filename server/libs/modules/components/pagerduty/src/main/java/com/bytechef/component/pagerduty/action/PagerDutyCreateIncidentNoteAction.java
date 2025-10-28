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

package com.bytechef.component.pagerduty.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.CONTENT;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.FROM;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.INCIDENT_ID;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.pagerduty.util.PagerDutyUtils;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class PagerDutyCreateIncidentNoteAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createIncidentNote")
        .title("Create Incident Note")
        .description("Create a new note for the specified incident.")
        .properties(
            string(FROM)
                .label("From")
                .description("The email address of a valid user associated with the account making the request.")
                .required(true),
            string(INCIDENT_ID)
                .label("Incident ID")
                .description("ID of the incident to which the note will be added.")
                .options((OptionsFunction<String>) PagerDutyUtils::getIncidentIdOptions)
                .required(true),
            string(CONTENT)
                .label("Content")
                .description("Content of the incident note.")
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        object("note")
                            .properties(
                                string("id")
                                    .description("Incident note ID."),
                                object("user")
                                    .description("The user who created an incident note.")
                                    .properties(
                                        string("type")
                                            .description("A string that determines the schema of the object."),
                                        string("id")
                                            .description("ID of the user."),
                                        string("summary")
                                            .description("A short description about the incident."),
                                        string("self")
                                            .description("The API show URL at which the object is accessible."),
                                        string("html_url")
                                            .description(
                                                "A URL at which the entity is uniquely displayed in the Web app.")),
                                object("channel")
                                    .description(
                                        "The means by which this Note was created. Has different formats depending " +
                                            "on type.")
                                    .properties(
                                        string("type")
                                            .description("A string that determines the schema of the object."),
                                        string("id")
                                            .description("ID of the channel."),
                                        string("summary")
                                            .description("A short description about the channel."),
                                        string("self")
                                            .description("The API show URL at which the object is accessible."),
                                        string("html_url")
                                            .description(
                                                "A URL at which the entity is uniquely displayed in the Web app.")),
                                string(CONTENT)
                                    .description("The note content."),
                                string("created_at")
                                    .description("The time at which the note was submitted.")))))
        .perform(PagerDutyCreateIncidentNoteAction::perform);

    private PagerDutyCreateIncidentNoteAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        return context.http(
            http -> http.post("/incidents/%s/notes".formatted(inputParameters.getRequiredString(INCIDENT_ID))))
            .header(FROM, inputParameters.getRequiredString(FROM))
            .body(Body.of(Map.of("note", Map.of(CONTENT, inputParameters.getRequiredString(CONTENT)))))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
