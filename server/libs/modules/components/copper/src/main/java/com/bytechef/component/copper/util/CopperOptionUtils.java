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

package com.bytechef.component.copper.util;

import static com.bytechef.component.copper.constant.CopperConstants.BASE_URL;
import static com.bytechef.component.copper.constant.CopperConstants.COMPANY;
import static com.bytechef.component.copper.constant.CopperConstants.ID;
import static com.bytechef.component.copper.constant.CopperConstants.LEAD;
import static com.bytechef.component.copper.constant.CopperConstants.NAME;
import static com.bytechef.component.copper.constant.CopperConstants.PERSON;
import static com.bytechef.component.copper.constant.CopperConstants.TYPE;
import static com.bytechef.component.definition.ComponentDSL.option;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.exception.ProviderException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Domiter
 */
public class CopperOptionUtils {

    private CopperOptionUtils() {
    }

    private static List<Option<String>> createOptions(List<Map<String, Object>> value) {
        List<Option<String>> options = new ArrayList<>();

        for (Map<String, Object> map : value) {
            options.add(option(String.valueOf(map.get(NAME)), String.valueOf(map.get(ID))));
        }

        return options;
    }

    @SuppressWarnings("unchecked")
    public static List<Option<String>> getActivityTypeOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) {

        Map<String, ArrayList<Map<String, Object>>> body = null;

        Http.Response response = context.http(http -> http.get(BASE_URL + "/activity_types"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute();

        try {
            body = response.getBody(new TypeReference<>() {});
        } catch (Exception e) {
            Map<String, Object> badRequestBody = (Map<String, Object>) response.getBody();

            String message = (String) badRequestBody.get("message");
            Integer code = (Integer) badRequestBody.get("status");

            throw new ProviderException(code, message);
        }

        List<Option<String>> options = new ArrayList<>();

        body
            .getOrDefault("user", new ArrayList<>())
            .forEach(map -> options.add(option(String.valueOf(map.get("name")), String.valueOf(map.get("id")))));

        return options;
    }

    public static List<Option<String>> getCompanyIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) {

        List<Map<String, Object>> body = context.http(http -> http.post(BASE_URL + "/companies/search"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return createOptions(body);
    }

    public static List<Option<String>> getContactTypesOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) {

        List<Map<String, Object>> body = context.http(http -> http.get(BASE_URL + "/contact_types"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return createOptions(body);
    }

    @SuppressWarnings("unchecked")
    public static List<Option<String>> getParentOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) {

        String parentType = inputParameters.getRequiredString(TYPE);

        Http.Executor executor = switch (parentType) {
            case LEAD -> context.http(http -> http.post(BASE_URL + "/leads/search"));
            case PERSON -> context.http(http -> http.post(BASE_URL + "/people/search"));
            case COMPANY -> context.http(http -> http.post(BASE_URL + "/companies/search"));
            default -> context.http(http -> http.post(BASE_URL + "/opportunities/search"));
        };

        List<Map<String, Object>> body;
        Http.Response response = executor.configuration(Http.responseType(Http.ResponseType.JSON))
            .execute();

        try {
            body = response.getBody(new TypeReference<>() {});
        } catch (Exception e) {
            Map<String, Object> badRequestBody = (Map<String, Object>) response.getBody();

            String message = (String) badRequestBody.get("message");
            Integer code = (Integer) badRequestBody.get("status");

            throw new ProviderException(code, message);
        }

        return createOptions(body);
    }

    public static List<Option<String>> getTagsOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) {

        List<Map<String, Object>> body = context.http(http -> http.get(BASE_URL + "/tags"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        for (Map<String, Object> linkedHashMap : body) {
            String name = String.valueOf(linkedHashMap.get(NAME));

            options.add(option(name, name));
        }

        return options;
    }

    public static List<Option<String>> getUserOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) {

        List<Map<String, Object>> body = context.http(http -> http.post(BASE_URL + "/users/search"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return createOptions(body);
    }
}
