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

package com.bytechef.component.shopify.util;

import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.shopify.constant.ShopifyConstants.ID;
import static com.bytechef.component.shopify.constant.ShopifyConstants.PRODUCT_ID;
import static com.bytechef.component.shopify.constant.ShopifyConstants.SHOP_NAME;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Domiter
 */
public class ShopifyUtils {

    private ShopifyUtils() {
    }

    public static String getBaseUrl(Parameters connectionParameters) {
        return "https://" + connectionParameters.getRequiredString(SHOP_NAME) + ".myshopify.com/admin/api/2024-04";
    }

    public static List<Option<Long>> getOrderIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) {

        Map<String, List<Map<String, Object>>> body = context
            .http(http -> http.get(getBaseUrl(connectionParameters) + "/orders.json?status=any"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<Long>> options = new ArrayList<>();

        for (Map<String, Object> map : body.get("orders")) {
            options.add(option((String) map.get("name"), ((Long) map.get(ID)).longValue()));
        }

        return options;
    }

    public static List<Option<Long>> getProductIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) {

        Map<String, List<Map<String, Object>>> body = context
            .http(http -> http.get(getBaseUrl(connectionParameters) + "/products.json"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<Long>> options = new ArrayList<>();

        for (Map<String, Object> map : body.get("products")) {
            options.add(option((String) map.get("title"), ((Long) map.get(ID)).longValue()));
        }

        return options;
    }

    public static List<Option<Long>> getVariantIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) {

        Map<String, List<Map<String, Object>>> body = context.http(
            http -> http.get(
                getBaseUrl(connectionParameters) + "/products/" +
                    inputParameters.getRequiredFromPath(dependencyPaths.get(PRODUCT_ID)) + "/variants.json"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<Long>> options = new ArrayList<>();

        for (Map<String, Object> map : body.get("variants")) {
            options.add(option((String) map.get("title"), ((Long) map.get(ID)).longValue()));
        }

        return options;
    }

    public static Long subscribeWebhook(
        Parameters connectionParameters, String webhookUrl, Context context, String topic) {

        Map<String, ?> body = context.http(http -> http.post(getBaseUrl(connectionParameters) + "/webhooks.json"))
            .body(Http.Body.of(
                "webhook", Map.of(
                    "topic", topic,
                    "address", webhookUrl,
                    "format", "json")))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (body.get("webhook") instanceof Map<?, ?> map) {
            return (Long) map.get(ID);
        }

        return null;
    }

    public static void unsubscribeWebhook(
        Parameters connectionParameters, Parameters outputParameters, Context context) {

        context.http(http -> http
            .delete(getBaseUrl(connectionParameters) + "/webhooks/" + outputParameters.getString(ID) + ".json"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute();
    }

}
