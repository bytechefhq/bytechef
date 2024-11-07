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

package com.bytechef.component.stripe.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.stripe.constant.StripeConstants.ID;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.exception.ProviderException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class StripeUtils {

    public static List<Option<String>> getCustomerOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext actionContext) {

        Map<String, Object> body = actionContext.http(
            http -> http.get("/customers"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        if (body.get("data") instanceof List<?> list) {
            for (Object object : list) {
                if (object instanceof Map<?, ?> map) {
                    options.add(option((String) map.get("name"), (String) map.get(ID)));
                }
            }
        }
        return options;

    }

    public static Object getNewObject(WebhookBody webhookBody) {
        Map<String, Object> content = webhookBody.getContent(new TypeReference<>() {});
        Object data = content.get("data");

        if (data instanceof Map<?, ?> map) {
            return map.get("object");
        }

        throw new ProviderException("Stripe webhook request is not valid.");
    }

    public static String subscribeWebhook(String webhookUrl, TriggerContext context, String event) {
        Map<String, Object> body = context
            .http(http -> http.post("/webhook_endpoints"))
            .body(
                Http.Body.of(
                    Map.of("enabled_events", List.of(event), "url", webhookUrl),
                    Http.BodyContentType.FORM_URL_ENCODED))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return (String) body.get(ID);
    }

    public static void unsubscribeWebhook(String webhookId, TriggerContext context) {
        context.http(http -> http.delete("/webhook_endpoints/" + webhookId))
            .configuration(responseType(ResponseType.JSON))
            .execute();
    }
}
