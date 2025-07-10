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

package com.bytechef.component.retable.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.Context.Http.ResponseType;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.retable.constant.RetableConstants.PROJECT_ID;
import static com.bytechef.component.retable.constant.RetableConstants.WORKSPACE_ID;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Marija Horvat
 */
public class RetableUtils {

    public static List<Option<String>> getWorkspaceOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        Map<String, Map<String, ?>> body = context.http(http -> http.get("/workspace"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        Map<String, ?> data = body.get("data");
        if (data != null && data.get("workspaces") instanceof List<?> workspaces) {
            for (Object workspace : workspaces) {
                if (workspace instanceof Map<?, ?> map) {

                    options.add(option((String) map.get("name"), (String) map.get("id")));
                }
            }
        }

        return options;
    }

    public static List<Option<String>> getProjectOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        Map<String, Map<String, ?>> body = context
            .http(http -> http.get("/workspace/%s/projects".formatted(
                inputParameters.getRequiredString(WORKSPACE_ID))))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        Map<String, ?> data = body.get("data");
        if (data != null && data.get("projects") instanceof List<?> projects) {
            for (Object project : projects) {
                if (project instanceof Map<?, ?> map) {

                    options.add(option((String) map.get("name"), (String) map.get("id")));
                }
            }
        }

        return options;
    }

    public static List<Option<String>> getRetableOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        Map<String, Map<String, ?>> body = context
            .http(http -> http.get("/project/%s/retable".formatted(
                inputParameters.getRequiredString(PROJECT_ID))))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        Map<String, ?> data = body.get("data");
        if (data != null && data.get("retables") instanceof List<?> retables) {
            for (Object retable : retables) {
                if (retable instanceof Map<?, ?> map) {

                    options.add(option((String) map.get("title"), (String) map.get("id")));
                }
            }
        }

        return options;
    }

    private RetableUtils() {
    }
}
