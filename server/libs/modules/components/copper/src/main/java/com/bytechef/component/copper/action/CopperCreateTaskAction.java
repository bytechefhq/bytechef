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

package com.bytechef.component.copper.action;

import static com.bytechef.component.copper.constant.CopperConstants.ASSIGNEE_ID;
import static com.bytechef.component.copper.constant.CopperConstants.CUSTOM_FIELDS;
import static com.bytechef.component.copper.constant.CopperConstants.DETAILS;
import static com.bytechef.component.copper.constant.CopperConstants.DUE_DATE;
import static com.bytechef.component.copper.constant.CopperConstants.ID;
import static com.bytechef.component.copper.constant.CopperConstants.NAME;
import static com.bytechef.component.copper.constant.CopperConstants.PRIORITY;
import static com.bytechef.component.copper.constant.CopperConstants.RELATED_RESOURCE;
import static com.bytechef.component.copper.constant.CopperConstants.REMINDER_DATE;
import static com.bytechef.component.copper.constant.CopperConstants.STATUS;
import static com.bytechef.component.copper.constant.CopperConstants.TAGS;
import static com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;

/**
 * @author Vihar Shah
 */
public class CopperCreateTaskAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createTask")
        .title("Create Task")
        .description("Create a new task in Copper")
        .properties(
            string(NAME)
                .label("Name")
                .description("The name of the task")
                .required(true),
            object(RELATED_RESOURCE)
                .label("Related Resource")
                .properties(
                    string("id")
                        .label("ID")
                        .description("The ID of the related resource")
                        .required(true),
                    string("type")
                        .label("Type")
                        .description("The type of the related resource")
                        .required(true))
                .required(false),
            string(ASSIGNEE_ID)
                .label("Assignee ID")
                .description("The ID of the user to assign the task to")
                .required(false),
            string(DUE_DATE)
                .label("Due Date")
                .description("The due date of the task")
                .required(false),
            string(REMINDER_DATE)
                .label("Reminder Date")
                .description("The reminder date of the task")
                .required(false),
            string(PRIORITY)
                .label("Priority")
                .description("The priority of the task")
                .required(false),
            string(STATUS)
                .label("Status")
                .description("The status of the task")
                .required(false),
            string(DETAILS)
                .label("Details")
                .description("The details of the task")
                .required(false),
            array(TAGS)
                .label("Tags")
                .required(false),
            array(CUSTOM_FIELDS)
                .label("Custom Fields")
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        string(ID)
                            .label("ID")
                            .description("The ID of the task"),
                        string(NAME)
                            .label("Name")
                            .description("The name of the task"),
                        object(RELATED_RESOURCE)
                            .label("Related Resource")
                            .properties(
                                string("id")
                                    .label("ID")
                                    .description("The ID of the related resource"),
                                string("type")
                                    .label("Type")
                                    .description("The type of the related resource")),
                        string(ASSIGNEE_ID)
                            .label("Assignee ID")
                            .description("The ID of the user to assign the task to"),
                        string(DUE_DATE)
                            .label("Due Date")
                            .description("The due date of the task"),
                        string(REMINDER_DATE)
                            .label("Reminder Date")
                            .description("The reminder date of the task"),
                        string("completed_date")
                            .label("Completed Date")
                            .description("The date of completion of the task"),
                        string(PRIORITY)
                            .label("Priority")
                            .description("The priority of the task"),
                        string(STATUS)
                            .label("Status")
                            .description("The status of the task"),
                        string(DETAILS)
                            .label("Details")
                            .description("The details of the task"),
                        array(TAGS)
                            .label("Tags"),
                        array(CUSTOM_FIELDS)
                            .label("Custom Fields"),
                        string("date_created")
                            .label("Date Created")
                            .description("The date and time the task was created"),
                        string("date_modified")
                            .label("Date Modified")
                            .description("The date and time the task was last updated"))))
        .perform(CopperCreateTaskAction::perform);

    protected static final ContextFunction<Http, Http.Executor> POST_CREATE_TASK_FUNCTION =
        http -> http.post("/tasks");

    private CopperCreateTaskAction() {
    }

    protected static Object perform(
        Parameters inputParameters, Parameters outputParameters, ActionContext actionContext) {

        return actionContext.http(POST_CREATE_TASK_FUNCTION)
            .body(
                Http.Body.of(
                    NAME, inputParameters.getString(NAME),
                    RELATED_RESOURCE, inputParameters.get(RELATED_RESOURCE),
                    ASSIGNEE_ID, inputParameters.getString(ASSIGNEE_ID),
                    DUE_DATE, inputParameters.getString(DUE_DATE),
                    REMINDER_DATE, inputParameters.getString(REMINDER_DATE),
                    PRIORITY, inputParameters.getString(PRIORITY),
                    STATUS, inputParameters.getString(STATUS),
                    DETAILS, inputParameters.getString(DETAILS),
                    TAGS, inputParameters.getList(TAGS),
                    CUSTOM_FIELDS, inputParameters.getList(CUSTOM_FIELDS)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
