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

package com.bytechef.component.calendly.util;

import static com.bytechef.component.calendly.constant.CalendlyConstants.RESOURCE;
import static com.bytechef.component.calendly.constant.CalendlyConstants.SCOPE;
import static com.bytechef.component.calendly.constant.CalendlyConstants.URI;
import static com.bytechef.component.calendly.constant.CalendlyConstants.UUID;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TypeReference;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class CalendlyUtils {

    private CalendlyUtils() {
    }

    public static WebhookEnableOutput subscribeWebhook(
        Context context, String webhookUrl, String scope, String event) {

        Map<String, String> user = getUser(context);

        Map<String, Map<String, String>> body = context.http(http -> http.post("/webhook_subscriptions"))
            .body(
                Http.Body.of(
                    "url", webhookUrl,
                    "organization", user.get("current_organization"),
                    "user", user.get(URI),
                    SCOPE, scope,
                    "events", List.of(event)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        Map<String, String> resourceMap = body.get(RESOURCE);

        return new WebhookEnableOutput(Map.of(UUID, getUuidFromUri(resourceMap.get(URI))), null);
    }

    public static void unsubscribeWebhook(Context context, String uuid) {
        context.http(http -> http.post("/webhook_subscriptions/%s".formatted(uuid)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute();
    }

    private static Map<String, String> getUser(Context context) {
        Map<String, Map<String, String>> body = context.http(http -> http.get("/users/me"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return body.get(RESOURCE);
    }

    private static String getUuidFromUri(String uri) {
        return uri.substring(uri.lastIndexOf("/") + 1);
    }
}
