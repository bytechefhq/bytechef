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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        if (body == null || body.isEmpty()) {
            return List.of();
        }

        body.entrySet()
            .forEach(entry -> {
                if (!(entry.getValue() instanceof List<?> applications)) {
                    return;
                }
                if (applications.isEmpty()) {
                    return;
                }

                for (Object application : applications) {
                    if (application instanceof String path) {
                        String applicationOpenapiRelativePath =
                            path.substring(path.indexOf((String) entry.getKey()) + 1);

                        options.add(option(getHeadlessApplicationQualifiedName(applicationOpenapiRelativePath),
                            applicationOpenapiRelativePath.replace("/openapi.yaml", "")));
                    }
                }
            });

        return options;
    }

    private static String getHeadlessApplicationQualifiedName(String path) {
        Matcher matcher = versionPattern.matcher(path);

        if (!matcher.find()) {
            return path;
        }

        int appNameEndIdx = path.indexOf("/");

        return path.substring(0, appNameEndIdx) + " " + matcher.group(1);
    }

    private static final Pattern versionPattern = Pattern.compile("/(v\\d.\\d+)/");

    public static List<Option<String>> getEndpointsOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        if (!inputParameters.containsKey(APPLICATION)) {
            return List.of();
        }

        List<Option<String>> options = new ArrayList<>();

        Map<String, ?> body = context
            .http(http -> http.get("/o/" + inputParameters.getRequiredString(APPLICATION) + "/openapi.json"))
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
