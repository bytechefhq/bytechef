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

package com.bytechef.component.zoho.commons;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.zoho.commons.ZohoConstants.CONTACT_NAME;
import static com.bytechef.component.zoho.commons.ZohoConstants.CONTACT_TYPE;

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
public class ZohoUtils {

    private ZohoUtils() {
    }

    public static List<Option<String>> getCurrencyOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        List<Option<String>> options = new ArrayList<>();

        Map<String, ?> body = context
            .http(http -> http.get("/settings/currencies"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (body.get("currencies") instanceof List<?> currencies) {
            for (Object currency : currencies) {
                if (currency instanceof Map<?, ?> map) {
                    options.add(option((String) map.get("currency_name"), (String) map.get("currency_id")));
                }
            }
        }

        return options;
    }

    public static List<Option<String>> getCustomersOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        List<Option<String>> options = new ArrayList<>();

        Map<String, ?> body = context
            .http(http -> http.get("/contacts"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (body.get("contacts") instanceof List<?> contacts) {
            for (Object contact : contacts) {
                if (contact instanceof Map<?, ?> map && map.get(CONTACT_TYPE)
                    .equals("customer")) {

                    options.add(option((String) map.get(CONTACT_NAME), (String) map.get("contact_id")));
                }
            }
        }

        return options;
    }

    public static List<Option<String>> getItemsOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        List<Option<String>> options = new ArrayList<>();

        Map<String, ?> body = context
            .http(http -> http.get("/items"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (body.get("items") instanceof List<?> items) {
            for (Object item : items) {
                if (item instanceof Map<?, ?> map) {
                    options.add(option((String) map.get("name"), (String) map.get("item_id")));
                }
            }
        }

        return options;
    }
}
