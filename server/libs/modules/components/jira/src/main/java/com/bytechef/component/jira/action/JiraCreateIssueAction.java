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

package com.bytechef.component.jira.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.jira.constant.JiraConstants.ASSIGNEE;
import static com.bytechef.component.jira.constant.JiraConstants.DESCRIPTION;
import static com.bytechef.component.jira.constant.JiraConstants.FIELDS;
import static com.bytechef.component.jira.constant.JiraConstants.ID;
import static com.bytechef.component.jira.constant.JiraConstants.ISSUETYPE;
import static com.bytechef.component.jira.constant.JiraConstants.KEY;
import static com.bytechef.component.jira.constant.JiraConstants.PARENT;
import static com.bytechef.component.jira.constant.JiraConstants.PRIORITY;
import static com.bytechef.component.jira.constant.JiraConstants.PROJECT;
import static com.bytechef.component.jira.constant.JiraConstants.SELF;
import static com.bytechef.component.jira.constant.JiraConstants.SUMMARY;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.jira.util.JiraOptionsUtils;
import com.bytechef.component.jira.util.JiraUtils;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Monika Domiter
 */
public class JiraCreateIssueAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createIssue")
        .title("Create Issue")
        .description("Creates a new issue.")
        .help("", "https://docs.bytechef.io/reference/components/jira_v1#create-issue")
        .properties(
            string(PROJECT)
                .label("Project ID")
                .description("ID of the project to create the issue in.")
                .options((OptionsFunction<String>) JiraOptionsUtils::getProjectIdOptions)
                .required(true),
            string(SUMMARY)
                .label("Summary")
                .description("A brief summary of the issue.")
                .required(true),
            string(ISSUETYPE)
                .label("Issue Type ID")
                .description("ID of the issue type.")
                .options((OptionsFunction<String>) JiraOptionsUtils::getIssueTypesIdOptions)
                .optionsLookupDependsOn(PROJECT)
                .required(true),
            string(PARENT)
                .label("Parent Issue ID")
                .description("ID of the parent issue.")
                .displayCondition("%s == '%s'".formatted(ISSUETYPE, "10003"))
                .options((OptionsFunction<String>) JiraOptionsUtils::getIssueIdOptions)
                .optionsLookupDependsOn(PROJECT)
                .required(true),
            string(ASSIGNEE)
                .label("Assignee ID")
                .description("ID of the user who will be assigned to the issue.")
                .options((OptionsFunction<String>) JiraOptionsUtils::getUserIdOptions)
                .required(false),
            string(PRIORITY)
                .label("Priority ID")
                .description("ID of the priority of the issue.")
                .options((OptionsFunction<String>) JiraOptionsUtils::getPriorityIdOptions)
                .required(false),
            string(DESCRIPTION)
                .label("Description")
                .description("Description of the issue.")
                .controlType(ControlType.TEXT_AREA)
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        string(ID)
                            .description("The ID of the created issue or subtask."),
                        string(KEY)
                            .description("The key of the created issue or subtask."),
                        string(SELF)
                            .description("The URL of the created issue or subtask."))))
        .perform(JiraCreateIssueAction::perform);

    private JiraCreateIssueAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context.http(http -> http.post("/issue"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .body(Http.Body.of(FIELDS, getIssueFieldsMap(inputParameters)))
            .execute()
            .getBody();
    }

    private static Map<String, Object> getIssueFieldsMap(Parameters inputParameters) {
        Map<String, Object> project = new HashMap<>();

        project.put(PROJECT, Map.of(ID, inputParameters.getRequiredString(PROJECT)));
        project.put(ISSUETYPE, Map.of(ID, inputParameters.getRequiredString(ISSUETYPE)));
        project.put(SUMMARY, inputParameters.getRequiredString(SUMMARY));

        addFieldIfNotNull(project, PARENT, inputParameters.getString(PARENT));
        addFieldIfNotNull(project, ASSIGNEE, inputParameters.getString(ASSIGNEE));
        addFieldIfNotNull(project, PRIORITY, inputParameters.getString(PRIORITY));
        JiraUtils.addDescriptionField(project, inputParameters.getString(DESCRIPTION));

        return project;
    }

    private static void addFieldIfNotNull(Map<String, Object> project, String fieldName, String value) {
        if (value != null) {
            project.put(fieldName, Map.of(ID, value));
        }
    }
}
