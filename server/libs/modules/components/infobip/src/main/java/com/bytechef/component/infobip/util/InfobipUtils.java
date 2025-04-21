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

package com.bytechef.component.infobip.util;

import static com.bytechef.component.infobip.constant.InfobipConstants.CONFIGURATION_KEY;
import static com.bytechef.component.infobip.constant.InfobipConstants.NUMBER_KEY;

import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TypeReference;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class InfobipUtils {

    public static WebhookEnableOutput getWebhookEnableOutput(
        String numberKey, String channel, String webhookUrl, TriggerContext triggerContext) {

        Map<String, Object> body =
            triggerContext.http(http -> http.post("/resource-management/1/inbound-message-configurations"))
                .body(
                    Http.Body.of(
                        "channel", channel,
                        NUMBER_KEY, numberKey,
                        "forwarding", Map.of("type", "HTTP_FORWARD", "url", webhookUrl)))
                .configuration(Http.responseType(Http.ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

        return new WebhookEnableOutput(Map.of(CONFIGURATION_KEY, (String) body.get(CONFIGURATION_KEY)), null);
    }

    public static void unsubscribeWebhook(String configurationKey, TriggerContext triggerContext) {
        triggerContext.http(http -> http.delete(
            "/resource-management/1/inbound-message-configurations/%s".formatted(configurationKey)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute();
    }

}
