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

package com.bytechef.component.wrike.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.wrike.constant.WrikeConstants.DESCRIPTION;
import static com.bytechef.component.wrike.constant.WrikeConstants.IMPORTANCE;
import static com.bytechef.component.wrike.constant.WrikeConstants.PARENT_ID;
import static com.bytechef.component.wrike.constant.WrikeConstants.RESPONSIBLES;
import static com.bytechef.component.wrike.constant.WrikeConstants.STATUS;
import static com.bytechef.component.wrike.constant.WrikeConstants.TITLE;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.wrike.util.WrikeUtils;

/**
 * @author Nikolina Spehar
 */
public class WrikeCreateTaskAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createTask")
        .title("Create Task")
        .description("Create a task.")
        .properties(
            string(PARENT_ID)
                .label("Parent ID")
                .description("ID of the parent folder.")
                .options((OptionsFunction<String>) WrikeUtils::getParentIdOptions)
                .required(true),
            string(TITLE)
                .label("Title")
                .description("The title of the task.")
                .required(true),
            string(DESCRIPTION)
                .label("Description")
                .description("Description of task, will be left blank, if not set.")
                .required(false),
            string(STATUS)
                .label("Status")
                .description("The status of the task.")
                .required(false)
                .options(
                    option("Active", "Active"),
                    option("Completed", "Completed"),
                    option("Deferred", "Deferred"),
                    option("Cancelled", "Cancelled")),
            string(IMPORTANCE)
                .label("Importance")
                .description("The importance of the task.")
                .required(false)
                .options(
                    option("High", "High"),
                    option("Normal", "Normal"),
                    option("Low", "Low")),
            array(RESPONSIBLES)
                .label("Assignees")
                .description("Choose assignees for the task.")
                .required(false)
                .options((OptionsFunction<String>) WrikeUtils::getContactIdOptions)
                .items(
                    string("assigneeId")
                        .description("ID of the assignee of the task.")))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("kind")
                            .description("Kind of the object that was created."),
                        array("data")
                            .description("Data of the object that was created.")
                            .items(
                                object()
                                    .properties(
                                        string("id")
                                            .description("ID of the object that was created."),
                                        string("accountId")
                                            .description("ID of the account that created the object."),
                                        string("title")
                                            .description("Title of the object that was created."),
                                        string("description")
                                            .description("Description of the object."),
                                        string("briefDescription")
                                            .description("Brief Description of the object."),
                                        array("parentIds")
                                            .description("Parent IDs of the object.")
                                            .items(
                                                string("parentId")
                                                    .description("Parent ID.")),
                                        array("superParentIds")
                                            .description("Super parent IDs of the object.")
                                            .items(
                                                string("superParentId")
                                                    .description("Super parent ID.")),
                                        array("sharedIds")
                                            .description("Shared IDs of the object.")
                                            .items(
                                                string("sharedId")
                                                    .description("Shared ID.")),
                                        array("responsibleIds")
                                            .description("IDs of responsible of the object.")
                                            .items(
                                                string("responsibleId")
                                                    .description("Responsible ID.")),
                                        string("status")
                                            .description("The status of the object."),
                                        string("importance")
                                            .description("The importance of the object."),
                                        string("createdDate")
                                            .description("Date when the object was created."),
                                        string("updatedDate")
                                            .description("Date when the object was updated."),
                                        string("completedDate")
                                            .description("Date when the object was completed."),
                                        object("dates")
                                            .description("Dates of the object.")
                                            .properties(
                                                string("type")
                                                    .description("Type of the date.")),
                                        string("scope")
                                            .description("Scope of the object."),
                                        array("authorIds")
                                            .description("Authors IDs.")
                                            .items(
                                                string("authorId")
                                                    .description("Author ID.")),
                                        string("customStatusId")
                                            .description("Custom status ID of the project."),
                                        bool("hasAttachments")
                                            .description("Whether the object has attachments"),
                                        integer("attachmentCount")
                                            .description("How many attachments does the object have."),
                                        string("permalink")
                                            .description("Permalink of the object."),
                                        string("priority")
                                            .description("Priority of the object."),
                                        bool("followedByMe")
                                            .description("Whether the object is followed by me."),
                                        array("followerIds")
                                            .description("Followers IDs of the object.")
                                            .items(
                                                string("followerId")
                                                    .description("Follower ID.")),
                                        array("superTaskIds")
                                            .description("Super task IDs of the object."),
                                        array("subTaskIds")
                                            .description("Sub task IDs of the object."),
                                        array("dependencyIds")
                                            .description("Dependency IDs of the object."),
                                        array("metadata")
                                            .description("Metadata of the object."),
                                        array("customFields")
                                            .placeholder("Custom fields of the object."))))))
        .perform(WrikeCreateTaskAction::perform);

    private WrikeCreateTaskAction() {
    }

    protected static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context.http(
            http -> http.post("/folders/%s/tasks".formatted(inputParameters.getRequiredString(PARENT_ID))))
            .configuration(responseType(ResponseType.JSON))
            .queryParameters(
                TITLE, inputParameters.getRequiredString(TITLE),
                DESCRIPTION, inputParameters.getString(DESCRIPTION),
                STATUS, inputParameters.getString(STATUS),
                IMPORTANCE, inputParameters.getString(IMPORTANCE),
                RESPONSIBLES, inputParameters.getList(RESPONSIBLES))
            .execute()
            .getBody();
    }
}
