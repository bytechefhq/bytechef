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

package com.bytechef.component.accelo.action;

import static com.bytechef.component.accelo.constant.AcceloConstants.AGAINST_ID;
import static com.bytechef.component.accelo.constant.AcceloConstants.AGAINST_TYPE;
import static com.bytechef.component.accelo.constant.AcceloConstants.DATE_STARTED;
import static com.bytechef.component.accelo.constant.AcceloConstants.TITLE;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.accelo.util.AcceloUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;

/**
 * @author Monika Kušter
 */
public class AcceloCreateTaskAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createTask")
        .title("Create Task")
        .description("Creates a new task.")
        .properties(
            string(TITLE)
                .label("Title")
                .required(true),
            string(AGAINST_TYPE)
                .label("Against Type")
                .description("The type of object the task is against.")
                .options(
                    option("Company", "company"),
                    option("Prospect", "prospect"))
                .required(true),
            string(AGAINST_ID)
                .label("Against Object ID")
                .description("ID of the object the task is against.")
                .optionsLookupDependsOn(AGAINST_TYPE)
                .options((OptionsFunction<String>) AcceloUtils::getAgainstIdOptions)
                .required(true),
            date(DATE_STARTED)
                .label("Start Date")
                .description("The date the task is is scheduled to start.")
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        object("response")
                            .properties(
                                string("id")
                                    .description("The ID of the created task."),
                                string(TITLE)
                                    .description("The title of the created task.")),
                        object("meta")
                            .properties(
                                string("more_info"),
                                string("status"),
                                string("message")))))
        .perform(AcceloCreateTaskAction::perform);

    private AcceloCreateTaskAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return actionContext.http(http -> http.post("/tasks"))
            .body(
                Http.Body.of(
                    TITLE, inputParameters.getRequiredString(TITLE),
                    AGAINST_TYPE, inputParameters.getRequiredString(AGAINST_TYPE),
                    AGAINST_ID, inputParameters.getRequiredString(AGAINST_ID),
                    DATE_STARTED, inputParameters.getRequiredDate(DATE_STARTED)
                        .toInstant()
                        .getEpochSecond()))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
