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

package com.bytechef.component.agile.crm.action;

import static com.bytechef.component.agile.crm.constant.AgileCrmConstants.DUE;
import static com.bytechef.component.agile.crm.constant.AgileCrmConstants.PRIORITY_TYPE;
import static com.bytechef.component.agile.crm.constant.AgileCrmConstants.SUBJECT;
import static com.bytechef.component.agile.crm.constant.AgileCrmConstants.TYPE;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.time.ZoneId;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class AgileCrmCreateTaskAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createTask")
        .title("Create Task")
        .description("Creates a new task.")
        .properties(
            string(SUBJECT)
                .label("Subject")
                .description("The subject of the task.")
                .required(true),
            string(TYPE)
                .label("Task Type")
                .description("The type of the task.")
                .options(
                    option("Call", "CALL"),
                    option("Email", "EMAIL"),
                    option("Follow Up", "FOLLOW_UP"),
                    option("Meeting", "MEETING"),
                    option("Milestone", "MILESTONE"),
                    option("Send", "SEND"),
                    option("Tweet", "TWEET"),
                    option("Other", "OTHER"))
                .required(true),
            string(PRIORITY_TYPE)
                .label("Priority")
                .description("The priority of the task.")
                .options(
                    option("High", "HIGH"),
                    option("Normal", "NORMAL"),
                    option("Low", "LOW"))
                .required(true),
            dateTime(DUE)
                .label("Due Date")
                .description("The due date of the task.")
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        number("id")
                            .description("The ID of the task."),
                        string("type")
                            .description("The type of the task."),
                        string("priority_type")
                            .description("The priority of the task."),
                        integer("due")
                            .description("The due date of the task."),
                        integer("task_completed_time")
                            .description("The time when task was completed."),
                        integer("task_start_time")
                            .description("The time when task was started."),
                        integer("created_time")
                            .description("The time when task was created."),
                        bool("is_complete")
                            .description("Whether the task is completed."),
                        array("contacts")
                            .description("Contacts that are connected to the task.")
                            .items(
                                object()
                                    .properties(
                                        number("id")
                                            .description("The ID of the contact."),
                                        string("type")
                                            .description("The type of the contact."),
                                        integer("created_time")
                                            .description("The time when contact was created."),
                                        integer("updated_time")
                                            .description("The time when contact was updated."))),
                        string("subject")
                            .description("The subject of the task."),
                        string("entity_type")
                            .description("The entity type."),
                        array("notes")
                            .description("Notes of the task."),
                        array("note_ids")
                            .description("Notes ID."),
                        integer("progress")
                            .description("The progress of the task."),
                        string("status")
                            .description("The status of the task."),
                        array("deal_ids")
                            .description("IDs of the deals that are connected to the task."),
                        object("taskOwner")
                            .description("Owner of the task.")
                            .properties(
                                number("id")
                                    .description("The ID of the owner."),
                                string("domain")
                                    .description("The domain of the owner."),
                                string("email")
                                    .description("The email of the owner."),
                                string("phone")
                                    .description("The phone number of the owner."),
                                string("name")
                                    .description("The name of the owner."),
                                string("pic")
                                    .description("The picture of the owner."),
                                string("schedule_id")
                                    .description("The schedule ID of the owner."),
                                string("calendar_url")
                                    .description("The calendar URL of the owner."),
                                string("calendarURL")
                                    .description("The calendar URL of the owner.")))))
        .perform(AgileCrmCreateTaskAction::perform);

    private AgileCrmCreateTaskAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        return context.http(http -> http.post("/tasks"))
            .body(
                Body.of(
                    SUBJECT, inputParameters.getRequiredString(SUBJECT),
                    TYPE, inputParameters.getRequiredString(TYPE),
                    PRIORITY_TYPE, inputParameters.getRequiredString(PRIORITY_TYPE),
                    DUE, inputParameters.getRequiredLocalDateTime(DUE)
                        .atZone(ZoneId.systemDefault())
                        .toEpochSecond()))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
