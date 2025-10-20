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

package com.bytechef.component.github.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.Context.Http;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.github.constant.GithubConstants.NAME;
import static com.bytechef.component.github.constant.GithubConstants.OWNER;
import static com.bytechef.component.github.constant.GithubConstants.REPOSITORY;
import static com.bytechef.component.github.constant.GithubConstants.TITLE;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author Luka Ljubić
 * @author Monika Kušter
 */
public class GithubUtils {

    private GithubUtils() {
    }

    public static List<Option<String>> getCollaborators(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        String url = "/repos/%s/%s/collaborators".formatted(
            getOwnerName(context), inputParameters.getRequiredString(REPOSITORY));

        List<Map<String, ?>> collaborators = getItems(context, url);

        return getOptions(collaborators, "login");
    }

    public static List<Option<String>> getIssueOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        List<Option<String>> options = new ArrayList<>();

        List<Map<?, ?>> issues = getRepositoryIssues(inputParameters, context);

        for (Map<?, ?> issue : issues) {
            String number = String.valueOf(issue.get("number"));

            options.add(option(number + " - " + issue.get(TITLE), number));
        }

        return options;
    }

    public static List<Option<String>> getLabels(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        String url = "/repos/%s/%s/labels".formatted(
            getOwnerName(context), inputParameters.getRequiredString(REPOSITORY));

        List<Map<String, ?>> labels = getItems(context, url);

        return getOptions(labels, NAME);
    }

    public static String getOwnerName(Context context) {
        Map<String, Object> body = context
            .http(http -> http.get("/user"))
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return (String) body.get("login");
    }

    public static List<Option<String>> getRepositoryOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        List<Map<String, ?>> repos = getItems(context, "/user/repos");

        return getOptions(repos, NAME);
    }

    private static List<Option<String>> getOptions(List<Map<String, ?>> body, String value) {
        List<Option<String>> options = new ArrayList<>();

        for (Object item : body) {
            if (item instanceof Map<?, ?> map) {
                options.add(option((String) map.get(NAME), String.valueOf(map.get(value))));
            }
        }

        return options;
    }

    public static List<Map<?, ?>> getRepositoryIssues(Parameters inputParameters, Context context) {
        List<Map<?, ?>> issues = new ArrayList<>();

        String url = "/repos/%s/%s/issues".formatted(
            inputParameters.getString(OWNER, getOwnerName(context)), inputParameters.getRequiredString(REPOSITORY));
        List<Map<String, ?>> items = getItems(context, url, "state", "open");

        for (Map<String, ?> item : items) {
            Object pullRequest = item.get("pull_request");

            if (pullRequest == null) {
                issues.add(item);
            }
        }

        return issues;
    }

    public static List<Map<String, ?>> getItems(Context context, String url, Object... queryParameters) {
        List<Map<String, ?>> items = new ArrayList<>();

        int page = 1;
        boolean hasMoreItems = false;

        do {
            List<Object> allQueryParameters = new ArrayList<>(List.of("per_page", 100, "page", page++));

            if (queryParameters != null && queryParameters.length % 2 == 0) {
                allQueryParameters.addAll(Arrays.asList(queryParameters));
            }

            Http.Response response = context
                .http(http -> http.get(url))
                .queryParameters(allQueryParameters.toArray())
                .configuration(responseType(Http.ResponseType.JSON))
                .execute();

            items.addAll(response.getBody(new TypeReference<>() {}));

            List<String> header = response.getHeader("link");
            if (header != null && !header.isEmpty()) {
                String link = header.getFirst();

                hasMoreItems = link != null && link.contains("rel=\"next\"");
            }

        } while (hasMoreItems);

        return items;
    }
}
