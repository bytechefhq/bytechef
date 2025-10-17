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
import static com.bytechef.component.github.constant.GithubConstants.REPOSITORY;
import static com.bytechef.component.github.constant.GithubConstants.TITLE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
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
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> stringStringMap, String s,
        ActionContext context) {

        List<Map<String, Object>> body = context
            .http(http -> http.get(
                "/repos/" + getOwnerName(context) + "/" + inputParameters.getRequiredString(REPOSITORY)
                    + "/collaborators"))
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return getOptions(body, NAME, "login");
    }

    public static List<Option<String>> getIssueOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> stringStringMap, String s,
        ActionContext context) {

        List<Map<String, Object>> body = context
            .http(http -> http.get(
                "/repos/" + getOwnerName(context) + "/" + inputParameters.getRequiredString(REPOSITORY) + "/issues"))
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return getOptions(body, TITLE, "number");
    }

    public static List<Option<String>> getLabels(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> stringStringMap, String s,
        ActionContext context) {

        List<Map<String, Object>> body = context
            .http(http -> http.get(
                "/repos/" + getOwnerName(context) + "/" + inputParameters.getRequiredString(REPOSITORY)
                    + "/labels"))
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return getOptions(body, NAME, NAME);
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
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> stringStringMap, String s,
        Context context) {

        List<Map<String, Object>> body = context.http(http -> http.get("/user/repos"))
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return getOptions(body, NAME, NAME);
    }

    private static List<Option<String>> getOptions(List<Map<String, Object>> body, String label, String value) {
        List<Option<String>> options = new ArrayList<>();

        for (Object item : body) {
            if (item instanceof Map<?, ?> map) {
                options.add(option((String) map.get(label), String.valueOf(map.get(value))));
            }
        }
        return options;
    }
}
