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

package com.bytechef.component.jira.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.jira.constant.JiraConstants.FIELDS;
import static com.bytechef.component.jira.constant.JiraConstants.ID;
import static com.bytechef.component.jira.constant.JiraConstants.ISSUES;
import static com.bytechef.component.jira.constant.JiraConstants.JQL;
import static com.bytechef.component.jira.constant.JiraConstants.NAME;
import static com.bytechef.component.jira.constant.JiraConstants.PROJECT;
import static com.bytechef.component.jira.constant.JiraConstants.SUMMARY;
import static com.bytechef.component.jira.util.JiraUtils.getProjectName;

import com.bytechef.component.definition.ActionContext;
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
public class JiraOptionsUtils {

    private JiraOptionsUtils() {
    }

    public static List<Option<String>> getIssueIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, ActionContext context) {

        List<Option<String>> options = new ArrayList<>();

        Map<String, Object> body = context
            .http(http -> http.get("/search/jql"))
            .queryParameters(
                JQL, PROJECT + "=\"" + getProjectName(inputParameters, connectionParameters, context) + "\"",
                FIELDS, SUMMARY)
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (body.get(ISSUES) instanceof List<?> list) {
            for (Object object : list) {
                if (object instanceof Map<?, ?> map && map.get(FIELDS) instanceof Map<?, ?> fields) {
                    options.add(option((String) fields.get(SUMMARY), (String) map.get(ID)));
                }
            }
        }

        return options;
    }

    public static List<Option<String>> getIssueTypesIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        List<Object> body = context
            .http(http -> http.get("/issuetype/project"))
            .queryParameter("projectId", inputParameters.getRequiredString(PROJECT))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return getOptions(body);
    }

    public static List<Option<String>> getPriorityIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, ActionContext context) {

        List<Object> body = context
            .http(http -> http.get("/priority"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return getOptions(body);
    }

    public static List<Option<String>> getProjectIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        List<Option<String>> options = new ArrayList<>();

        Map<String, Object> body = context
            .http(http -> http.get("/project/search"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (body.get("values") instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    options.add(option((String) map.get(NAME), (String) map.get(ID)));
                }
            }
        }

        return options;
    }

    public static List<Option<String>> getUserIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, ActionContext context) {

        List<Option<String>> options = new ArrayList<>();

        List<Object> body = context
            .http(http -> http.get("/users/search"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        for (Object object : body) {
            if (object instanceof Map<?, ?> map) {
                String displayName = (String) map.get("displayName");
                options.add(option(displayName == null ? "User" : displayName, (String) map.get("accountId")));
            }
        }

        return options;
    }

    private static List<Option<String>> getOptions(List<Object> body) {
        List<Option<String>> options = new ArrayList<>();

        for (Object object : body) {
            if (object instanceof Map<?, ?> map) {
                options.add(option((String) map.get(NAME), (String) map.get(ID)));
            }
        }

        return options;
    }
}
