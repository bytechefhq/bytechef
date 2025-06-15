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

package com.bytechef.component.woocommerce.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.ID;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Marija Horvat
 */
public class WoocommerceUtils {

    private WoocommerceUtils() {
    }

    public static WebhookEnableOutput createWebhook(String webhookUrl, TriggerContext context, String topic) {
        Map<String, ?> body = context.http(http -> http.post("/webhooks"))
            .body(
                Http.Body.of(
                    "delivery_url", webhookUrl,
                    "name", "New Webhook",
                    "topic", topic))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return new WebhookEnableOutput(Map.of(ID, body.get(ID)), null);
    }

    public static void deleteWebhook(Integer webhookId, Context context) {
        context.http(http -> http.delete("/webhooks/" + webhookId))
            .execute();
    }

    public static List<Option<String>> getCategoryIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        List<Map<String, Object>> response = context
            .http(http -> http.get("/products/categories"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> categoriesId = new ArrayList<>();

        for (Object item : response) {
            if (item instanceof Map<?, ?> map) {
                categoriesId.add(option((String) map.get("name"), (String) map.get(ID)));
            }
        }

        return categoriesId;
    }

    public static List<Option<String>> getCustomerIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        List<Map<String, Object>> response = context
            .http(http -> http.get("/customers"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        for (Object item : response) {
            if (item instanceof Map<?, ?> map) {
                options.add(option((String) map.get("username"), (String) map.get(ID)));
            }
        }

        return options;
    }

    public static List<Option<String>> getPaymentIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        List<Map<String, Object>> response = context
            .http(http -> http.get("/payment_gateways"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        for (Object item : response) {
            if (item instanceof Map<?, ?> map) {
                options.add(option((String) map.get("title"), (String) map.get(ID)));
            }
        }

        return options;
    }

    public static List<Option<String>> getProductIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        List<Map<String, Object>> response = context
            .http(http -> http.get("/products"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        for (Object item : response) {
            if (item instanceof Map<?, ?> map) {
                options.add(option((String) map.get("name"), (String) map.get(ID)));
            }
        }

        return options;
    }

    public static List<Option<String>> getTagIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        List<Map<String, Object>> response = context
            .http(http -> http.get("/products/tags"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        for (Object item : response) {
            if (item instanceof Map<?, ?> map) {
                options.add(option((String) map.get("name"), (String) map.get(ID)));
            }
        }

        return options;
    }
}
