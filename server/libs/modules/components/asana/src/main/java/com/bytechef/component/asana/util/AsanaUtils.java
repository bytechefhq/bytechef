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

package com.bytechef.component.asana.util;

import static com.bytechef.component.asana.constant.AsanaConstants.WORKSPACE;
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
public class AsanaUtils extends AbstractAsanaUtils {

    private AsanaUtils() {
    }

    public static List<Option<String>> getAssigneeOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        return getPaginatedOptions(
            "/users?workspace=" + inputParameters.getRequiredFromPath("data." + WORKSPACE, String.class),
            context);
    }

    public static List<Option<String>> getProjectOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        return getPaginatedOptions(
            "/projects?workspace=" + inputParameters.getRequiredFromPath("data." + WORKSPACE, String.class),
            context);
    }

    public static List<Option<String>> getTagsOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        return getPaginatedOptions(
            "/tags",
            context);
    }

    public static List<Option<String>> getTeamOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        return getPaginatedOptions(
            "/workspaces/" + inputParameters.getRequiredFromPath("data." + WORKSPACE, String.class) + "/teams",
            context);
    }

    public static List<Option<String>> getWorkspaceOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        return getPaginatedOptions(
            "/workspaces",
            context);
    }

    public static List<Option<String>> getTaskGidOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        return getPaginatedOptions(
            "/projects/" + inputParameters.getRequiredFromPath("data.project", String.class)
                + "/tasks?opt_fields=gid,name",
            context);
    }

    private static List<Option<String>> getPaginatedOptions(String baseUrl, Context context) {

        List<Option<String>> options = new ArrayList<>();
        String offset = null;
        final int limit = 100;

        do {

            StringBuilder url = new StringBuilder(baseUrl);
            url.append(baseUrl.contains("?") ? "&" : "?")
                .append("limit=")
                .append(limit);
            if (offset != null) {
                url.append("&offset=")
                    .append(offset);
            }

            Map<String, Object> response = context.http(http -> http.get(url.toString()))
                .configuration(Http.responseType(Http.ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<Map<String, Object>>() {});

            Object dataObj = response.get("data");
            addOptions(dataObj, options);

            offset = extractNextOffset(response);

        } while (offset != null);

        return options;
    }

    private static void addOptions(Object dataObj, List<Option<String>> options) {

        if (dataObj instanceof List<?> dataList) {
            for (Object obj : dataList) {
                if (obj instanceof Map<?, ?> map) {

                    String gid = String.valueOf(map.get("gid"));
                    String name = String.valueOf(map.get("name"));

                    options.add(option(name, gid));
                }
            }
        }
    }

    private static String extractNextOffset(Map<String, Object> response) {
        Object nextPageObj = response.get("next_page");

        if (nextPageObj instanceof Map<?, ?> nextPage) {
            Object offsetObj = nextPage.get("offset");
            if (offsetObj != null) {
                return offsetObj.toString();
            }
        }

        return null;
    }
}
