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

package com.bytechef.component.linear.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.linear.constant.LinearConstants.DATA;
import static com.bytechef.component.linear.constant.LinearConstants.DESCRIPTION;
import static com.bytechef.component.linear.constant.LinearConstants.NAME;
import static com.bytechef.component.linear.constant.LinearConstants.PRIORITY;
import static com.bytechef.component.linear.constant.LinearConstants.START_DATE;
import static com.bytechef.component.linear.constant.LinearConstants.STATUS_ID;
import static com.bytechef.component.linear.constant.LinearConstants.TEAM_ID;
import static com.bytechef.component.linear.util.LinearUtils.appendOptionalField;
import static com.bytechef.component.linear.util.LinearUtils.executeGraphQLQuery;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.linear.util.LinearUtils;
import java.util.Map;

/**
 * @author Marija Horvat
 */
public class LinearCreateProjectAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createProject")
        .title("Create Project")
        .description("Creates a new project.")
        .properties(
            string(NAME)
                .label("Project Name")
                .description("The name of the project.")
                .required(true),
            string(TEAM_ID)
                .label("Team ID")
                .description("The ID of the team where this project should be created.")
                .options((OptionsFunction<String>) LinearUtils::getTeamOptions)
                .required(true),
            string(STATUS_ID)
                .label("Status")
                .description("The status of the project.")
                .options((OptionsFunction<String>) LinearUtils::getProjectStateOptions)
                .required(false),
            integer(PRIORITY)
                .label("Priority")
                .description("The priority of the project.")
                .options(
                    option("No priority", 0),
                    option("Urgent", 1),
                    option("High", 2),
                    option("Normal", 3),
                    option("Low", 4))
                .defaultValue(0)
                .required(false),
            date(START_DATE)
                .label("Start Date")
                .description("The planned start date of the project.")
                .required(false),
            string(DESCRIPTION)
                .label("Description")
                .description("The detailed description of the project.")
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        bool("success")
                            .description(
                                "Whether the operation was successful."),
                        object("project")
                            .description("The project that was created or updated.")
                            .properties(
                                string("id")
                                    .description("The ID of the project."),
                                string("name")
                                    .description("The name of the project.")))))
        .perform(LinearCreateProjectAction::perform);

    private LinearCreateProjectAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String query = buildGraphQLQuery(inputParameters);
        Map<String, Object> body = executeGraphQLQuery(query, context);

        if (body.get(DATA) instanceof Map<?, ?> data &&
            data.get("projectCreate") instanceof Map<?, ?> projectCreate) {

            return projectCreate;
        }

        return null;
    }

    private static String buildGraphQLQuery(Parameters inputParameters) {
        StringBuilder sb = new StringBuilder("mutation{projectCreate(input: {");

        sb.append("name: \"")
            .append(inputParameters.getRequiredString(NAME))
            .append("\", teamIds: [\"")
            .append(inputParameters.getRequiredString(TEAM_ID))
            .append("\"], ");

        appendOptionalField(sb, STATUS_ID, inputParameters.getString(STATUS_ID));
        appendOptionalField(sb, PRIORITY, inputParameters.getInteger(PRIORITY));
        appendOptionalField(sb, START_DATE, inputParameters.getString(START_DATE));
        appendOptionalField(sb, DESCRIPTION, inputParameters.getString(DESCRIPTION));

        String query = sb.toString();

        if (query.endsWith(", ")) {
            sb.setLength(sb.length() - 2);
        }

        sb.append("}){success project{id name}}}");

        return sb.toString();
    }
}
