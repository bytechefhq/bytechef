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

package com.bytechef.component.posthog.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.posthog.constant.PostHogConstants.API_KEY;
import static com.bytechef.component.posthog.constant.PostHogConstants.DISTINCT_ID;
import static com.bytechef.component.posthog.constant.PostHogConstants.EVENT;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class PostHogCreateEventAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createEvent")
        .title("Create Event")
        .description("Create a new event.")
        .properties(
            string(API_KEY)
                .label("Api Key Project")
                .description(
                    "The project API key used to create a new event." + "Found in Settings -> Project -> " +
                        "Project ID -> Project API key.")
                .required(true),
            string(EVENT)
                .label("Event")
                .description("Event name used to create a new event.")
                .required(true),
            string(DISTINCT_ID)
                .label("Distinct ID")
                .description(
                    "A unique identifier for the user creating the event, such as their username, email address, or " +
                        "system-assigned ID.")
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("status")
                            .description("The status of the request."))))
        .perform(PostHogCreateEventAction::perform);

    private PostHogCreateEventAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        return context.http(http -> http.post("/capture/"))
            .body(
                Body.of(
                    API_KEY, inputParameters.getRequiredString(API_KEY),
                    EVENT, inputParameters.getRequiredString(EVENT),
                    "properties", Map.of(DISTINCT_ID, inputParameters.getRequiredString(DISTINCT_ID))))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
