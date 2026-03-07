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

package com.bytechef.component.github.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.github.constant.GithubConstants.ASSIGNEES;
import static com.bytechef.component.github.constant.GithubConstants.BODY;
import static com.bytechef.component.github.constant.GithubConstants.ISSUE;
import static com.bytechef.component.github.constant.GithubConstants.ISSUE_OUTPUT_PROPERTY;
import static com.bytechef.component.github.constant.GithubConstants.LABELS;
import static com.bytechef.component.github.constant.GithubConstants.MILESTONE;
import static com.bytechef.component.github.constant.GithubConstants.OWNER;
import static com.bytechef.component.github.constant.GithubConstants.OWNER_PROPERTY;
import static com.bytechef.component.github.constant.GithubConstants.REPOSITORY;
import static com.bytechef.component.github.constant.GithubConstants.STATE;
import static com.bytechef.component.github.constant.GithubConstants.TITLE;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.github.util.GithubUtils;
import java.util.List;
import java.util.Map;

/**
 * @author Ivona Pavela
 */
public class GithubUpdateIssueAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("updateIssue")
        .title("Update Issue")
        .description("Update the details of an existing issue within a specified repository.")
        .help("", "https://docs.bytechef.io/reference/components/github_v1#update-issue")
        .properties(
            OWNER_PROPERTY,
            string(REPOSITORY)
                .label("Repository")
                .description("The repository where issue is located.")
                .required(true),
            string(ISSUE)
                .label("Issue Number")
                .description("The number of the issue you want to update.")
                .options((OptionsFunction<String>) GithubUtils::getIssueOptions)
                .optionsLookupDependsOn(REPOSITORY, OWNER)
                .required(true),
            string(TITLE)
                .label("Title")
                .description("The new title of the issue.")
                .required(false),
            string(BODY)
                .label("Body")
                .description("The updated description of the issue.")
                .required(false),
            string(STATE)
                .label("State")
                .description("The new state of the issue (open/closed).")
                .options(List.of(
                    option("Open", "open"),
                    option("Closed", "closed")))
                .required(false),
            string(MILESTONE)
                .label("Milestone")
                .description(
                    "The number of the milestone to associate this issue with or use null to remove the current milestone.")
                .required(false),
            array(LABELS)
                .label("Labels")
                .description("A list of labels to associate with this issue.")
                .items(string())
                .options((OptionsFunction<String>) GithubUtils::getLabels)
                .optionsLookupDependsOn(REPOSITORY)
                .required(false),
            array(ASSIGNEES)
                .label("Assignees")
                .description("A list of usernames to assign this issue.")
                .items(string())
                .maxItems(10)
                .options((OptionsFunction<String>) GithubUtils::getCollaborators)
                .optionsLookupDependsOn(REPOSITORY)
                .required(false))
        .output(outputSchema(ISSUE_OUTPUT_PROPERTY))
        .perform(GithubUpdateIssueAction::perform);

    public static Map<String, Object>
        perform(Parameters inputParameters, Parameters connectionParameters, Context context) {

        String json = buildJson(inputParameters);

        return context
            .http(http -> http.patch(
                "/repos/" + inputParameters.getRequiredString(OWNER) + "/"
                    + inputParameters.getRequiredString(REPOSITORY) + "/issues/"
                    + inputParameters.getRequiredString(ISSUE)))
            .body(Body.of(json))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }

    private static String buildJson(Parameters inputParameters) {
        StringBuilder json = new StringBuilder();
        json.append("{");

        java.util.function.BiConsumer<String, String> appendStringField = (key, value) -> {
            if (value != null) {
                json.append("\"")
                    .append(key)
                    .append("\":")
                    .append("\"")
                    .append(value)
                    .append("\",");
            }
        };

        java.util.function.BiConsumer<String, Object[]> appendArrayField = (key, array) -> {
            if (array != null && array.length > 0) {
                json.append("\"")
                    .append(key)
                    .append("\":[");
                for (Object item : array) {
                    json.append("\"")
                        .append(item.toString())
                        .append("\",");
                }
                json.setLength(json.length() - 1);
                json.append("],");
            }
        };

        appendStringField.accept(TITLE, inputParameters.getString(TITLE));
        appendStringField.accept(BODY, inputParameters.getString(BODY));
        appendStringField.accept(STATE, inputParameters.getString(STATE));

        appendArrayField.accept(LABELS, inputParameters.getArray(LABELS));
        appendArrayField.accept(ASSIGNEES, inputParameters.getArray(ASSIGNEES));

        if (inputParameters.containsKey(MILESTONE)) {
            String milestoneValue = inputParameters.getString(MILESTONE);
            if (milestoneValue != null) {
                json.append("\"")
                    .append(MILESTONE)
                    .append("\":");
                json.append("null".equalsIgnoreCase(milestoneValue) ? "null," : milestoneValue + ",");
            }
        }

        if (json.charAt(json.length() - 1) == ',') {
            json.setLength(json.length() - 1);
        }

        json.append("}");
        return json.toString();
    }
}
