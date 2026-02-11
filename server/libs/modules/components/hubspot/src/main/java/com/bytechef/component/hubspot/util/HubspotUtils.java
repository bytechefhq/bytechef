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

package com.bytechef.component.hubspot.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.hubspot.constant.HubspotConstants.EVENT_TYPE;
import static com.bytechef.component.hubspot.constant.HubspotConstants.HAPIKEY;
import static com.bytechef.component.hubspot.constant.HubspotConstants.ID;
import static com.bytechef.component.hubspot.constant.HubspotConstants.LABEL;
import static com.bytechef.component.hubspot.constant.HubspotConstants.RESULTS;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class HubspotUtils extends AbstractHubspotUtils {

    private HubspotUtils() {
    }

    public static Map<String, Object> extractFirstContentMap(TriggerDefinition.WebhookBody body) {
        List<Map<String, Object>> content = body.getContent(new TypeReference<>() {});

        return content.getFirst();
    }

    public static List<Option<String>> getContactIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

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

    public static List<Option<String>> getDealstageOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        Map<String, Object> body = context
            .http(http -> http.get("/crm/v3/pipelines/deals/"
                + inputParameters.getRequiredFromPath("properties.pipeline", String.class) + "/stages"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return getOptions(body, LABEL);
    }

    public static List<Option<String>> getHubspotOwnerIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        Map<String, Object> body =
            context.http(http -> http.get("/crm/v3/owners"))
                .configuration(Http.responseType(Http.ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

        return getOptions(body, "email");

    }

    public static List<Option<String>> getPipelineOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        Map<String, Object> body =
            context.http(http -> http.get("/crm/v3/pipelines/deals"))
                .configuration(Http.responseType(Http.ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

        return getOptions(body, LABEL);
    }

    public static List<Option<String>> getTicketIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) {

        Map<String, Object> body =
            context.http(http -> http.get("/crm/v3/objects/tickets"))
                .configuration(Http.responseType(Http.ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        if (body.get(RESULTS) instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?, ?> map && map.get("properties") instanceof Map<?, ?> propertiesMap) {

                    options.add(option((String) propertiesMap.get("subject"), (String) map.get(ID)));
                }
            }
        }

        return options;
    }

    public static String subscribeWebhook(
        String eventType, String appId, String hapikey, String webhookUrl, TriggerContext triggerContext) {

        triggerContext
            .http(http -> http.put("/webhooks/v3/%s/settings".formatted(appId)))
            .queryParameter(HAPIKEY, hapikey)
            .body(Context.Http.Body.of(
                "throttling", Map.of(
                    "period", "SECONDLY",
                    "maxConcurrentRequests", 10),
                "targetUrl", webhookUrl))
            .configuration(
                Context.Http.responseType(Context.Http.ResponseType.JSON)
                    .disableAuthorization(true))
            .execute();

        Map<String, Object> body = triggerContext
            .http(http -> http.post("/webhooks/v3/%s/subscriptions".formatted(appId)))
            .queryParameter(HAPIKEY, hapikey)
            .body(Context.Http.Body.of(
                EVENT_TYPE, eventType,
                "active", true))
            .configuration(
                Context.Http.responseType(Context.Http.ResponseType.JSON)
                    .disableAuthorization(true))
            .execute()
            .getBody(new TypeReference<>() {});

        return (String) body.get(ID);
    }

    public static void unsubscribeWebhook(String appId, String subscriptionId, String hapikey, TriggerContext context) {
        context
            .http(http -> http.delete("/webhooks/v3/%s/subscriptions/%s".formatted(appId, subscriptionId)))
            .queryParameter(HAPIKEY, hapikey)
            .configuration(
                Context.Http.responseType(Context.Http.ResponseType.JSON)
                    .disableAuthorization(true))
            .execute();
    }

    private static List<Option<String>> getOptions(Map<String, Object> body, String label) {
        List<Option<String>> options = new ArrayList<>();

        if (body.get(RESULTS) instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    options.add(option((String) map.get(label), (String) map.get(ID)));
                }
            }
        }

        return options;
    }
}
