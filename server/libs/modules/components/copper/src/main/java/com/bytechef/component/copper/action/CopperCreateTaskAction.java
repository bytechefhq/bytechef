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

package com.bytechef.component.copper.action;

import static com.bytechef.component.copper.constant.CopperConstants.ASSIGNEE_ID;
import static com.bytechef.component.copper.constant.CopperConstants.DETAILS;
import static com.bytechef.component.copper.constant.CopperConstants.DUE_DATE;
import static com.bytechef.component.copper.constant.CopperConstants.ID;
import static com.bytechef.component.copper.constant.CopperConstants.NAME;
import static com.bytechef.component.copper.constant.CopperConstants.PRIORITY;
import static com.bytechef.component.copper.constant.CopperConstants.REMINDER_DATE;
import static com.bytechef.component.copper.constant.CopperConstants.STATUS;
import static com.bytechef.component.copper.constant.CopperConstants.TAGS;
import static com.bytechef.component.copper.constant.CopperConstants.TYPE;
import static com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.copper.util.CopperOptionUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.Date;

/**
 * @author Vihar Shah
 * @author Monika Kušter
 */
public class CopperCreateTaskAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createTask")
        .title("Create Task")
        .description("Creates a new task in Copper.")
        .properties(
            string(NAME)
                .label("Name")
                .description("The name of the task.")
                .required(true),
            string(ASSIGNEE_ID)
                .label("Assignee ID")
                .description("ID of the user to assign the task to.")
                .options((OptionsFunction<String>) CopperOptionUtils::getUserOptions)
                .required(false),
            date(DUE_DATE)
                .label("Due Date")
                .description("The due date of the task.")
                .required(false),
            date(REMINDER_DATE)
                .label("Reminder Date")
                .description("The reminder date of the task.")
                .required(false),
            string(DETAILS)
                .label("Description")
                .description("Description of the task.")
                .required(false),
            string(PRIORITY)
                .label("Priority")
                .description("The priority of the task.")
                .options(
                    option("None", "None"),
                    option("Low", "Low"),
                    option("Medium", "Medium"),
                    option("High", "High"))
                .defaultValue("None")
                .required(true),
            array(TAGS)
                .label("Tags")
                .items(
                    string()
                        .options((OptionsFunction<String>) CopperOptionUtils::getTagsOptions))
                .required(false),
            string(STATUS)
                .label("Status")
                .description("The status of the task.")
                .options(
                    option("Open", "Open"),
                    option("Completed", "Completed"))
                .defaultValue("Open")
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        string(ID)
                            .description("ID of the new task."),
                        string(NAME)
                            .description("Name of the new task."),
                        object("related_resource")
                            .description("Primary related resource for the new task.")
                            .properties(
                                string(ID)
                                    .description(""),
                                string(TYPE)),
                        string(ASSIGNEE_ID)
                            .description("ID of the user that is owner of the new task."),
                        string(DUE_DATE)
                            .description("The due date of the new task."),
                        string(REMINDER_DATE)
                            .description("The reminder date of the new task."),
                        string("completed_date")
                            .description("The date the task was completed."),
                        string(PRIORITY)
                            .description("The priority of the new task."),
                        string(STATUS)
                            .description("The status of the new task."),
                        string(DETAILS)
                            .description("Description of the new task."),
                        array(TAGS)
                            .description("Tags associated with the new task.")
                            .items(string()))))
        .perform(CopperCreateTaskAction::perform);

    protected static final ContextFunction<Http, Http.Executor> POST_CREATE_TASK_FUNCTION =
        http -> http.post("/tasks");

    private CopperCreateTaskAction() {
    }

    protected static Object perform(
        Parameters inputParameters, Parameters outputParameters, ActionContext actionContext) {

        Date dueDate = inputParameters.getDate(DUE_DATE);
        Date reminderDate = inputParameters.getDate(REMINDER_DATE);

        return actionContext.http(POST_CREATE_TASK_FUNCTION)
            .body(
                Http.Body.of(
                    NAME, inputParameters.getRequiredString(NAME),
                    ASSIGNEE_ID, inputParameters.getString(ASSIGNEE_ID),
                    DUE_DATE, dueDate == null ? null : dueDate.toInstant()
                        .getEpochSecond(),
                    REMINDER_DATE, reminderDate == null ? null : reminderDate.toInstant()
                        .getEpochSecond(),
                    PRIORITY, inputParameters.getRequiredString(PRIORITY),
                    STATUS, inputParameters.getRequiredString(STATUS),
                    DETAILS, inputParameters.getString(DETAILS),
                    TAGS, inputParameters.getList(TAGS, String.class)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
