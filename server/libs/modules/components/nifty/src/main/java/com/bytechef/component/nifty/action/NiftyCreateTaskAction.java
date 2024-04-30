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

package com.bytechef.component.nifty.action;

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.dateTime;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.nifty.constant.NiftyConstants.BASE_URL;
import static com.bytechef.component.nifty.constant.NiftyConstants.CREATE_TASK;
import static com.bytechef.component.nifty.constant.NiftyConstants.DESCRIPTION;
import static com.bytechef.component.nifty.constant.NiftyConstants.DUE_DATE;
import static com.bytechef.component.nifty.constant.NiftyConstants.NAME;
import static com.bytechef.component.nifty.constant.NiftyConstants.PROJECT;
import static com.bytechef.component.nifty.constant.NiftyConstants.TASK_GROUP_ID;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.nifty.util.NiftyOptionUtils;

/**
 * @author Luka Ljubić
 */
public class NiftyCreateTaskAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_TASK)
        .title("Create Task")
        .description("Create a new task")
        .properties(
            string(NAME)
                .label("Name")
                .description("Name of the task")
                .maxLength(50)
                .required(true),
            string(DESCRIPTION)
                .label("Description")
                .description("Description of the task.")
                .maxLength(320)
                .required(false),
            string(PROJECT)
                .label("Project")
                .description("Project within which the task will be created.")
                .options((ActionOptionsFunction<String>) NiftyOptionUtils::getProjectIdOptions)
                .required(true),
            string(TASK_GROUP_ID)
                .label("Status")
                .options((ActionOptionsFunction<String>) NiftyOptionUtils::getTaskGroupIdOptions)
                .loadOptionsDependsOn(PROJECT)
                .required(true),
            dateTime(DUE_DATE)
                .label("Due date")
                .description("Due date for the task.")
                .required(false))
        .outputSchema(
            object()
                .properties(
                    string("id"),
                    string(NAME),
                    string(PROJECT),
                    string(DESCRIPTION),
                    string(DUE_DATE),
                    string("task_group")))
        .perform(NiftyCreateTaskAction::perform);

    private NiftyCreateTaskAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return actionContext.http(http -> http.post(BASE_URL + "/tasks"))
            .body(
                Http.Body.of(
                    NAME, inputParameters.getRequiredString(NAME),
                    DESCRIPTION, inputParameters.getString(DESCRIPTION),
                    TASK_GROUP_ID, inputParameters.getRequiredString(TASK_GROUP_ID),
                    DUE_DATE, inputParameters.getLocalDateTime(DUE_DATE)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
