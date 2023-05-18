
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.component.pipedrive.util;

import com.bytechef.hermes.component.util.HttpClientUtils;
import com.bytechef.hermes.component.util.HttpClientUtils.Body;
import com.bytechef.hermes.component.util.HttpClientUtils.ResponseFormat;

import java.util.Map;

import static com.bytechef.hermes.component.util.HttpClientUtils.responseFormat;

/**
 * @author Ivica Cardic
 */
public class PipedriveUtils {

    public static String subscribeWebhook(String serverUrl, String eventObject, String eventAction, String webhookUrl) {
        Map<?, ?> result = (Map<?, ?>) HttpClientUtils
            .post("%s/api/v1/webhooks".formatted(serverUrl))
            .body(
                Body.of(
                    Map.of(
                        "event_object", eventObject,
                        "event_action", eventAction,
                        "subscription_url", webhookUrl)))
            .configuration(responseFormat(ResponseFormat.JSON))
            .execute()
            .body();

        return (String) result.get("id");
    }

    public static void unsubscribeWebhook(String serverUrl, String webhookId) {
        HttpClientUtils
            .delete("%s/api/v1/webhooks/%s".formatted(serverUrl, webhookId))
            .configuration(responseFormat(ResponseFormat.JSON))
            .execute();
    }
}
