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

package com.bytechef.component.sentry.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.sentry.constant.SentryConstants.BASE_URL;
import static com.bytechef.component.sentry.constant.SentryConstants.DEFAULT_BASE_URL;
import static com.bytechef.component.sentry.constant.SentryConstants.NAME;
import static com.bytechef.component.sentry.constant.SentryConstants.ORGANIZATION;
import static com.bytechef.component.sentry.constant.SentryConstants.SLUG;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class SentryUtils {

    private static final String PROJECTS_PER_PAGE = "100";

    private SentryUtils() {
    }

    public static String getBaseUri(Parameters connectionParameters) {
        String baseUrl = connectionParameters.getString(BASE_URL, DEFAULT_BASE_URL);

        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = DEFAULT_BASE_URL;
        }

        baseUrl = baseUrl.strip();

        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        return baseUrl + "/api/0";
    }

    public static String getIssuePath(Parameters connectionParameters, String issueId) {
        return "/organizations/" + connectionParameters.getRequiredString(ORGANIZATION) + "/issues/" + issueId + "/";
    }

    public static List<Option<String>> getProjectOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) {

        List<Map<String, Object>> projects = context
            .http(http -> http.get(
                "/organizations/" + connectionParameters.getRequiredString(ORGANIZATION) + "/projects/"))
            .queryParameters("per_page", PROJECTS_PER_PAGE)
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        if (projects == null) {
            return options;
        }

        for (Map<String, Object> project : projects) {
            options.add(option((String) project.get(NAME), (String) project.get(SLUG)));
        }

        return options;
    }
}
