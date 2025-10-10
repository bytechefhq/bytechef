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
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.wrike.constant.WrikeConstants.BUDGET;
import static com.bytechef.component.wrike.constant.WrikeConstants.CONTRACT_TYPE;
import static com.bytechef.component.wrike.constant.WrikeConstants.DESCRIPTION;
import static com.bytechef.component.wrike.constant.WrikeConstants.END_DATE;
import static com.bytechef.component.wrike.constant.WrikeConstants.OWNER_IDS;
import static com.bytechef.component.wrike.constant.WrikeConstants.PARENT_ID;
import static com.bytechef.component.wrike.constant.WrikeConstants.PROJECT;
import static com.bytechef.component.wrike.constant.WrikeConstants.START_DATE;
import static com.bytechef.component.wrike.constant.WrikeConstants.TITLE;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.wrike.util.WrikeUtils;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class WrikeCreateProjectAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createProject")
        .title("Create Project")
        .description("Create a project.")
        .properties(
            string(PARENT_ID)
                .label("Parent ID")
                .description("ID of the parent folder.")
                .options((OptionsFunction<String>) WrikeUtils::getParentIdOptions)
                .required(true),
            string(TITLE)
                .label("Title")
                .description("The title of the project.")
                .required(true),
            string(DESCRIPTION)
                .label("Description")
                .description("The description of the project.")
                .required(false),
            date(START_DATE)
                .label("Start Date")
                .description("The start date of the project.")
                .required(false),
            date(END_DATE)
                .label("End Date")
                .description("The end date of the project.")
                .required(false),
            string(CONTRACT_TYPE)
                .label("Contract Type")
                .description("The contract type of the project.")
                .options(
                    option("Billable", "Billable"),
                    option("Non-Billable", "NonBillable"))
                .required(false),
            array(OWNER_IDS)
                .label("Owner IDs")
                .description("List of project owner IDs.")
                .required(false)
                .options((OptionsFunction<String>) WrikeUtils::getContactIdOptions)
                .items(string()),
            integer(BUDGET)
                .label("Budget")
                .description("The budget of the project.")
                .required(false))
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
                                        string("createdDate")
                                            .description("Date when the object was created."),
                                        string("updatedDate")
                                            .description("Date when the object was updated."),
                                        string("description")
                                            .description("Description of the object."),
                                        array("sharedIds")
                                            .description("Shared IDs of the object.")
                                            .items(
                                                string("sharedId")
                                                    .description("Shared ID.")),
                                        array("parentIds")
                                            .description("Parent IDs of the object.")
                                            .items(
                                                string("parentId")
                                                    .description("Parent ID.")),
                                        array("childIds")
                                            .description("Child IDs of the object.")
                                            .items(
                                                string("childId")
                                                    .description("Child ID.")),
                                        array("superParentIds")
                                            .description("Super Parent IDs of the object.")
                                            .items(
                                                string("superParentId")
                                                    .description("Super parent ID.")),
                                        string("scope")
                                            .description("Scope of the object."),
                                        bool("hasAttachments")
                                            .description("Whether the object has attachments"),
                                        string("permalink")
                                            .description("Permalink of the object."),
                                        string("workflowId")
                                            .description("Workflow ID."),
                                        array("metadata")
                                            .description("Metadata of the object."),
                                        array("customFields")
                                            .placeholder("Custom fields of the object."),
                                        object("project")
                                            .description("Project properties.")
                                            .properties(
                                                string("authorId")
                                                    .description("Project author ID."),
                                                array("ownerIds")
                                                    .description("Owner IDs.")
                                                    .items(
                                                        string("ownerId")
                                                            .description("Owner ID.")),
                                                string("customStatusId")
                                                    .description("Custom status ID of the project."),
                                                string("startDate")
                                                    .description("Start date of the project"),
                                                string("endDate")
                                                    .description("End date of the project."),
                                                string("createdDate")
                                                    .description("Date when the project was created.")))))))
        .perform(WrikeCreateProjectAction::perform);

    private WrikeCreateProjectAction() {
    }

    protected static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        Map<String, Object> project = getProject(inputParameters);

        return context.http(
            http -> http.post("/folders/%s/folders".formatted(inputParameters.getRequiredString(PARENT_ID))))
            .configuration(responseType(ResponseType.JSON))
            .queryParameters(
                TITLE, inputParameters.getRequiredString(TITLE),
                DESCRIPTION, inputParameters.getString(DESCRIPTION),
                PROJECT, project)
            .execute()
            .getBody();
    }

    private static Map<String, Object> getProject(Parameters inputParameters) {
        Map<String, Object> project = new HashMap<>();

        addIfNotNull(inputParameters.getLocalDate(START_DATE), START_DATE, project);
        addIfNotNull(inputParameters.getLocalDate(END_DATE), END_DATE, project);
        addIfNotNull(inputParameters.getString(CONTRACT_TYPE), CONTRACT_TYPE, project);
        addIfNotNull(inputParameters.getList(OWNER_IDS, String.class), OWNER_IDS, project);
        addIfNotNull(inputParameters.getInteger(BUDGET), BUDGET, project);

        return project;
    }

    private static <T> void addIfNotNull(T value, String key, Map<String, Object> project) {
        if (value != null) {
            project.put(key, value);
        }
    }
}
