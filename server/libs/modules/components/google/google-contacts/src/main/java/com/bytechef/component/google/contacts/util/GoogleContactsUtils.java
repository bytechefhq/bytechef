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
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.EMAIL_ADDRESSES;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.E_TAG;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.FAMILY_NAME;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.GIVEN_NAME;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.MIDDLE_NAME;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.NAME;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.NAMES;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.ORGANIZATIONS;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.PAGE_SIZE;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.PERSON_FIELDS;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.PHONE_NUMBERS;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.RESOURCE_NAME;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.TITLE;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.VALUE;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.exception.ProviderException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 * @author Nikolina Spehar
 */
public class GoogleContactsUtils {

    private GoogleContactsUtils() {
    }

    private static List<Map<String, Object>> extractConnections(Map<String, Object> response) {
        return (List<Map<String, Object>>) response.getOrDefault("connections", List.of());
    }

    private static List<Map<String, Object>> fetchAllConnections(Context context) {
        List<Map<String, Object>> allConnections = new ArrayList<>();
        String pageToken = null;

        do {
            Map<String, Object> response = fetchConnectionsPage(context, pageToken);

            allConnections.addAll(extractConnections(response));
            pageToken = (String) response.get("nextPageToken");

        } while (pageToken != null);

        return allConnections;
    }

    private static Map<String, Object> fetchConnectionsPage(Context context, String pageToken) {
        var request = context.http(http -> http.get("/people/me/connections"))
            .configuration(responseType(Http.ResponseType.JSON))
            .queryParameters(
                PAGE_SIZE, DEFAULT_PAGE_SIZE,
                PERSON_FIELDS, "names");

        if (pageToken != null) {
            request = request.queryParameters(
                "pageToken", pageToken,
                PAGE_SIZE, DEFAULT_PAGE_SIZE,
                PERSON_FIELDS, "names");
        }

        return request.execute()
            .getBody(new TypeReference<>() {});
    }

    private static Map<String, String> getContactEmailAddressesOrPhoneNumbers(Object object) {
        Map<String, String> map = new HashMap<>();

        if (object instanceof List<?> list &&
            list.getFirst() instanceof Map<?, ?> firstElement) {

            map.put(VALUE, getIfMapContains(firstElement, VALUE));
        }

        return map;
    }

    private static Map<String, String> getContactNames(Object names) {
        Map<String, String> namesMap = new HashMap<>();

        if (names instanceof List<?> namesList &&
            namesList.getFirst() instanceof Map<?, ?> namesObject) {

            namesMap.put(GIVEN_NAME, getIfMapContains(namesObject, GIVEN_NAME));
            namesMap.put(MIDDLE_NAME, getIfMapContains(namesObject, MIDDLE_NAME));
            namesMap.put(FAMILY_NAME, getIfMapContains(namesObject, FAMILY_NAME));
        }

        return namesMap;
    }

    private static Map<String, String> getContactOrganizations(Object organizations) {
        Map<String, String> organizationsMap = new HashMap<>();

        if (organizations instanceof List<?> organizationsList &&
            organizationsList.getFirst() instanceof Map<?, ?> organizationObject) {

            organizationsMap.put(TITLE, getIfMapContains(organizationObject, TITLE));
            organizationsMap.put(NAME, getIfMapContains(organizationObject, NAME));
        }

        return organizationsMap;
    }

    public static List<Option<String>> getContactResourceNameOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        List<Option<String>> contactResourceNameOptions = new ArrayList<>();

        List<Map<String, Object>> connections = fetchAllConnections(context);

        for (Map<String, Object> connection : connections) {
            String displayName = getDisplayNameFromNames(connection.get(NAMES));

            contactResourceNameOptions.add(option(displayName, (String) connection.get(RESOURCE_NAME)));
        }

        return contactResourceNameOptions;
    }

    public static Map<String, Object> getContactToUpdate(String resourceName, Context context) {
        Map<String, Object> response = context.http(http -> http.get("/%s".formatted(resourceName)))
            .configuration(responseType(Http.ResponseType.JSON))
            .queryParameter(PERSON_FIELDS, "emailAddresses,names,phoneNumbers,organizations")
            .execute()
            .getBody(new TypeReference<>() {});

        Map<String, String> names = getContactNames(response.get(NAMES));
        Map<String, String> organizations = getContactOrganizations(response.get(ORGANIZATIONS));
        Map<String, String> emailAddresses = getContactEmailAddressesOrPhoneNumbers(response.get(EMAIL_ADDRESSES));
        Map<String, String> phoneNumbers = getContactEmailAddressesOrPhoneNumbers(response.get(PHONE_NUMBERS));

        return Map.of(
            E_TAG, response.get(E_TAG),
            NAMES, names,
            ORGANIZATIONS, organizations,
            EMAIL_ADDRESSES, emailAddresses,
            PHONE_NUMBERS, phoneNumbers);
    }

    private static String getDisplayNameFromNames(Object names) {
        if (names instanceof List<?> namesList &&
            namesList.getFirst() instanceof Map<?, ?> namesMap) {

            return (String) namesMap.get("displayName");
        }

        throw new ProviderException("Unable to find contact display name.");
    }

    private static String getIfMapContains(Map<?, ?> map, String key) {
        return map.containsKey(key) ? (String) map.get(key) : "";
    }
}
