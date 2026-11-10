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
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.greenhouse.constant.GreenhouseConstants.APPLICATION_ID;
import static com.bytechef.component.greenhouse.constant.GreenhouseConstants.SCORECARDS_OUTPUT_PROPERTY;
import static com.bytechef.component.greenhouse.constant.GreenhouseConstants.STATUS;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.greenhouse.util.GreenhouseUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class GreenhouseListApplicationScorecardsAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("listApplicationScorecards")
        .title("List Scorecards for Application")
        .description(
            "List the scorecards of an application, including the overall recommendation, notes and interviewer.")
        .properties(
            string(APPLICATION_ID)
                .label("Application ID")
                .description("The ID of the application whose scorecards to list.")
                .required(true),
            string(STATUS)
                .label("Status")
                .description(
                    "Return only scorecards with this status. Complete scorecards are submitted feedback, draft " +
                        "scorecards are autosaved working copies. Leave empty to return both.")
                .options(
                    option("Complete", "complete"),
                    option("Draft", "draft"))
                .required(false))
        .output(outputSchema(SCORECARDS_OUTPUT_PROPERTY))
        .help("", "https://docs.bytechef.io/reference/components/greenhouse_v1#list-scorecards-for-application")
        .perform(GreenhouseListApplicationScorecardsAction::perform);

    private GreenhouseListApplicationScorecardsAction() {
    }

    public static List<Map<String, Object>> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        Map<String, List<String>> queryParameters = new HashMap<>();

        queryParameters.put("application_ids", List.of(inputParameters.getRequiredString(APPLICATION_ID)));

        String status = inputParameters.getString(STATUS);

        if (status != null) {
            queryParameters.put(STATUS, List.of(status));
        }

        return GreenhouseUtils.getAllRecords("/scorecards", queryParameters, context);
    }
}
