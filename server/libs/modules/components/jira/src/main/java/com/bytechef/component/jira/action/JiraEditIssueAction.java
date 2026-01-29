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
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.jira.constant.JiraConstants.ADD;
import static com.bytechef.component.jira.constant.JiraConstants.ADD_LABELS;
import static com.bytechef.component.jira.constant.JiraConstants.DESCRIPTION;
import static com.bytechef.component.jira.constant.JiraConstants.FIELDS;
import static com.bytechef.component.jira.constant.JiraConstants.ISSUE_ID;
import static com.bytechef.component.jira.constant.JiraConstants.PROJECT;
import static com.bytechef.component.jira.constant.JiraConstants.REMOVE;
import static com.bytechef.component.jira.constant.JiraConstants.REMOVE_LABELS;
import static com.bytechef.component.jira.constant.JiraConstants.SUMMARY;
import static com.bytechef.component.jira.constant.JiraConstants.UPDATE;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.jira.util.JiraOptionsUtils;
import com.bytechef.component.jira.util.JiraUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ivona Pavela
 */
public class JiraEditIssueAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("editIssue")
        .title("Edit Issue")
        .description("Edits an issue.")
        .properties(
            string(PROJECT)
                .label("Project ID")
                .description("ID of the project where the issue is located.")
                .options((ActionDefinition.OptionsFunction<String>) JiraOptionsUtils::getProjectIdOptions)
                .required(true),
            string(ISSUE_ID)
                .label("Issue ID")
                .description("ID of the issue.")
                .options((ActionDefinition.OptionsFunction<String>) JiraOptionsUtils::getIssueIdOptions)
                .optionsLookupDependsOn(PROJECT)
                .required(true),
            string(SUMMARY)
                .label("Summary")
                .description("The summary that will be edited.")
                .required(false),
            string(DESCRIPTION)
                .label("Description")
                .description("The description that will be edited.")
                .required(false),
            array(ADD_LABELS)
                .label("Add labels")
                .description("List of labels that will be added to the issue.")
                .items(
                    string()
                        .label("Label")
                        .description("Label that will be added to the issue.")
                        .required(false))
                .required(false),
            array(REMOVE_LABELS)
                .label("Remove labels")
                .description("List of labels that will be removed from the issue.")
                .items(
                    string()
                        .label("Label")
                        .description("Label that will be removed from the issue.")
                        .required(false))
                .required(false))
        .output(outputSchema(bool().description("Returns true if the issue was edited successfully.")))
        .perform(JiraEditIssueAction::perform);

    public static Boolean perform(Parameters inputParameters, Parameters connectionParameters, Context context) {

        Map<String, Object> body = addBody(inputParameters);

        context.http(http -> http.put("/issue/" + inputParameters.getRequiredString(ISSUE_ID)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .body(Http.Body.of(body))
            .execute();

        return true;
    }

    private static Map<String, Object> addBody(Parameters inputParameters) {

        Map<String, Object> fields = new HashMap<>();
        String summary = inputParameters.getString(SUMMARY);

        if (summary != null) {
            fields.put(SUMMARY, summary);
        }

        JiraUtils.addDescriptionField(fields, inputParameters.getString(DESCRIPTION));

        List<Map<String, Object>> labelsToUpdate = new ArrayList<>();

        addLabels(labelsToUpdate, ADD, inputParameters.getList(ADD_LABELS, String.class));
        addLabels(labelsToUpdate, REMOVE, inputParameters.getList(REMOVE_LABELS, String.class));

        return Map.of(
            FIELDS, fields,
            UPDATE, Map.of("labels", labelsToUpdate));
    }

    private static void addLabels(List<Map<String, Object>> labelsToUpdate, String operation, List<String> labels) {

        if (labels != null) {
            labels.forEach(label -> labelsToUpdate.add(Map.of(operation, label)));
        }
    }
}
