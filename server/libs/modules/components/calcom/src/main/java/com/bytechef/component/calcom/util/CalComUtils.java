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

package com.bytechef.component.calcom.util;

import static com.bytechef.component.calcom.constant.CalComConstants.DATA;
import static com.bytechef.component.calcom.constant.CalComConstants.ID;
import static com.bytechef.component.calcom.constant.CalComConstants.PAYLOAD;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.exception.ProviderException;
import java.util.List;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class CalComUtils {

    public static String subscribeWebhook(String eventType, Context context, String webhookUrl) {
        Map<String, Object> body = context.http(http -> http.post("/webhooks"))
            .body(
                Body.of(
                    "active", true,
                    "subscriberUrl", webhookUrl,
                    "triggers", List.of(eventType)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (body.get(DATA) instanceof Map<?, ?> dataMap) {
            return (String) dataMap.get(ID);
        }

        throw new ProviderException("Failed to subscribe to webhook");
    }

    public static void unsubscribeWebhook(Context context, String webhookId) {
        context.http(http -> http.delete("/webhooks/%s".formatted(webhookId)))
            .execute();
    }

    public static Object getContent(WebhookBody body) {
        Map<String, Object> content = body.getContent(new TypeReference<>() {});

        return content.get(PAYLOAD);
    }
}
