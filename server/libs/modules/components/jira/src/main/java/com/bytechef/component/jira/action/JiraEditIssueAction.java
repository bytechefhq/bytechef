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

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
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
        .help("", "https://docs.bytechef.io/reference/components/jira_v1#edit-issue")
        .properties(
            string(PROJECT)
                .label("Project ID")
                .description("ID of the project where the issue is located.")
                .options((OptionsFunction<String>) JiraOptionsUtils::getProjectIdOptions)
                .required(false),
            string(ISSUE_ID)
                .label("Issue ID")
                .description("ID of the issue.")
                .options((OptionsFunction<String>) JiraOptionsUtils::getIssueIdOptions)
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
                .label("Add Labels")
                .description("List of labels that will be added to the issue.")
                .items(
                    string()
                        .label("Label")
                        .description("Label that will be added to the issue.")
                        .required(false))
                .required(false),
            array(REMOVE_LABELS)
                .label("Remove Labels")
                .description("List of labels that will be removed from the issue.")
                .items(
                    string()
                        .label("Label")
                        .description("Label that will be removed from the issue.")
                        .required(false))
                .required(false))
        .perform(JiraEditIssueAction::perform);

    private JiraEditIssueAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        context.http(http -> http.put("/issue/" + inputParameters.getRequiredString(ISSUE_ID)))
            .body(
                Http.Body.of(
                    FIELDS, createFieldsMap(inputParameters),
                    UPDATE, Map.of("labels", getLabelsToUpdate(inputParameters))))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute();

        return null;
    }

    private static Map<String, Object> createFieldsMap(Parameters inputParameters) {
        Map<String, Object> map = new HashMap<>();

        if (inputParameters.getString(SUMMARY) != null) {
            map.put(SUMMARY, inputParameters.getString(SUMMARY));
        }

        JiraUtils.addDescriptionField(map, inputParameters.getString(DESCRIPTION));

        return map;
    }

    private static List<Map<String, Object>> getLabelsToUpdate(Parameters inputParameters) {
        List<Map<String, Object>> labels = new ArrayList<>();

        labels.addAll(getLabels(ADD, inputParameters.getList(ADD_LABELS, String.class)));
        labels.addAll(getLabels(REMOVE, inputParameters.getList(REMOVE_LABELS, String.class)));

        return labels;
    }

    private static List<Map<String, Object>> getLabels(String operation, List<String> labels) {
        if (labels == null) {
            return List.of();
        }

        return labels.stream()
            .map(label -> Map.<String, Object>of(operation, label))
            .toList();
    }
}
