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

package com.bytechef.component.google.contacts.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.DEFAULT_PAGE_SIZE;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.NAMES;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.PAGE_SIZE;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.PERSON_FIELDS;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.RESOURCE_NAME;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.exception.ProviderException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 * @author Nikolina Spehar
 */
public class GoogleContactsUtils {

    private GoogleContactsUtils() {
    }

    public static List<Option<String>> getContactResourceNameOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        List<Option<String>> contactResourceNameOptions = new ArrayList<>();

        List<Map<?, ?>> connections = fetchAllConnections(context);

        for (Map<?, ?> connection : connections) {
            String displayName = getDisplayNameFromNames(connection.get(NAMES));

            contactResourceNameOptions.add(option(displayName, (String) connection.get(RESOURCE_NAME)));
        }

        return contactResourceNameOptions;
    }

    private static List<Map<?, ?>> fetchAllConnections(Context context) {
        List<Map<?, ?>> allConnections = new ArrayList<>();
        String pageToken = null;

        do {
            Map<String, Object> response = fetchConnectionsPage(context, pageToken);

            Object extractedConnections = extractConnections(response);

            if (extractedConnections instanceof List<?> connections) {
                for (Object connection : connections) {
                    if (connection instanceof Map<?, ?> connectionMap) {
                        allConnections.add(connectionMap);
                    }
                }
            }

            pageToken = (String) response.get("nextPageToken");

        } while (pageToken != null);

        return allConnections;
    }

    private static Map<String, Object> fetchConnectionsPage(Context context, String pageToken) {
        return context.http(http -> http.get("/people/me/connections"))
            .configuration(responseType(Http.ResponseType.JSON))
            .queryParameters(
                PAGE_SIZE, DEFAULT_PAGE_SIZE,
                PERSON_FIELDS, "names",
                "pageToken", pageToken)
            .execute()
            .getBody(new TypeReference<>() {});
    }

    private static Object extractConnections(Map<String, Object> response) {
        return response.get("connections");
    }

    private static String getDisplayNameFromNames(Object names) {
        if (names instanceof List<?> namesList && namesList.getFirst() instanceof Map<?, ?> namesMap) {

            return (String) namesMap.get("displayName");
        }

        throw new ProviderException("Unable to find contact display name.");
    }
}
