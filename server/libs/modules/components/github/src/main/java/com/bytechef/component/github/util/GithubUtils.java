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

package com.bytechef.component.github.util;

import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.Context.Http;
import static com.bytechef.component.github.constant.GithubConstants.BASE_URL;
import static com.bytechef.component.github.constant.GithubConstants.REPO;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GithubUtils {

    private GithubUtils() {
    }

    public static List<Option<String>> getRepositoryOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> stringStringMap, String s,
        ActionContext context) {

        List<Map<String, Object>> body = context.http(http -> http.get(BASE_URL + "/user" + "/repos"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        for (Object item : body) {
            if (item instanceof Map<?, ?> map) {
                options.add(option((String) map.get("name"), (String) map.get("name")));
            }
        }

        return options;
    }

    public static List<Option<String>> getIssueOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> stringStringMap, String s,
        ActionContext context) {

        List<Map<String, Object>> body = context.http(http -> http.get(
            BASE_URL + "/repos/" + getOwnerName(context) + "/" + inputParameters.getRequiredString(REPO)
                + "/issues"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        for (Object item : body) {
            if (item instanceof Map<?, ?> map) {
                options.add(option((String) map.get("title"), String.valueOf(map.get("number"))));
            }
        }

        return options;
    }

    public static String getOwnerName(ActionContext context) {

        Map<String, Object> body = context
            .http(http -> http.get(BASE_URL + "/user"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return (String) body.get("login");
    }
}
