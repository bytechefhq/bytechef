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

package com.bytechef.component.pipedrive.util;

import static com.bytechef.hermes.component.definition.ComponentDSL.option;

import com.bytechef.hermes.component.definition.Context;
import com.bytechef.hermes.component.definition.Context.Http;
import com.bytechef.hermes.component.definition.OptionsDataSource;
import com.bytechef.hermes.definition.Option;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class PipedriveUtils {

    public static String subscribeWebhook(
        String eventObject, String eventAction, String webhookUrl, Context context) {

        Map<?, ?> result = context
            .http(http -> http.post("/api/v1/webhooks"))
            .body(
                Http.Body.of(
                    Map.of(
                        "event_object", eventObject,
                        "event_action", eventAction,
                        "subscription_url", webhookUrl)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody();

        return (String) result.get("id");
    }

    public static void unsubscribeWebhook(String webhookId, Context context) {
        context
            .http(http -> http.delete("/api/v1/webhooks/%s".formatted(webhookId)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute();
    }

    @SuppressWarnings("unchecked")
    public static OptionsDataSource.ActionOptionsFunction getOptions(String path, String dependsOn) {
        return (inputParameters, connectionParameters, searchText, context) -> {
            Map<String, ?> response = context
                .http(http -> http.get(path))
                .queryParameters(
                    dependsOn == null
                        ? Map.of()
                        : Map.of(dependsOn, List.of(inputParameters.getString(dependsOn, ""))))
                .configuration(Http.responseType(Http.ResponseType.JSON))
                .execute()
                .getBody();

            context.logger(logger -> logger.debug("Response for path='%s': %s".formatted(path, response)));

            List<Option<?>> options = new ArrayList<>();

            for (Map<?, ?> list : (List<Map<?, ?>>) response.get("data")) {
                options.add(option((String) list.get("name"), list.get("id")));
            }

            return new OptionsDataSource.OptionsResponse(options);
        };
    }
}
