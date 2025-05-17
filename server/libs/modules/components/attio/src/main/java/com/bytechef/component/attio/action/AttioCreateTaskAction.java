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

package com.bytechef.component.attio.action;

import static com.bytechef.component.attio.constant.AttioConstants.ASSIGNEES;
import static com.bytechef.component.attio.constant.AttioConstants.CONTENT;
import static com.bytechef.component.attio.constant.AttioConstants.DATA;
import static com.bytechef.component.attio.constant.AttioConstants.DEADLINE_AT;
import static com.bytechef.component.attio.constant.AttioConstants.FORMAT;
import static com.bytechef.component.attio.constant.AttioConstants.ID;
import static com.bytechef.component.attio.constant.AttioConstants.IS_COMPLETED;
import static com.bytechef.component.attio.constant.AttioConstants.LINKED_RECORDS;
import static com.bytechef.component.attio.constant.AttioConstants.REFERENCED_ACTOR_ID;
import static com.bytechef.component.attio.constant.AttioConstants.REFERENCED_ACTOR_TYPE;
import static com.bytechef.component.attio.constant.AttioConstants.TARGET_OBJECT;
import static com.bytechef.component.attio.constant.AttioConstants.TARGET_RECORD_ID;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.attio.util.AttioUtils;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class AttioCreateTaskAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createTask")
        .title("Create Task")
        .description("Creates a new task.")
        .properties(
            string(CONTENT)
                .label("Content")
                .required(true),
            string(FORMAT)
                .label("Format")
                .options(option("plaintext", "plaintext"))
                .required(true),
            dateTime(DEADLINE_AT)
                .label("Deadline")
                .required(true),
            bool(IS_COMPLETED)
                .label("Is Completed")
                .required(true),
            array(LINKED_RECORDS)
                .label("Linked Records")
                .items(
                    object()
                        .properties(
                            string(TARGET_OBJECT)
                                .label("Target object")
                                .options((ActionOptionsFunction<String>) AttioUtils::getTargetObjectOptions)
                                .required(true),
                            string(TARGET_RECORD_ID)
                                .label("Target Record ID")
                                .optionsLookupDependsOn(LINKED_RECORDS + "[index]." + TARGET_OBJECT)
                                .options(AttioUtils.getTargetRecordIdOptions(TARGET_OBJECT))
                                .required(true)))
                .required(true),
            array(ASSIGNEES)
                .label("Assignees")
                .items(
                    object()
                        .properties(
                            string(REFERENCED_ACTOR_TYPE)
                                .label("Reference Actor Type")
                                .options(option("Workspace Member", "workspace-member"))
                                .required(true),
                            string(REFERENCED_ACTOR_ID)
                                .label("Reference Actor ID")
                                .options((ActionOptionsFunction<String>) AttioUtils::getTargetActorIdOptions)
                                .required(true)))
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        object("data")
                            .properties(
                                object(ID)
                                    .properties(
                                        string("workspace_id")
                                            .description("ID of the workspace."),
                                        string("task_id")
                                            .description("ID of the task.")),
                                string("content_plaintext")
                                    .description("Content of the task."),
                                bool(IS_COMPLETED)
                                    .description("Whether the task is completed or not."),
                                string(DEADLINE_AT)
                                    .description("Deadline of the task in ISO 8601 format."),
                                array(LINKED_RECORDS)
                                    .description("List of records that are linked to the task.")
                                    .items(
                                        string("target_object_id"),
                                        string(TARGET_RECORD_ID)),
                                array(ASSIGNEES)
                                    .description("List of actors that are assigned to the task.")
                                    .items(
                                        string(REFERENCED_ACTOR_TYPE),
                                        string(REFERENCED_ACTOR_ID)),
                                object("created_by_actor")
                                    .description("Information about the actor that created the task.")
                                    .properties(
                                        string("type")
                                            .description("Type of the actor."),
                                        string(ID)
                                            .description("ID of the actor.")),
                                string("created_at")
                                    .description("Date when the task was created in ISO 8601 format.")))))
        .perform(AttioCreateTaskAction::perform);

    private AttioCreateTaskAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        return context.http(http -> http.post("/tasks"))
            .body(
                Body.of(
                    DATA, Map.of(
                        CONTENT, inputParameters.getRequiredString(CONTENT),
                        FORMAT, inputParameters.getRequiredString(FORMAT),
                        DEADLINE_AT, inputParameters.getRequiredLocalDateTime(DEADLINE_AT)
                            .toString(),
                        IS_COMPLETED, inputParameters.getRequiredBoolean(IS_COMPLETED),
                        LINKED_RECORDS, inputParameters.getRequiredList(LINKED_RECORDS),
                        ASSIGNEES, inputParameters.getRequiredList(ASSIGNEES))))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
