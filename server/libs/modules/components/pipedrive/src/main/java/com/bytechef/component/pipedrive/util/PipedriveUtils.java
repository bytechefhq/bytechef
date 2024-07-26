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

import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.pipedrive.constant.PipedriveConstants.ID;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
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

    public static ActionOptionsFunction<String> getOptions(String path, String dependsOn) {
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

            context.logger(logger -> logger.debug("Response for path='%s': %s".formatted(path, response)));

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
            .getBody(new Context.TypeReference<>() {});

        return (String) result.get("id");
    }

    public static void unsubscribeWebhook(String webhookId, Context context) {
        context
            .http(http -> http.delete("/api/v1/webhooks/%s".formatted(webhookId)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute();
    }
}
