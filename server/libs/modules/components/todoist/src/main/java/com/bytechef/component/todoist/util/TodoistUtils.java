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

package com.bytechef.component.todoist.util;

import static com.bytechef.component.definition.ComponentDsl.option;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class TodoistUtils extends AbstractTodoistUtils {

    private TodoistUtils() {
    }

    public static List<Option<String>> getLabelsOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        return getOptions(context, "/labels", "name");
    }

    public static List<Option<String>> getProjectIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        return getOptions(context, "/projects", "name");
    }

    public static List<Option<String>> getSectionIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        return getOptions(context, "/sections", "name", "project_id", inputParameters.getString("project_id"));
    }

    public static List<Option<String>> getTaskIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        return getOptions(context, "/tasks", "content");
    }

    public static List<Option<Long>> getWorkspaceIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        List<Map<String, ?>> body = context
            .http(http -> http.get("/workspaces"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<Long>> options = new ArrayList<>();

        for (Map<String, ?> workspace : body) {
            options.add(option((String) workspace.get("name"), ((Integer) workspace.get("id")).intValue()));
        }

        return options;
    }

    private static List<Option<String>> getOptions(
        Context context, String path, String label, Object... additionalQueryParameters) {

        List<Option<String>> options = new ArrayList<>();
        String cursor = null;

        do {
            List<Object> queryParameters = new ArrayList<>();

            queryParameters.add("cursor");
            queryParameters.add(cursor);
            queryParameters.add("limit");
            queryParameters.add(200);

            if (additionalQueryParameters.length > 0) {
                queryParameters.addAll(List.of(additionalQueryParameters));
            }

            Map<String, ?> body = context
                .http(http -> http.get(path))
                .queryParameters(queryParameters.toArray())
                .configuration(Http.responseType(Http.ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

            if (body.get("results") instanceof List<?> results) {
                for (Object result : results) {
                    if (result instanceof Map<?, ?> map) {
                        options.add(option((String) map.get(label), (String) map.get("id")));
                    }
                }
            }

            cursor = (String) body.get("next_cursor");
        } while (cursor != null);

        return options;
    }
}
