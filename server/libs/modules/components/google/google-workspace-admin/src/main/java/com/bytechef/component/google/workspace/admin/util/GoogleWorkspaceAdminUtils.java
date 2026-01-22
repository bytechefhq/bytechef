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

package com.bytechef.component.google.workspace.admin.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.google.workspace.admin.constant.GoogleWorkspaceAdminConstants.EMAIL;
import static com.bytechef.component.google.workspace.admin.constant.GoogleWorkspaceAdminConstants.MAX_RESULTS;
import static com.bytechef.component.google.workspace.admin.constant.GoogleWorkspaceAdminConstants.NEXT_PAGE_TOKEN;
import static com.bytechef.component.google.workspace.admin.constant.GoogleWorkspaceAdminConstants.PAGE_TOKEN;

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
 */
public class GoogleWorkspaceAdminUtils {

    public static List<Option<String>> getRoleIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        List<Option<String>> options = new ArrayList<>();
        String nextPageToken = null;

        do {
            Map<String, Object> response = context
                .http(http -> http.get("https://admin.googleapis.com/admin/directory/v1/customer/my_customer/roles"))
                .queryParameters(PAGE_TOKEN, nextPageToken, MAX_RESULTS, 100)
                .configuration(responseType(ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

            if (response.get("items") instanceof List<?> items) {
                for (Object item : items) {
                    if (item instanceof Map<?, ?> map) {
                        options.add(option((String) map.get("roleName"), (String) map.get("roleId")));
                    }
                }
            }

            nextPageToken = (String) response.getOrDefault(NEXT_PAGE_TOKEN, null);
        } while (nextPageToken != null);

        return options;
    }

    public static List<Option<String>> getUserIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        List<Option<String>> options = new ArrayList<>();
        String nextPageToken = null;

        do {
            Map<String, Object> response = context
                .http(http -> http.get("https://admin.googleapis.com/admin/directory/v1/users"))
                .queryParameters("customer", "my_customer", PAGE_TOKEN, nextPageToken, MAX_RESULTS, 100)
                .configuration(responseType(ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

            if (response.get("users") instanceof List<?> users) {
                for (Object user : users) {
                    if (user instanceof Map<?, ?> map) {
                        options.add(option((String) map.get(EMAIL), (String) map.get("id")));
                    }
                }
            }

            nextPageToken = (String) response.getOrDefault(NEXT_PAGE_TOKEN, null);
        } while (nextPageToken != null);

        return options;
    }

    private GoogleWorkspaceAdminUtils() {
    }

}
