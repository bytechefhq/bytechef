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

package com.bytechef.component.asana.util;

import static com.bytechef.component.asana.constant.AsanaConstants.BASE_URL;
import static com.bytechef.component.asana.constant.AsanaConstants.WORKSPACE;
import static com.bytechef.component.definition.ComponentDSL.option;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Domiter
 */
public class AsanaUtils {

    private AsanaUtils() {
    }

    public static List<Option<String>> getAssigneeOptions(
        Parameters inputParameters, Parameters connectionParameters, String searchText, ActionContext context) {

        Map<String, List<Map<String, String>>> body = context
            .http(http -> http.get(BASE_URL + "/users?workspace=" + inputParameters.getRequiredString(WORKSPACE)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return getOptions(body);
    }

    public static List<Option<String>> getProjectIdOptions(
        Parameters inputParameters, Parameters connectionParameters, String searchText, ActionContext context) {

        Map<String, List<Map<String, String>>> body = context
            .http(http -> http.get(BASE_URL + "/projects?workspace=" + inputParameters.getRequiredString(WORKSPACE)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return getOptions(body);
    }

    public static List<Option<String>> getTagOptions(
        Parameters inputParameters, Parameters connectionParameters, String searchText, ActionContext context) {

        Map<String, List<Map<String, String>>> body = context
            .http(http -> http.get(BASE_URL + "/tags"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return getOptions(body);
    }

    public static List<Option<String>> getTeamOptions(
        Parameters inputParameters, Parameters connectionParameters, String searchText, ActionContext context) {

        Map<String, List<Map<String, String>>> body = context
            .http(http -> http.get(BASE_URL + "/workspaces/" + inputParameters.getRequiredString(WORKSPACE) + "/teams"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return getOptions(body);
    }

    public static List<Option<String>> getWorkspaceIdOptions(
        Parameters inputParameters, Parameters connectionParameters, String searchText, ActionContext context) {

        Map<String, List<Map<String, String>>> body = context
            .http(http -> http.get(BASE_URL + "/workspaces"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return getOptions(body);
    }

    private static List<Option<String>> getOptions(Map<String, List<Map<String, String>>> body) {
        List<Option<String>> options = new ArrayList<>();

        for (Map<String, String> map : body.get("data")) {
            options.add(option(map.get("name"), map.get("gid")));
        }

        return options;
    }
}
