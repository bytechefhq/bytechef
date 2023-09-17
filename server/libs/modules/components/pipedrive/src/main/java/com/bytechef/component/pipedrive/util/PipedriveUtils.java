
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

import com.bytechef.hermes.component.definition.ComponentOptionsFunction;

import com.bytechef.hermes.component.definition.Context.Http;
import com.bytechef.hermes.component.definition.TriggerDefinition.TriggerContext;
import com.bytechef.hermes.definition.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.bytechef.hermes.definition.DefinitionDSL.option;

/**
 * @author Ivica Cardic
 */
public class PipedriveUtils {

    private static final Logger logger = LoggerFactory.getLogger(PipedriveUtils.class);

    public static String subscribeWebhook(
        String eventObject, String eventAction, String webhookUrl, TriggerContext context) {

        Map<?, ?> result = (Map<?, ?>) context
            .http(http -> http.post("/api/v1/webhooks"))
            .body(
                Http.Body.of(
                    Map.of(
                        "event_object", eventObject,
                        "event_action", eventAction,
                        "subscription_url", webhookUrl)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .body();

        return (String) result.get("id");
    }

    public static void unsubscribeWebhook(String webhookId, TriggerContext context) {
        context
            .http(http -> http.delete("/api/v1/webhooks/%s".formatted(webhookId)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute();
    }

    @SuppressWarnings("unchecked")
    public static ComponentOptionsFunction getOptions(String path, String dependsOn) {
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

            if (logger.isDebugEnabled()) {
                logger.debug("Response for path='%s': %s".formatted(path, response));
            }

            List<Option<?>> options = new ArrayList<>();

            for (Map<?, ?> list : (List<Map<?, ?>>) response.get("data")) {
                options.add(option((String) list.get("name"), list.get("id")));
            }

            return options;
        };
    }
}
