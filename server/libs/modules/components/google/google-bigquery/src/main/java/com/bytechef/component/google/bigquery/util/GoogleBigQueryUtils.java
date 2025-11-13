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

package com.bytechef.component.google.bigquery.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.google.bigquery.constant.GoogleBigQueryConstants.ID;
import static com.bytechef.component.google.bigquery.constant.GoogleBigQueryConstants.NEXT_PAGE_TOKEN;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class GoogleBigQueryUtils {

    public static List<Option<String>> getProjectIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) {

        List<Option<String>> projectIdOptions = new ArrayList<>();

        Map<String, Object> response;
        String nextPageToken = "";

        do {
            response = context.http(
                http -> http.get("https://bigquery.googleapis.com/bigquery/v2/projects"))
                .configuration(responseType(Http.ResponseType.JSON))
                .queryParameter(NEXT_PAGE_TOKEN, nextPageToken)
                .execute()
                .getBody(new TypeReference<>() {});

            if (response.get("projects") instanceof List<?> projects) {
                for (Object project : projects) {
                    if (project instanceof Map<?, ?> projectMap) {
                        projectIdOptions.add(
                            option((String) projectMap.get("friendlyName"), (String) projectMap.get(ID)));
                    }
                }
            }

            nextPageToken = (String) response.get(NEXT_PAGE_TOKEN);

        } while (nextPageToken != null);

        return projectIdOptions;

    }
}
