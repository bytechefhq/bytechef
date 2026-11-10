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

package com.bytechef.component.greenhouse.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.greenhouse.constant.GreenhouseConstants.APPLICATION_ID;
import static com.bytechef.component.greenhouse.constant.GreenhouseConstants.SCORECARDS_OUTPUT_PROPERTY;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class GreenhouseListApplicationScorecardsAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("listApplicationScorecards")
        .title("List Scorecards for Application")
        .description(
            "List all submitted scorecards for an application, including ratings, recommendations and question " +
                "answers.")
        .properties(
            string(APPLICATION_ID)
                .label("Application ID")
                .description("The ID of the application whose scorecards to list.")
                .required(true))
        .output(outputSchema(SCORECARDS_OUTPUT_PROPERTY))
        .help("", "https://docs.bytechef.io/reference/components/greenhouse_v1#list-scorecards-for-application")
        .perform(GreenhouseListApplicationScorecardsAction::perform);

    private GreenhouseListApplicationScorecardsAction() {
    }

    public static List<Map<String, Object>> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        String applicationId = inputParameters.getRequiredString(APPLICATION_ID);

        return context.http(http -> http.get("/applications/" + applicationId + "/scorecards"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
