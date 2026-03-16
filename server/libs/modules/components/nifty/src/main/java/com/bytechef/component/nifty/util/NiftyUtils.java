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

package com.bytechef.component.nifty.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.nifty.constant.NiftyConstants.ID;
import static com.bytechef.component.nifty.constant.NiftyConstants.NAME;
import static com.bytechef.component.nifty.constant.NiftyConstants.PROJECT;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Luka Ljubić
 * @author Monika Kušter
 */
public class NiftyUtils extends AbstractNiftyUtils {

    private NiftyUtils() {
    }

    public static List<Option<String>> getAppIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, TriggerContext triggerContext) {

        return getAllOptions(triggerContext, "/apps", "apps");
    }

    public static List<Option<String>> getTaskGroupIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        return getAllOptions(
            context, "/taskgroups", "items",
            "project_id", inputParameters.getRequiredString(PROJECT), "archived", "false");
    }

    public static List<Option<String>> getProjectIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        return getAllOptions(context, "/projects", "projects");
    }

    public static List<Option<String>> getTemplateIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        return getAllOptions(context, "/templates", "items", "type", "project");
    }

    public static List<Option<String>> getTaskIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        return getAllOptions(context, "/tasks", "tasks");
    }

    public static List<Option<String>> getLabelsOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        return getAllOptions(context, "/labels", "items");
    }

    private static List<Option<String>> getAllOptions(
        Context context, String resourcePath, String resourceName, Object... additionalQueryParameters) {

        List<Option<String>> options = new ArrayList<>();
        int offset = 0;
        int limit = 100;
        boolean hasMore;

        do {
            List<Object> queryParameters = new ArrayList<>();

            queryParameters.add("limit");
            queryParameters.add(limit);
            queryParameters.add("offset");
            queryParameters.add(offset);

            queryParameters.addAll(List.of(additionalQueryParameters));

            Map<String, Object> body = context.http(http -> http.get(resourcePath))
                .queryParameters(queryParameters.toArray())
                .configuration(Http.responseType(Http.ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

            options.addAll(getOptions(body, resourceName));

            offset += limit;
            hasMore = body != null && Boolean.TRUE.equals(body.get("hasMore"));
        } while (hasMore);

        return options;
    }

    private static List<Option<String>> getOptions(Map<String, Object> body, String resource) {
        List<Option<String>> options = new ArrayList<>();

        if (body != null && body.get(resource) instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    options.add(option((String) map.get(NAME), (String) map.get(ID)));
                }
            }
        }

        return options;
    }
}
