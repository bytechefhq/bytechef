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

package com.bytechef.component.github.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.github.constant.GithubConstants.ID;
import static com.bytechef.component.github.constant.GithubConstants.ISSUE;
import static com.bytechef.component.github.constant.GithubConstants.LABELS;
import static com.bytechef.component.github.constant.GithubConstants.REPOSITORY;
import static com.bytechef.component.github.util.GithubUtils.getOwnerName;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.github.util.GithubUtils;
import java.util.Map;

/**
 * @author Mayank Madan
 */
public class GithubAddLabelsToIssueAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("addLabelsToIssue")
        .title("Add Labels to Issue")
        .description("Adds labels to the specified issue.")
        .properties(
            string(REPOSITORY)
                .options((ActionOptionsFunction<String>) GithubUtils::getRepositoryOptions)
                .label("Repository")
                .required(true),
            string(ISSUE)
                .options((ActionOptionsFunction<String>) GithubUtils::getIssueOptions)
                .optionsLookupDependsOn(REPOSITORY)
                .label("Issue")
                .description("The issue to add labels to.")
                .required(true),
            array(LABELS)
                .label("Labels")
                .description("The list of labels to add to the issue.")
                .items(
                    string("label")
                        .label("Label")
                        .options((ActionOptionsFunction<String>) GithubUtils::getLabels)
                        .optionsLookupDependsOn(REPOSITORY)
                        .required(true))
                .required(true))
        .output(outputSchema(
            array()
                .items(
                    object()
                        .properties(
                            string(ID),
                            string("name"),
                            string("description")))))
        .perform(GithubAddLabelsToIssueAction::perform);

    private GithubAddLabelsToIssueAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        return context
            .http(http -> http.post(
                "/repos/" + getOwnerName(context) + "/" + inputParameters.getRequiredString(REPOSITORY)
                    + "/issues/" + inputParameters.getRequiredString(ISSUE) + "/labels"))
            .body(
                Http.Body.of(
                    Map.of(LABELS, inputParameters.getRequiredList(LABELS, String.class))))
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
