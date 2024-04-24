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

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.OptionsDataSource;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.nifty.constant.NiftyConstants;
import com.bytechef.component.nifty.util.NiftyOptionUtils;

/**
 * @author Luka Ljubić
 */
public class NiftyCreateTaskAction {

    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION =
        ComponentDSL.action(NiftyConstants.CREATE_TASK)
            .title("Create Task")
            .description("Create a new task")
            .properties(
                ComponentDSL.string(NiftyConstants.NAME)
                    .label("Name")
                    .description("Name of the task")
                    .maxLength(50)
                    .required(true),
                ComponentDSL.string(NiftyConstants.DESCRIPTION)
                    .label("Description")
                    .description("Description of the project")
                    .maxLength(320)
                    .required(false),
                ComponentDSL.string(NiftyConstants.PROJECT)
                    .options((OptionsDataSource.ActionOptionsFunction<String>) NiftyOptionUtils::getProjectId),
                ComponentDSL.string(NiftyConstants.TASK_GROUP_ID)
                    .options((OptionsDataSource.ActionOptionsFunction<String>) NiftyOptionUtils::getTaskGroupId)
                    .loadOptionsDependsOn(NiftyConstants.PROJECT),
                ComponentDSL.dateTime(NiftyConstants.DUE_DATE))
            .outputSchema(
                ComponentDSL.object()
                    .properties(
                        ComponentDSL.string(NiftyConstants.TASK_GROUP_ID)))
            .perform(NiftyCreateTaskAction::perform);

    private NiftyCreateTaskAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return actionContext.http(http -> http.post(NiftyConstants.BASE_URL + "/tasks"))
            .body(Http.Body.of(
                NiftyConstants.NAME, inputParameters.getString(NiftyConstants.NAME),
                NiftyConstants.DESCRIPTION, inputParameters.getString(NiftyConstants.DESCRIPTION),
                NiftyConstants.TASK_GROUP_ID, inputParameters.getString(NiftyConstants.TASK_GROUP_ID),
                NiftyConstants.DUE_DATE, inputParameters.getString(NiftyConstants.DUE_DATE)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new Context.TypeReference<>() {});
    }
}
