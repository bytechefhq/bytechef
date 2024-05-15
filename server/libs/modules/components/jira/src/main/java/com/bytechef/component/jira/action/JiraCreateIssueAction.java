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

package com.bytechef.component.jira.action;

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.jira.constant.JiraConstants.ASSIGNEE;
import static com.bytechef.component.jira.constant.JiraConstants.CONTENT;
import static com.bytechef.component.jira.constant.JiraConstants.CREATE_ISSUE;
import static com.bytechef.component.jira.constant.JiraConstants.DESCRIPTION;
import static com.bytechef.component.jira.constant.JiraConstants.FIELDS;
import static com.bytechef.component.jira.constant.JiraConstants.ID;
import static com.bytechef.component.jira.constant.JiraConstants.ISSUETYPE;
import static com.bytechef.component.jira.constant.JiraConstants.KEY;
import static com.bytechef.component.jira.constant.JiraConstants.PARENT;
import static com.bytechef.component.jira.constant.JiraConstants.PRIORITY;
import static com.bytechef.component.jira.constant.JiraConstants.PROJECT;
import static com.bytechef.component.jira.constant.JiraConstants.SUMMARY;
import static com.bytechef.component.jira.constant.JiraConstants.TEXT;
import static com.bytechef.component.jira.constant.JiraConstants.TYPE;
import static com.bytechef.component.jira.util.JiraUtils.getBaseUrl;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.jira.util.JiraOptionsUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Domiter
 */
public class JiraCreateIssueAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_ISSUE)
        .title("Create issue")
        .description("Creates a new issue.")
        .properties(
            string(PROJECT)
                .label("Project Name")
                .description("The name of the project to create the issue in.")
                .options((ActionOptionsFunction<String>) JiraOptionsUtils::getProjectIdOptions)
                .required(true),
            string(SUMMARY)
                .label("Summary")
                .description("A brief summary of the issue.")
                .required(true),
            string(ISSUETYPE)
                .label("Issue type")
                .description("The type of issue.")
                .options((ActionOptionsFunction<String>) JiraOptionsUtils::getIssueTypesIdOptions)
                .optionsLookupDependsOn(PROJECT)
                .required(true),
            string(PARENT)
                .label("Parent")
                .description("")
                .displayCondition("%s == '%s'".formatted(ISSUETYPE, "10003"))
                .options((ActionOptionsFunction<String>) JiraOptionsUtils::getIssueIdOptions)
                .optionsLookupDependsOn(PROJECT)
                .required(true),
            string(ASSIGNEE)
                .label("Assignee")
                .description("User who will be assigned to the issue.")
                .options((ActionOptionsFunction<String>) JiraOptionsUtils::getUserIdOptions)
                .optionsLookupDependsOn(PROJECT)
                .required(false),
            string(PRIORITY)
                .label("Priority")
                .description("Priority of the issue.")
                .options((ActionOptionsFunction<String>) JiraOptionsUtils::getPriorityIdOptions)
                .required(false),
            string(DESCRIPTION)
                .label("Description")
                .description("Description of the issue.")
                .controlType(ControlType.TEXT_AREA)
                .required(false))
        .outputSchema(
            object()
                .properties(
                    string(ID),
                    string(KEY)))
        .perform(JiraCreateIssueAction::perform);

    private JiraCreateIssueAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        Http.Response execute = context
            .http(http -> http.post(getBaseUrl(connectionParameters) + "/issue"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .body(Http.Body.of(FIELDS, getIssueFieldsMap(inputParameters)))
            .execute();

        return execute.getBody(new TypeReference<>() {});

    }

    private static Map<String, Object> getIssueFieldsMap(Parameters inputParameters) {
        Map<String, Object> project = new HashMap<>();

        project.put(PROJECT, Map.of(ID, inputParameters.getRequiredString(PROJECT)));
        project.put(ISSUETYPE, Map.of(ID, inputParameters.getRequiredString(ISSUETYPE)));
        project.put(SUMMARY, inputParameters.getRequiredString(SUMMARY));

        addFieldIfNotNull(project, PARENT, inputParameters.getString(PARENT));
        addFieldIfNotNull(project, ASSIGNEE, inputParameters.getString(ASSIGNEE));
        addFieldIfNotNull(project, PRIORITY, inputParameters.getString(PRIORITY));
        addDescriptionField(project, inputParameters.getString(DESCRIPTION));

        return project;
    }

    private static void addFieldIfNotNull(Map<String, Object> project, String fieldName, String value) {
        if (value != null) {
            project.put(fieldName, Map.of(ID, value));
        }
    }

    private static void addDescriptionField(Map<String, Object> project, String description) {
        if (description != null) {
            project.put(DESCRIPTION, Map.of(
                CONTENT, List.of(
                    Map.of(
                        CONTENT, List.of(
                            Map.of(
                                TEXT, description,
                                TYPE, TEXT)),
                        TYPE, "paragraph")),
                TYPE, "doc",
                "version", 1));
        }
    }
}
