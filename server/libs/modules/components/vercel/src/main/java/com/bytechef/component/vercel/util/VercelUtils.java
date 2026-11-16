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

package com.bytechef.component.vercel.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.vercel.constant.VercelConstants.TEAM_ID;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class VercelUtils {

    private static final int MAX_PAGES = 50;

    private VercelUtils() {
    }

    public static String encodePathSegment(String value) {
        String encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8);

        return encodedValue.replace("+", "%20");
    }

    public static Executor withTeamId(Executor executor, String teamId) {
        if (teamId == null || teamId.isBlank()) {
            return executor;
        }

        return executor.queryParameters(TEAM_ID, teamId);
    }

    private static String toFullUrl(Object url) {
        if (url == null) {
            return null;
        }

        String value = url.toString();

        if (value.startsWith("http://") || value.startsWith("https://")) {
            return value;
        }

        return "https://" + value;
    }

    public static Map<String, Object> toDeploymentSummary(Map<String, Object> deployment) {
        Map<String, Object> summary = new HashMap<>();

        if (deployment == null) {
            return summary;
        }

        summary.put("id", deployment.get("id"));
        summary.put("url", toFullUrl(deployment.get("url")));
        summary.put("state", deployment.get("readyState"));
        summary.put("errorCode", deployment.get("errorCode"));
        summary.put("errorMessage", deployment.get("errorMessage"));

        return summary;
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> getProjects(Context context, String teamId, String search) {
        List<Map<String, Object>> projects = new ArrayList<>();
        String cursor = null;

        for (int page = 0; page < MAX_PAGES; page++) {
            List<Object> queryParameters = new ArrayList<>();

            if (teamId != null && !teamId.isBlank()) {
                queryParameters.add(TEAM_ID);
                queryParameters.add(teamId);
            }

            if (search != null && !search.isBlank()) {
                queryParameters.add("search");
                queryParameters.add(search);
            }

            if (cursor != null) {
                queryParameters.add("from");
                queryParameters.add(cursor);
            }

            Executor executor = context.http(http -> http.get("/v10/projects"));

            if (!queryParameters.isEmpty()) {
                executor = executor.queryParameters(queryParameters.toArray());
            }

            Object body = executor
                .configuration(Http.responseType(Http.ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

            Object projectList = body;
            cursor = null;

            if (body instanceof Map<?, ?> bodyMap) {
                projectList = bodyMap.get("projects");

                if (bodyMap.get("pagination") instanceof Map<?, ?> pagination && pagination.get("next") != null) {
                    cursor = String.valueOf(pagination.get("next"));
                }
            }

            if (projectList instanceof List<?> items) {
                for (Object item : items) {
                    Map<String, Object> projectMap = (Map<String, Object>) item;
                    Map<String, Object> summary = new HashMap<>();

                    summary.put("id", projectMap.get("id"));
                    summary.put("name", projectMap.get("name"));

                    projects.add(summary);
                }
            }

            if (cursor == null) {
                break;
            }
        }

        return projects;
    }

    public static List<Option<String>> getProjectIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        List<Option<String>> options = new ArrayList<>();

        for (Map<String, Object> project : getProjects(context, inputParameters.getString(TEAM_ID), searchText)) {
            options.add(option((String) project.get("name"), (String) project.get("id")));
        }

        return options;
    }
}
