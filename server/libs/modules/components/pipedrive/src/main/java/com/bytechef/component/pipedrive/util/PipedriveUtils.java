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

package com.bytechef.component.pipedrive.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.pipedrive.constant.PipedriveConstants.ID;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.exception.ProviderException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 * @author Monika Kušter
 */
public class PipedriveUtils {

    private PipedriveUtils() {
    }

    public static OptionsFunction<String> getOptions(String path, String dependsOn) {
        return (inputParameters, connectionParameters, arrayIndex, searchText, context) -> {
            Map<String, ?> response = context
                .http(http -> http.get(path))
                .queryParameters(
                    dependsOn == null
                        ? Map.of()
                        : Map.of(dependsOn, List.of(inputParameters.getString(dependsOn, ""))))
                .configuration(Http.responseType(Http.ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

            context.log(log -> log.debug("Response for path='%s': %s".formatted(path, response)));

            List<Option<String>> options = new ArrayList<>();

            if (response.get("data") instanceof List<?> list) {
                for (Object o : list) {
                    if (o instanceof Map<?, ?> map) {
                        String id = map.get(ID)
                            .toString();
                        String name = (String) map.get("name");

                        if (path.equals("/deals")) {
                            options.add(option((String) map.get("title"), id));
                        } else if (path.equals("/currencies")) {
                            options.add(option(name, (String) map.get("symbol")));
                        } else {
                            options.add(option(name, id));
                        }
                    }
                }
            }

            return options;
        };
    }

    public static Integer subscribeWebhook(
        String eventObject, String eventAction, String webhookUrl, TriggerContext context) {

        Map<?, ?> result = context
            .http(http -> http.post("/webhooks"))
            .body(
                Http.Body.of(
                    "event_object", eventObject,
                    "event_action", eventAction,
                    "subscription_url", webhookUrl))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (result.get("data") instanceof Map<?, ?> map) {
            return (Integer) map.get(ID);
        }

        throw new ProviderException("Failed to start Pipedrive webhook.");
    }

    public static void unsubscribeWebhook(Integer webhookId, TriggerContext context) {
        context
            .http(http -> http.delete("/webhooks/%s".formatted(webhookId)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute();
    }
}
