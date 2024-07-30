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

package com.bytechef.component.hubspot.util;

import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.hubspot.constant.HubspotConstants.ID;
import static com.bytechef.component.hubspot.constant.HubspotConstants.LABEL;
import static com.bytechef.component.hubspot.constant.HubspotConstants.RESULTS;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class HubspotUtils {

    private HubspotUtils() {
    }

    public static List<Option<String>> getContactsOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) {

        Map<String, Object> response = context
            .http(http -> http.get("/crm/v3/objects/contacts"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        if (response.get(RESULTS) instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?, ?> map && map.get("properties") instanceof Map<?, ?> propertiesMap) {
                    String firstname = (String) propertiesMap.get("firstname");
                    String lastname = (String) propertiesMap.get("lastname");

                    options.add(option(firstname + " " + lastname, (String) map.get(ID)));

                }
            }
        }

        return options;
    }

    public static List<Option<String>> getDealStageOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) {

        Map<String, Object> body =
            context
                .http(http -> http.get("/crm/v3/pipelines/deals/" + inputParameters.getString("pipeline") + "/stages"))
                .configuration(Http.responseType(Http.ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

        return getOptions(body, LABEL);
    }

    public static List<Option<String>> getOwnerOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) {

        Map<String, Object> body =
            context.http(http -> http.get("/crm/v3/owners"))
                .configuration(Http.responseType(Http.ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

        return getOptions(body, "email");

    }

    public static List<Option<String>> getPipelineDealOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) {

        Map<String, Object> body =
            context.http(http -> http.get("/crm/v3/pipelines/deals"))
                .configuration(Http.responseType(Http.ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

        return getOptions(body, LABEL);
    }

    private static List<Option<String>> getOptions(Map<String, Object> body, String label) {
        List<Option<String>> options = new ArrayList<>();

        if (body.get(RESULTS) instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    options.add(option(label, (String) map.get(ID)));
                }
            }
        }

        return options;
    }
}
