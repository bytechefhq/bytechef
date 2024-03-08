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

package com.bytechef.component.capsule.crm.action;

import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.BASE_URL;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.CATEGORY;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.COLOUR;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.CREATE_TASK;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.DESCRIPTION;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.DETAIL;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.DUE_ON;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.NAME;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.date;
import static com.bytechef.component.definition.ComponentDSL.dateTime;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.nullable;
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.ComponentDSL.time;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Parameters;

/**
 * @author Monika Domiter
 */
public class CapsuleCRMCreateTaskAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_TASK)
        .title("Create Task")
        .description("Creates a new Task")
        .properties(
            string(DESCRIPTION)
                .label("Description")
                .description("A short description of the task.")
                .required(true),
            date(DUE_ON)
                .label("Due date")
                .description("The date when this task is due.")
                .required(true),
            string(DETAIL)
                .label("Detail")
                .description("More details about the task.")
                .required(false),
            object(CATEGORY)
                .label("Category")
                .description("The category of this task.")
                .properties(
                    string(NAME)
                        .label("Name")
                        .description("The name of the category.")
                        .required(true),
                    string(COLOUR)
                        .label("Colour")
                        .description(
                            "The hex colour code of the category. Must be a # followed by 6 hexadecimal digits " +
                                "(e.g. #ffffff).")
                        .required(false))
                .required(false))
        .outputSchema(
            object()
                .additionalProperties(
                    array(), bool(), date(), dateTime(), integer(), nullable(), number(), object(), string(), time()))
        .perform(CapsuleCRMCreateTaskAction::perform);

    private CapsuleCRMCreateTaskAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return actionContext.http(http -> http.post(BASE_URL + "/tasks"))
            .body(
                Http.Body.of(
                    "task",
                    new Object[] {
                        DESCRIPTION, inputParameters.getRequiredString(DESCRIPTION),
                        DUE_ON, inputParameters.getDate(DUE_ON),
                        DETAIL, inputParameters.getString(DETAIL),
                        CATEGORY, inputParameters.get(CATEGORY)
                    }))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
