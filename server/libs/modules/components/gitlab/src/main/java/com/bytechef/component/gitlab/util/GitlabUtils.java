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

package com.bytechef.component.gitlab.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.Context.Http;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.gitlab.constant.GitlabConstants.ID;
import static com.bytechef.component.gitlab.constant.GitlabConstants.PROJECT_ID;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class GitlabUtils {

    private GitlabUtils() {
    }

    public static List<Option<String>> getIssueOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> stringStringMap, String s,
        Context context) {

        List<Map<String, Object>> body = context
            .http(http -> http.get("/projects/" + inputParameters.getRequiredString(PROJECT_ID) + "/issues"))
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        for (Map<String, Object> item : body) {
            options.add(option((String) item.get("title"), item.get("iid")
                .toString()));
        }

        return options;
    }

    public static List<Option<String>> getProjectOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> stringStringMap, String s,
        Context context) {

        List<Map<String, Object>> body = context
            .http(http -> http.get("/projects"))
            .queryParameters(
                Map.of(
                    "simple", List.of("true"),
                    "membership", List.of("true")))
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        for (Map<String, Object> item : body) {
            options.add(option((String) item.get("name"), item.get(ID)
                .toString()));
        }

        return options;
    }

}
