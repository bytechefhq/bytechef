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

package com.bytechef.component.pipeliner.util;

import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.pipeliner.constant.PipelinerConstants.SERVER_URL;
import static com.bytechef.component.pipeliner.constant.PipelinerConstants.SPACE_ID;

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
public class PipelinerUtils {

    private PipelinerUtils() {
    }

    public static List<Option<String>> getActivityTypeIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) {

        Map<String, ?> body = context
            .http(http -> http.get(getUrl(connectionParameters, "TaskTypes")))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (body.get("data") instanceof List<?> list) {
            return getOptions(list);
        } else {
            return List.of();
        }
    }

    public static List<Option<String>> getOwnerIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) {

        Map<String, ?> body = context
            .http(http -> http.get(getUrl(connectionParameters, "Clients")))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        if (body.get("data") instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    options.add(option(map.get("first_name") + " " + map.get("last_name"), (String) map.get("id")));
                }
            }
        }

        return options;
    }

    public static List<Option<String>> getSalesUnitsIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) {

        Map<String, ?> body = context
            .http(http -> http.get(getUrl(connectionParameters, "SalesUnits")))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (body.get("data") instanceof List<?> list) {
            return getOptions(list);
        } else {
            return List.of();
        }
    }

    private static List<Option<String>> getOptions(List<?> list) {
        List<Option<String>> options = new ArrayList<>();

        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                options.add(option((String) map.get("name"), (String) map.get("id")));
            }
        }

        return options;
    }

    private static String getUrl(Parameters connectionParameters, String resource) {
        return connectionParameters.getRequiredString(SERVER_URL) + connectionParameters.getRequiredString(SPACE_ID) +
            "/entities/" + resource;
    }
}
