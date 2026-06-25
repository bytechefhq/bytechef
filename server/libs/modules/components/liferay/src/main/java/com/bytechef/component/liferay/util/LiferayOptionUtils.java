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

package com.bytechef.component.liferay.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.liferay.constant.LiferayConstants.APPLICATION;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Marija Horvat
 * @author Nikolina Spehar
 */
public class LiferayOptionUtils {

    private LiferayOptionUtils() {
    }

    public static List<Option<String>> getApplicationsOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        List<Option<String>> options = new ArrayList<>();

        Map<String, ?> body = context
            .http(http -> http.get("/o/openapi"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        for (String key : body.keySet()) {
            if (body.get(key) instanceof List<?> values) {
                String yamlUrl = (String) values.getFirst();

                options.add(option(key, yamlUrl.replace(".yaml", ".json")));
            }
        }

        return options;
    }

    public static List<Option<String>> getEndpointsOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        if (!inputParameters.containsKey(APPLICATION)) {
            return List.of();
        }

        List<Option<String>> options = new ArrayList<>();

        Map<String, ?> body = context
            .http(http -> http.get(inputParameters.getRequiredString(APPLICATION)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        String version = "";

        if (body.get("info") instanceof Map<?, ?> info) {
            version = String.valueOf(info.get("version"));
        }

        if (body.get("paths") instanceof Map<?, ?> paths) {
            for (Map.Entry<?, ?> pathEntry : paths.entrySet()) {
                String endpoint = String.valueOf(pathEntry.getKey());
                Object methodsObj = pathEntry.getValue();

                if (methodsObj instanceof Map<?, ?> methods) {
                    for (Map.Entry<?, ?> methodEntry : methods.entrySet()) {
                        String key = String.valueOf(methodEntry.getKey());

                        String method = key.toUpperCase();

                        String label = method + " " + endpoint;

                        String value = method + " " + endpoint.replaceFirst("/" + version, "");

                        options.add(option(label, value));
                    }
                }
            }
        }

        return options;
    }
}
