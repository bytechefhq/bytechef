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
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.github.constant.GithubConstants.ISSUE_OUTPUT_PROPERTY;
import static com.bytechef.component.github.constant.GithubConstants.LIST_REPOSITORY_ISSUES;
import static com.bytechef.component.github.constant.GithubConstants.LIST_REPOSITORY_ISSUES_DESCRIPTION;
import static com.bytechef.component.github.constant.GithubConstants.LIST_REPOSITORY_ISSUES_TITLE;
import static com.bytechef.component.github.constant.GithubConstants.REPOSITORY;
import static com.bytechef.component.github.util.GithubUtils.getOwnerName;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.Property.ArrayProperty;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.github.util.GithubUtils;
import com.bytechef.definition.BaseOutputDefinition.OutputSchema;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class GithubListRepositoryIssuesAction {

    @SuppressFBWarnings("MS")
    public static final Property[] PROPERTIES = {
        string(REPOSITORY)
            .label("Repository")
            .options((ActionOptionsFunction<String>) GithubUtils::getRepositoryOptions)
            .description("The name of the repository")
            .required(true)
    };

    public static final OutputSchema<ArrayProperty> OUTPUT_SCHEMA = outputSchema(
        array()
            .description("List of issues in the repository.")
            .items(ISSUE_OUTPUT_PROPERTY));

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(LIST_REPOSITORY_ISSUES)
        .title(LIST_REPOSITORY_ISSUES_TITLE)
        .description(LIST_REPOSITORY_ISSUES_DESCRIPTION)
        .properties(PROPERTIES)
        .output(OUTPUT_SCHEMA)
        .perform(GithubListRepositoryIssuesAction::perform);

    private GithubListRepositoryIssuesAction() {
    }

    public static List<Map<String, Object>> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        return context
            .http(http -> http.get(
                "/repos/" + getOwnerName(context) + "/" + inputParameters.getRequiredString(REPOSITORY) +
                    "/issues"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
