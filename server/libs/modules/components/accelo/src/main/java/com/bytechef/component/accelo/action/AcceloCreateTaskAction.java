/*
 * Copyright 2023-present ByteChef Inc.
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
import static com.bytechef.component.accelo.constant.AcceloConstants.CREATE_TASK;
import static com.bytechef.component.accelo.constant.AcceloConstants.DATE_STARTED;
import static com.bytechef.component.accelo.constant.AcceloConstants.TITLE;
import static com.bytechef.component.accelo.util.AcceloUtils.createUrl;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.date;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.accelo.util.AcceloUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.OptionsDataSource;
import com.bytechef.component.definition.Parameters;

/**
 * @author Monika Domiter
 */
public class AcceloCreateTaskAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_TASK)
        .title("Create task")
        .description("Creates a new task")
        .properties(
            string(TITLE)
                .label("Title")
                .required(true),
            string(AGAINST_TYPE)
                .label("Against type")
                .description("The type of object the task is against.")
                .options(
                    option("Company", "company"),
                    option("Prospect", "prospect"))
                .required(true),
            string(AGAINST_ID)
                .label("Against object")
                .description("Object the task is against.")
                .optionsLookupDependsOn(AGAINST_TYPE)
                .options((OptionsDataSource.ActionOptionsFunction<String>) AcceloUtils::getAgainstIdOptions)
                .required(true),
            date(DATE_STARTED)
                .label("Start date")
                .description("The date the task is is scheduled to start.")
                .required(true))
        .outputSchema(
            object()
                .properties(
                    object("response")
                        .properties(
                            string("id"),
                            string(TITLE)),
                    object("meta")
                        .properties(
                            string("more_info"),
                            string("status"),
                            string("message"))))
        .perform(AcceloCreateTaskAction::perform);

    private AcceloCreateTaskAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return actionContext.http(http -> http.post(createUrl(connectionParameters, "tasks")))
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
