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

import static com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.copper.constant.CopperConstants;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;

/**
 * @author Vihar Shah
 */
public class CopperCreateTaskAction {
    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CopperConstants.CREATE_TASK)
        .title("Create Task")
        .description("Create a new task in Copper")
        .properties(
            string("name")
                .label("Name")
                .description("The name of the task")
                .required(true),
            object("related_resource")
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
            string("assignee_id")
                .label("Assignee ID")
                .description("The ID of the user to assign the task to")
                .required(false),
            string("due_date")
                .label("Due Date")
                .description("The due date of the task")
                .required(false),
            string("reminder_date")
                .label("Reminder Date")
                .description("The reminder date of the task")
                .required(false),
            string("priority")
                .label("Priority")
                .description("The priority of the task")
                .required(false),
            string("status")
                .label("Status")
                .description("The status of the task")
                .required(false),
            string("details")
                .label("Details")
                .description("The details of the task")
                .required(false),
            array("tags")
                .label("Tags")
                .required(false),
            array("custom_fields")
                .label("Custom Fields")
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("id")
                            .label("ID")
                            .description("The ID of the task"),
                        string("name")
                            .label("Name")
                            .description("The name of the task"),
                        object("related_resource")
                            .label("Related Resource")
                            .properties(
                                string("id")
                                    .label("ID")
                                    .description("The ID of the related resource"),
                                string("type")
                                    .label("Type")
                                    .description("The type of the related resource")),
                        string("assignee_id")
                            .label("Assignee ID")
                            .description("The ID of the user to assign the task to"),
                        string("due_date")
                            .label("Due Date")
                            .description("The due date of the task"),
                        string("reminder_date")
                            .label("Reminder Date")
                            .description("The reminder date of the task"),
                        string("completed_date")
                            .label("Completed Date")
                            .description("The date of completion of the task"),
                        string("priority")
                            .label("Priority")
                            .description("The priority of the task"),
                        string("status")
                            .label("Status")
                            .description("The status of the task"),
                        string("details")
                            .label("Details")
                            .description("The details of the task"),
                        array("tags")
                            .label("Tags"),
                        array("custom_fields")
                            .label("Custom Fields"),
                        string("date_created")
                            .label("Date Created")
                            .description("The date and time the task was created"),
                        string("date_modified")
                            .label("Date Modified")
                            .description("The date and time the task was last updated"))))
        .perform(CopperCreateTaskAction::perform);

    protected static final Context.ContextFunction<Context.Http, Context.Http.Executor> POST_CREATE_TASK_FUNCTION =
        http -> http.post("/tasks");

    private CopperCreateTaskAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters outputParameters, ActionContext actionContext) {
        return actionContext.http(POST_CREATE_TASK_FUNCTION)
            .body(
                Context.Http.Body.of(
                    CopperConstants.NAME, inputParameters.getString(CopperConstants.NAME),
                    CopperConstants.RELATED_RESOURCE, inputParameters.get(CopperConstants.RELATED_RESOURCE),
                    CopperConstants.ASSIGNEE_ID, inputParameters.getString(CopperConstants.ASSIGNEE_ID),
                    CopperConstants.DUE_DATE, inputParameters.getString(CopperConstants.DUE_DATE),
                    CopperConstants.REMINDER_DATE, inputParameters.getString(CopperConstants.REMINDER_DATE),
                    CopperConstants.PRIORITY, inputParameters.getString(CopperConstants.PRIORITY),
                    CopperConstants.STATUS, inputParameters.getString(CopperConstants.STATUS),
                    CopperConstants.DETAILS, inputParameters.getString(CopperConstants.DETAILS),
                    CopperConstants.TAGS, inputParameters.getList(CopperConstants.TAGS),
                    CopperConstants.CUSTOM_FIELDS, inputParameters.getList(CopperConstants.CUSTOM_FIELDS)))
            .configuration(Context.Http.responseType(Context.Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
