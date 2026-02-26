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
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.github.constant.GithubConstants.EXTENSION;
import static com.bytechef.component.github.constant.GithubConstants.FILE;
import static com.bytechef.component.github.constant.GithubConstants.FILENAME;
import static com.bytechef.component.github.constant.GithubConstants.IN;
import static com.bytechef.component.github.constant.GithubConstants.OWNER;
import static com.bytechef.component.github.constant.GithubConstants.OWNER_PROPERTY;
import static com.bytechef.component.github.constant.GithubConstants.PAGE;
import static com.bytechef.component.github.constant.GithubConstants.PATH;
import static com.bytechef.component.github.constant.GithubConstants.PER_PAGE;
import static com.bytechef.component.github.constant.GithubConstants.QUERY;
import static com.bytechef.component.github.constant.GithubConstants.REPOSITORY;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.List;

/**
 * @author Ivona Pavela
 */
public class GithubSearchCodeAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("searchCode")
        .title("Search Code")
        .description("Searches the code in repository and returns up to 100 results per page.")
        .properties(
            OWNER_PROPERTY,
            string(REPOSITORY)
                .label("Repository")
                .description("The repository in which to search for matching code.")
                .required(true),
            string(QUERY)
                .label("Query")
                .description("Query of the code in the repository.")
                .required(true),
            string(EXTENSION)
                .label("Extension")
                .description("Matches code files with a certain file extension.")
                .required(false),
            string(FILENAME)
                .label("Filename")
                .description("Matches code files with a certain filename.")
                .required(false),
            string(PATH)
                .label("Path")
                .description("Searches for source code that appears at a specific location in a repository.")
                .required(false),
            string(IN)
                .label("In")
                .description("Restricts your search to the contents of the source code file, the file path, or both.")
                .options(List.of(
                    option("File", FILE),
                    option("Path", PATH),
                    option("both", FILE + "," + PATH)))
                .required(false),
            integer(PAGE)
                .label("Page")
                .description("The page number of the results to fetch.")
                .required(false),
            integer(PER_PAGE)
                .label("Per page")
                .description("The number of results per page (max 100).")
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        integer("total_count")
                            .description("Total number of matching results."),
                        bool("incomplete_results")
                            .description("Whether the results are incomplete."),
                        array("items")
                            .description("List of code search results.")
                            .items(
                                object()
                                    .properties(
                                        string("name")
                                            .description("The name of the file."),
                                        string("path")
                                            .description("The file path in the repository."),
                                        string("sha")
                                            .description("SHA hash of the file."),
                                        string("url")
                                            .description("API URL of the file."),
                                        string("git_url")
                                            .description("Git URL of the file."),
                                        string("html_url")
                                            .description("HTML URL of the file on GitHub."),
                                        number("score")
                                            .description("Search relevance score."),
                                        object("repository")
                                            .properties(
                                                integer("id")
                                                    .description("Repository ID."),
                                                string("name")
                                                    .description("Repository name."),
                                                string("full_name")
                                                    .description("Repository full name (owner/name)."),
                                                string("html_url")
                                                    .description("Repository HTML URL."),
                                                string("url")
                                                    .description("Repository API URL.")))))))
        .perform(GithubSearchCodeAction::perform);

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {

        String query = buildQuery(inputParameters);

        return context.http(http -> http.get("/search/code"))
            .queryParameters(
                "q", query,
                PAGE, inputParameters.getInteger(PAGE),
                PER_PAGE, inputParameters.getInteger(PER_PAGE))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }

    private static String buildQuery(Parameters inputParameters) {

        StringBuilder query = new StringBuilder();

        query.append(inputParameters.getString(QUERY));
        query.append(" repo:")
            .append(inputParameters.getRequiredString(OWNER))
            .append("/")
            .append(inputParameters.getRequiredString(REPOSITORY));

        appendIfPresent(query, " extension:", inputParameters.getString(EXTENSION));
        appendIfPresent(query, " filename:", inputParameters.getString(FILENAME));
        appendIfPresent(query, " path:", inputParameters.getString(PATH));
        appendIfPresent(query, " in:", inputParameters.getString(IN));

        return query.toString()
            .trim();
    }

    private static void appendIfPresent(StringBuilder query, String prefix, String value) {
        if (value != null) {
            query.append(prefix)
                .append(value);
        }
    }
}
