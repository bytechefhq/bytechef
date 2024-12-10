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

package com.bytechef.component.webflow.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.webflow.constant.WebflowConstants.COLLECTION_ID;
import static com.bytechef.component.webflow.constant.WebflowConstants.DISPLAY_NAME;
import static com.bytechef.component.webflow.constant.WebflowConstants.ID;
import static com.bytechef.component.webflow.constant.WebflowConstants.ORDER_ID;
import static com.bytechef.component.webflow.constant.WebflowConstants.SITE_ID;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class WebflowUtils {

    private WebflowUtils() {
    }

    public static List<Option<String>> getCollectionItemOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) {

        Map<String, Object> body = context
            .http(http -> http.get("/collections/" + inputParameters.getRequiredString(COLLECTION_ID) + "/items"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        if (body.get("items") instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> itemMap && (itemMap.get("fieldData") instanceof Map<?, ?> fieldData)) {
                    options.add(option((String) fieldData.get("name"), (String) itemMap.get(ID)));
                }
            }
        }

        return options;
    }

    public static List<Option<String>> getCollectionOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) {

        Map<String, List<Map<String, Object>>> body = context
            .http(http -> http.get("/sites/" + inputParameters.getRequiredString(SITE_ID) + "/collections"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        for (Map<String, Object> map : body.get("collections")) {
            options.add(option((String) map.get(DISPLAY_NAME), (String) map.get(ID)));
        }

        return options;
    }

    public static List<Option<String>> getOrderOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) {

        Map<String, List<Map<String, Object>>> body = context
            .http(http -> http.get("/sites/" + inputParameters.getRequiredString(SITE_ID) + "/orders"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        for (Map<String, Object> map : body.get("orders")) {
            options.add(option((String) map.get(ORDER_ID), (String) map.get(ORDER_ID)));
        }

        return options;
    }

    public static List<Option<String>> getSiteOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) {

        Map<String, List<Map<String, Object>>> body = context
            .http(http -> http.get("/sites"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        for (Map<String, Object> map : body.get("sites")) {
            options.add(option((String) map.get(DISPLAY_NAME), (String) map.get(ID)));
        }

        return options;
    }
}
